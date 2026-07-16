package za.co.taloms.businessoccupancy.domain.entity;

public enum BusinessStatus {
    ACTIVE,
    INACTIVE,
    PENDING,
    SUSPENDED;

    public String getDisplayName() {
        return switch (this) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case PENDING -> "Pending";
            case SUSPENDED -> "Suspended";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case ACTIVE -> "bg-success text-white";
            case INACTIVE -> "bg-secondary text-white";
            case PENDING -> "bg-warning text-dark";
            case SUSPENDED -> "bg-danger text-white";
        };
    }
}