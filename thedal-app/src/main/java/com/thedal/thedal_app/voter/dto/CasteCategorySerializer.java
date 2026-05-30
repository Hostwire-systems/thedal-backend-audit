package com.thedal.thedal_app.voter.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity;

public class CasteCategorySerializer extends StdSerializer<CasteCategoryEntity> {

    public CasteCategorySerializer() {
        this(null);
    }

    public CasteCategorySerializer(Class<CasteCategoryEntity> t) {
        super(t);
    }

    @Override
    public void serialize(CasteCategoryEntity casteCategory, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (casteCategory == null) {
            jsonGenerator.writeNull();
            return;
        }
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("id", casteCategory.getId());
        jsonGenerator.writeStringField("casteCategoryName", casteCategory.getCasteCategoryName());
        jsonGenerator.writeNumberField("electionId", casteCategory.getElectionId());
        jsonGenerator.writeNumberField("orderIndex", casteCategory.getOrderIndex());
        jsonGenerator.writeEndObject();
    }
}