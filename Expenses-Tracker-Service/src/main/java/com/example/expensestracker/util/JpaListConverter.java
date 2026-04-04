package com.example.expensestracker.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

@Converter(autoApply = false)
public class JpaListConverter implements AttributeConverter<List<Object>, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Object> attribute) {
        try {
            if (attribute == null) return null;
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not convert list to JSON string.", e);
        }
    }

    @Override
    public List<Object> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isEmpty()) return null;
            return mapper.readValue(dbData, List.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not convert JSON string to list.", e);
        }
    }
}
