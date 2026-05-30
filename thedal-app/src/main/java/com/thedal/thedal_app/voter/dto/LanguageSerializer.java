package com.thedal.thedal_app.voter.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.thedal.thedal_app.settings.electionsettings.Language;

public class LanguageSerializer extends JsonSerializer<Language> {
    @Override
    public void serialize(Language language, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (language == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeNumberField("id", language.getId());
        gen.writeStringField("languageName", language.getLanguageName());
        gen.writeNumberField("orderIndex", language.getOrderIndex() != null ? language.getOrderIndex() : 0);
        gen.writeEndObject();
    }
}
