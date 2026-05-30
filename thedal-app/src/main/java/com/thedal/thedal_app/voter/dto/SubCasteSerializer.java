package com.thedal.thedal_app.voter.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;

public class SubCasteSerializer extends JsonSerializer<SubCasteEntity> {
    @Override
    public void serialize(SubCasteEntity subCaste, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (subCaste == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeNumberField("id", subCaste.getId());
        gen.writeStringField("subCasteName", subCaste.getSubCasteName());
        
        // Handle potentially null Long values properly
        if (subCaste.getCaste() != null && subCaste.getCaste().getId() != null) {
            gen.writeNumberField("casteId", subCaste.getCaste().getId());
        } else {
            gen.writeNullField("casteId");
        }
        
        if (subCaste.getReligion() != null && subCaste.getReligion().getId() != null) {
            gen.writeNumberField("religionId", subCaste.getReligion().getId());
        } else {
            gen.writeNullField("religionId");
        }
        
        if (subCaste.getAccountId() != null) {
            gen.writeNumberField("accountId", subCaste.getAccountId());
        } else {
            gen.writeNullField("accountId");
        }
        
        if (subCaste.getElectionId() != null) {
            gen.writeNumberField("electionId", subCaste.getElectionId());
        } else {
            gen.writeNullField("electionId");
        }
        
        gen.writeNumberField("orderIndex", subCaste.getOrderIndex() != null ? subCaste.getOrderIndex() : 0);
        gen.writeEndObject();
    }
}
