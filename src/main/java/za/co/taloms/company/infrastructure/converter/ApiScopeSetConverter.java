package za.co.taloms.company.infrastructure.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import za.co.taloms.company.domain.entity.ApiScope;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persists a key's {@link ApiScope} set as a comma-separated string column.
 *
 * Scopes are a small, fixed set, so a single VARCHAR column avoids an extra
 * join table while remaining extensible (new enum constants need no migration).
 */
@Converter
public class ApiScopeSetConverter implements AttributeConverter<Set<ApiScope>, String> {

    @Override
    public String convertToDatabaseColumn(Set<ApiScope> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<ApiScope> convertToEntityAttribute(String dbData) {
        Set<ApiScope> scopes = new LinkedHashSet<>();
        if (dbData == null || dbData.isBlank()) {
            return scopes;
        }
        Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(name -> {
                    try {
                        scopes.add(ApiScope.valueOf(name));
                    } catch (IllegalArgumentException ignored) {
                        // Unknown scope in the database is ignored rather than
                        // failing the whole request; it simply grants nothing.
                    }
                });
        return scopes;
    }
}