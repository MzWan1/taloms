package za.co.taloms.common;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Jackson serializer that masks Long IDs during JSON serialization.
 * Masked format: first 6 digits + "***" + last 3 digits.
 * <p>
 * Use this serializer only on specific fields annotated with @JsonSerialize(using = MaskedLongSerializer.class).
 * Do NOT register this globally as it will mask counts, version numbers, and other non-ID numeric values.
 * <p>
 * Internal operations always use unmasked IDs; masking only applies to JSON output for annotated fields.
 */
public class MaskedLongSerializer extends StdSerializer<Long> {

    public MaskedLongSerializer() {
        super(Long.class);
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String masked = IdMasker.mask(value);
        gen.writeString(masked);
    }
}
