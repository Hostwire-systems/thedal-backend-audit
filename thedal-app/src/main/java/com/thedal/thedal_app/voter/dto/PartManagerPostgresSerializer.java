package com.thedal.thedal_app.voter.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.thedal.thedal_app.election.PartManager;

public class PartManagerPostgresSerializer extends StdSerializer<PartManager> {
    
    public PartManagerPostgresSerializer() {
        this(null);
    }
    
    public PartManagerPostgresSerializer(Class<PartManager> t) {
        super(t);
    }

    @Override
    public void serialize(PartManager partManager, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) 
            throws IOException {
        if (partManager == null) {
            jsonGenerator.writeNull();
            return;
        }
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("pincode", partManager.getPincode());
        jsonGenerator.writeStringField("partNameEn", partManager.getPartNameEnglish());
        jsonGenerator.writeStringField("partNameL1", partManager.getPartNameL1());
        jsonGenerator.writeNumberField("partLat", partManager.getPartLat() != null ? partManager.getPartLat() : null);
        jsonGenerator.writeNumberField("partLong", partManager.getPartLong() != null ? partManager.getPartLong() : null);
        jsonGenerator.writeEndObject();
    }
}
