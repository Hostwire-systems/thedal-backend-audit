package com.thedal.thedal_app.voter.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.thedal.thedal_app.settings.electionsettings.Party;

public class PartySerializer extends JsonSerializer<Party> {
    @Override
    public void serialize(Party party, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (party == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeNumberField("id", party.getId());
        gen.writeStringField("partyName", party.getPartyName());
        gen.writeStringField("partyShortName", party.getPartyShortName());
        gen.writeStringField("partyImage", party.getPartyImage());
        gen.writeNumberField("orderIndex", party.getOrderIndex() != null ? party.getOrderIndex() : 0);
        gen.writeEndObject();
    }
}