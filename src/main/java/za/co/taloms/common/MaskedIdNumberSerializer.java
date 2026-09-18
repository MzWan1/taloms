package za.co.taloms.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Jackson serializer that masks a South African ID number whenever a response
 * DTO is serialized to JSON for presentation to a user.
 * <p>
 * Masked format: first 6 characters + {@code *} mask + last 3 characters, e.g.
 * {@code 9001011234087 -> 900101****087}.
 * <p>
 * Apply it only to user-facing {@code String} ID-number fields with
 * {@code @JsonSerialize(using = MaskedIdNumberSerializer.class)}. It never
 * changes the in-memory value returned by the getter, so internal operations
 * (database lookups, ownership checks, edit forms, sync payloads) continue to
 * use the full ID number.
 */
public class MaskedIdNumberSerializer extends StdSerializer<String> {

    public MaskedIdNumberSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(IdMasker.maskIdNumber(value));
    }
}
