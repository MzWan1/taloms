package za.co.taloms.businessoccupancy.domain.entity;

public enum BusinessType {
    RETAIL,
    WHOLESALE,
    RESTAURANT,
    OFFICE,
    WAREHOUSE,
    OTHER;

    public String getDisplayName() {
        return switch (this) {
            case RETAIL -> "Retail";
            case WHOLESALE -> "Wholesale";
            case RESTAURANT -> "Restaurant";
            case OFFICE -> "Office";
            case WAREHOUSE -> "Warehouse";
            case OTHER -> "Other";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case RETAIL -> "bg-primary text-white";
            case WHOLESALE -> "bg-info text-dark";
            case RESTAURANT -> "bg-warning text-dark";
            case OFFICE -> "bg-secondary text-white";
            case WAREHOUSE -> "bg-dark text-white";
            case OTHER -> "bg-light text-dark";
        };
    }
}