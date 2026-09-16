package za.co.taloms.company.domain.entity;

/**
 * Lifecycle state of a single company API key. Independent of the owning
 * company's status: both must be ACTIVE for a request to be allowed.
 */
public enum ApiKeyStatus {
    ACTIVE,
    REVOKED;

    public String getDisplayName() {
        return switch (this) {
            case ACTIVE  -> "Active";
            case REVOKED -> "Revoked";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case ACTIVE  -> "bg-success";
            case REVOKED -> "bg-danger";
        };
    }
}