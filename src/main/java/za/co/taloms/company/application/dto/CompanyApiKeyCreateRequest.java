package za.co.taloms.company.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import za.co.taloms.company.domain.entity.ApiScope;

import java.util.Set;

/** Admin request to generate a new API key for a company. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyApiKeyCreateRequest {

    @Size(max = 100, message = "Key label must be at most 100 characters")
    private String label;

    /**
     * Permissions to grant. Defaults to {@link ApiScope#POR_READ} when omitted,
     * which keeps key generation usable without knowing the scope model.
     */
    @NotEmpty(message = "At least one API scope must be granted")
    private Set<ApiScope> scopes;
}