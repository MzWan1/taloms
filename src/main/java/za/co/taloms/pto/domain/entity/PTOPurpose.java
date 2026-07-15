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
            case RESIDENTIAL  -> "bg-primary text-white";     // Dark blue with white text
            case BUSINESS     -> "bg-dark text-white";        // Dark gray with white text
            case AGRICULTURAL -> "bg-success text-white";     // Dark green with white text
            case MIXED_USE    -> "bg-warning text-dark";      // Yellow with dark text
        };
    }
}