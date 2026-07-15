package za.co.taloms.pto.domain.entity;

public enum PTOStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    REVOKED,
    EXPIRED;

    public String getDisplayName() {
        return switch (this) {
            case PENDING   -> "Pending Approval";
            case ACTIVE    -> "Active";
            case SUSPENDED -> "Suspended";
            case REVOKED   -> "Revoked";
            case EXPIRED   -> "Expired";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case PENDING   -> "bg-warning text-dark";
            case ACTIVE    -> "bg-success";
            case SUSPENDED -> "bg-orange text-white";
            case REVOKED   -> "bg-danger";
            case EXPIRED   -> "bg-secondary";
        };
    }
}