package za.co.taloms.company.application.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Minimised proof-of-residence verification result.
 *
 * Contains only what an approved business needs to confirm residence: whether a
 * valid proof exists, the holder's name, the proof reference and its validity,
 * and the village/authority it relates to. No contact details, date of birth,
 * identity document, authentication data, audit data or internal ids are exposed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PorVerificationResponse {

    /** True only when an ACTIVE, non-deleted, unexpired proof of residence exists. */
    private boolean verified;

    /** Coarse machine-readable outcome, e.g. {@code NOT_FOUND}. Null when verified. */
    private String reason;

    /** Human-readable explanation of {@link #reason}. Null when verified. */
    private String message;

    /** Full name of the proof holder. Null when not verified. */
    private String residentName;

    /** Masked ID number (first 6 + last 3 digits) — never the full ID number. */
    private String maskedIdNumber;

    /** TALOMS proof-of-residence reference (PTO number). */
    private String proofOfResidenceNumber;

    private String proofStatus;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String villageName;
    private String traditionalAuthorityName;

    /** Time the verification was performed (server time). */
    private LocalDateTime verifiedAt;
}