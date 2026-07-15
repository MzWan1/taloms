package za.co.taloms.pto.domain.entity;

public enum PTOPurpose {
    RESIDENTIAL,
    BUSINESS,
    AGRICULTURAL,
    MIXED_USE;

    public String getDisplayName() {
        return switch (this) {
            case RESIDENTIAL  -> "Residential";
            case BUSINESS     -> "Business";
            case AGRICULTURAL -> "Agricultural";
            case MIXED_USE    -> "Mixed Use";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case RESIDENTIAL  -> "bg-primary";
            case BUSINESS     -> "bg-purple text-white";
            case AGRICULTURAL -> "bg-success";
            case MIXED_USE    -> "bg-info text-dark";
        };
    }
}