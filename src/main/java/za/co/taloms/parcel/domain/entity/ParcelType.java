package za.co.taloms.parcel.domain.entity;

public enum ParcelType {
    RESIDENTIAL,
    BUSINESS,
    AGRICULTURAL,
    COMMUNAL,
    RESERVED;

    public String getDisplayName() {
        return switch (this) {
            case RESIDENTIAL -> "Residential";
            case BUSINESS -> "Business";
            case AGRICULTURAL -> "Agricultural";
            case COMMUNAL -> "Communal";
            case RESERVED -> "Reserved";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case RESIDENTIAL -> "bg-primary text-white";
            case BUSINESS -> "bg-dark text-white";
            case AGRICULTURAL -> "bg-success text-white";
            case COMMUNAL -> "bg-info text-dark";
            case RESERVED -> "bg-warning text-dark";
        };
    }
}