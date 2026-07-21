package com.example.transaction_service.entity.converter;

import com.example.transaction_service.entity.TransactionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {
    @Override
    public String convertToDatabaseColumn(TransactionStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TransactionStatus convertToEntityAttribute(String dbData) {
        return TransactionStatus.fromCode(dbData);
    }
}
