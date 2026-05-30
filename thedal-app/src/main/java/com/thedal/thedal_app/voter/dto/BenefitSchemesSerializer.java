package com.thedal.thedal_app.voter.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemes;

public class BenefitSchemesSerializer extends JsonSerializer<BenefitSchemes> {
    @Override
    public void serialize(BenefitSchemes benefitSchemes, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (benefitSchemes == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeNumberField("id", benefitSchemes.getId());
        gen.writeStringField("schemeName", benefitSchemes.getSchemeName());
        gen.writeStringField("imageUrl", benefitSchemes.getImageUrl());
        gen.writeStringField("schemeBy", benefitSchemes.getSchemeBy() != null ? benefitSchemes.getSchemeBy().name() : null);
        gen.writeBooleanField("userSelection", benefitSchemes.getUserSelection() != null ? benefitSchemes.getUserSelection() : false);       
        gen.writeNumberField("orderIndex", benefitSchemes.getOrderIndex() != null ? benefitSchemes.getOrderIndex() : 0);
//        gen.writeFieldName("userSelection");
//        if (benefitSchemes.getUserSelection() != null) {
//            gen.writeBoolean(benefitSchemes.getUserSelection());
//        } else {
//            gen.writeNull();
//        }        
        gen.writeEndObject();
    }
}