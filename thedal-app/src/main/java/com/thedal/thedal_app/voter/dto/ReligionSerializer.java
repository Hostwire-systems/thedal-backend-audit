package com.thedal.thedal_app.voter.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;

public class ReligionSerializer extends JsonSerializer<ReligionEntity> {
    @Override
    public void serialize(ReligionEntity religion, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (religion == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeNumberField("id", religion.getId());
        gen.writeStringField("religionName", religion.getReligionName());
        gen.writeStringField("religionImage", religion.getReligionImage());
        gen.writeNumberField("orderIndex", religion.getOrderIndex() != null ? religion.getOrderIndex() : 0);
        gen.writeEndObject();
    }
}