package za.co.taloms.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;

import java.util.List;

@Component
public class StringToBoundaryListConverter implements Converter<String, List<BoundaryPointDto>> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<BoundaryPointDto> convert(String source) {
        try {
            if (source == null || source.trim().isEmpty() || "[]".equals(source.trim())) {
                return List.of();
            }
            return mapper.readValue(source, new TypeReference<List<BoundaryPointDto>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON for boundary points", e);
        }
    }
}