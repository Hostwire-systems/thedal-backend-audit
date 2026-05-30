package com.thedal.thedal_app.voter.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.thedal.thedal_app.settings.electionsettings.CasteEntity;

public class CasteSerializer extends JsonSerializer<CasteEntity> {
    @Override
    public void serialize(CasteEntity caste, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (caste == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeNumberField("id", caste.getId());
        gen.writeStringField("casteName", caste.getCasteName());
        
        // Handle potentially null Long values properly
        if (caste.getReligion() != null && caste.getReligion().getId() != null) {
            gen.writeNumberField("religionId", caste.getReligion().getId());
        } else {
            gen.writeNullField("religionId");
        }
        
        if (caste.getElectionId() != null) {
            gen.writeNumberField("electionId", caste.getElectionId());
        } else {
            gen.writeNullField("electionId");
        }
        
        gen.writeNumberField("orderIndex", caste.getOrderIndex() != null ? caste.getOrderIndex() : 0);
        gen.writeEndObject();
    }
}