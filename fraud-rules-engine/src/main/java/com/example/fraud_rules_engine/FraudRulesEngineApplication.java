package com.example.fraud_rules_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FraudRulesEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(FraudRulesEngineApplication.class, args);
	}

}
