package com.thedal.thedal_app.voter;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.thedal.thedal_app.election.DynamicFieldEntity;

public class DynamicFieldSerializer extends StdSerializer<DynamicFieldEntity> {

    public DynamicFieldSerializer() {
        this(null);
    }

    public DynamicFieldSerializer(Class<DynamicFieldEntity> t) {
        super(t);
    }

    @Override
    public void serialize(DynamicFieldEntity dynamicField, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (dynamicField != null) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("id", dynamicField.getId());
            jsonGenerator.writeStringField("label", dynamicField.getLabel());
            jsonGenerator.writeStringField("name", dynamicField.getName());
            jsonGenerator.writeStringField("type", dynamicField.getType());
            jsonGenerator.writeBooleanField("required", dynamicField.getRequired());

            // Writing array field properly
            jsonGenerator.writeFieldName("options");
            jsonGenerator.writeStartArray();
            List<String> options = dynamicField.getOptions();
            if (options != null) {
                for (String option : options) {
                    jsonGenerator.writeString(option);
                }
            }
            jsonGenerator.writeEndArray();

            //jsonGenerator.writeNumberField("orderIndex", dynamicField.getOrderIndex());
            jsonGenerator.writeBooleanField("status", dynamicField.getStatus());

            jsonGenerator.writeEndObject();
        } else {
            jsonGenerator.writeNull();
        }
    }

}