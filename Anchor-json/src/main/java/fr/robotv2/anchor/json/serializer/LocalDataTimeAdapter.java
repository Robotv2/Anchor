package fr.robotv2.anchor.json.serializer;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

public class LocalDataTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        return LocalDateTime.parse(element.getAsString());
    }

    @Override
    public JsonElement serialize(LocalDateTime time, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(time.toString());
    }
}
