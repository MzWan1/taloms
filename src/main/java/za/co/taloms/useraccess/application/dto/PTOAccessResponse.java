package za.co.taloms.useraccess.application.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.*;
import za.co.taloms.common.MaskedIdNumberSerializer;
import za.co.taloms.common.MaskedLongSerializer;

/** A user currently linked to a specific PTO (delegated proof-of-residence access). */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PTOAccessResponse {
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long   userId;
    private String fullName;
    private String email;
    @JsonSerialize(using = MaskedIdNumberSerializer.class)
    private String idNumber;
    private boolean owner;
}
