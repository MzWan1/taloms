package za.co.taloms.parcel.domain.entity;

public enum ParcelStatus {
    AVAILABLE,
    ALLOCATED,
    DISPUTED,
    RESERVED,
    INACTIVE;

    public String getDisplayName() {
        return switch (this) {
            case AVAILABLE -> "Available";
            case ALLOCATED -> "Allocated";
            case DISPUTED -> "Disputed";
            case RESERVED -> "Reserved";
            case INACTIVE -> "Inactive";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case AVAILABLE -> "bg-success text-white";
            case ALLOCATED -> "bg-primary text-white";
            case DISPUTED -> "bg-danger text-white";
            case RESERVED -> "bg-warning text-dark";
            case INACTIVE -> "bg-secondary text-white";
        };
    }
}