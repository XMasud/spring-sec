package com.example.fraud_rules_engine.streams;

import com.example.fraud_rules_engine.event.FraudAlertEvent;
import com.example.fraud_rules_engine.event.TransactionCreatedEvent;
import com.example.fraud_rules_engine.serde.JsonSerdeFactory;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Configuration
public class FraudDetectionTopology {

    private static final int VELOCITY_THRESHOLD = 5;         // >5 txns in window = flagged
    private static final Duration VELOCITY_WINDOW = Duration.ofSeconds(60);
    private static final Duration GRACE_PERIOD = Duration.ofSeconds(5); // late-arriving events

    @Bean
    public KStream<String, TransactionCreatedEvent> topology(StreamsBuilder streamsBuilder) {

        Serde<TransactionCreatedEvent> txnSerde = JsonSerdeFactory.forType(TransactionCreatedEvent.class);
        Serde<FraudAlertEvent> alertSerde = JsonSerdeFactory.forType(FraudAlertEvent.class);

        // Source: keyed by sourceAccountId (the partition key set by Transaction Service's outbox)
        KStream<String, TransactionCreatedEvent> transactions = streamsBuilder.stream(
                "transactions.raw",
                Consumed.with(Serdes.String(), txnSerde)
        );

        // ---------- RULE 1: Velocity ----------
        // Tumbling 60s window per account, count transactions. >5 in a window = flagged.
        KTable<Windowed<String>, Long> velocityCounts = transactions
                .groupByKey(Grouped.with(Serdes.String(), txnSerde))
                .windowedBy(TimeWindows.ofSizeAndGrace(VELOCITY_WINDOW, GRACE_PERIOD))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("velocity-store"));

        KStream<String, FraudAlertEvent> velocityAlerts = velocityCounts
                .toStream()
                .filter((windowedKey, count) -> count != null && count > VELOCITY_THRESHOLD)
                .map((windowedKey, count) -> {
                    String accountId = windowedKey.key();
                    FraudAlertEvent alert = new FraudAlertEvent(
                            null, // no single transactionId — this alert is about the account's pattern
                            accountId,
                            "VELOCITY_BREACH",
                            BigDecimal.valueOf(Math.min(0.99, count / 10.0)), // simple scoring — refine later
                            "Account had " + count + " transactions within " + VELOCITY_WINDOW.getSeconds() + "s",
                            Instant.now()
                    );
                    return KeyValue.pair(accountId, alert);
                });

        // ---------- RULE 2: Amount spike (stateless, per-event) ----------
        // Simple fixed threshold for now — a real version compares against a rolling
        // per-account average (a KTable aggregation), which is a natural next step.
        BigDecimal SPIKE_THRESHOLD = BigDecimal.valueOf(10000);

        KStream<String, FraudAlertEvent> spikeAlerts = transactions
                .filter((accountId, txn) -> txn.amount().compareTo(SPIKE_THRESHOLD) > 0)
                .map((accountId, txn) -> KeyValue.pair(
                        accountId,
                        new FraudAlertEvent(
                                txn.transactionId(),
                                accountId,
                                "AMOUNT_SPIKE",
                                BigDecimal.valueOf(0.85),
                                "Transaction amount " + txn.amount() + " exceeds threshold " + SPIKE_THRESHOLD,
                                Instant.now()
                        )
                ));

        // Merge both rule streams into one flagged-transactions output topic
        velocityAlerts.merge(spikeAlerts)
                .to("transactions.flagged", Produced.with(Serdes.String(), alertSerde));

        // Transactions that triggered NO rule get marked cleared.
        // (Simplified: real version would need to correlate against the alert streams
        // with a join + a timeout window before declaring "cleared" — flag this as a
        // known simplification worth revisiting once the happy path works.)
        transactions
                .filter((accountId, txn) -> txn.amount().compareTo(SPIKE_THRESHOLD) <= 0)
                .mapValues(txn -> txn.transactionId().toString())
                .to("transactions.cleared", Produced.with(Serdes.String(), Serdes.String()));

        return transactions;
    }
}
