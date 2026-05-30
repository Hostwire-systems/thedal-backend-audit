package com.thedal.thedal_app.voter.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.thedal.thedal_app.settings.electionsettings.Availability;

public class AvailabilitySerializer extends JsonSerializer<Availability> {
    @Override
    public void serialize(Availability availability, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (availability == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeNumberField("id", availability.getId());
        gen.writeStringField("description", availability.getDescription());
        gen.writeStringField("availabilityImage", availability.getAvailabilityImage());
        gen.writeStringField("availabilityName", availability.getAvailabilityName());
        gen.writeStringField("categoryName", availability.getCategoryName());
        gen.writeNumberField("orderIndex", availability.getOrderIndex() != null ? availability.getOrderIndex() : 0);
        gen.writeEndObject();
    }
}