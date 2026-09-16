package za.co.taloms.company.domain.entity;

/**
 * Lifecycle state of an external company's API authorization.
 *
 * Only an ADMIN may move a company between these states. A DISABLED company
 * loses access to every protected API endpoint immediately, regardless of the
 * state of its individual API keys.
 */
public enum CompanyStatus {
    ACTIVE,
    DISABLED;

    public String getDisplayName() {
        return switch (this) {
            case ACTIVE   -> "Active";
            case DISABLED -> "Disabled";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case ACTIVE   -> "bg-success";
            case DISABLED -> "bg-danger";
        };
    }
}