package za.co.taloms.reporting.domain.entity;

public enum ReportType {
    PTO,
    PARCEL,
    POPULATION,
    LAND_UTILISATION;

    public String getDisplayName() {
        return switch (this) {
            case PTO -> "PTO Report";
            case PARCEL -> "Parcel Report";
            case POPULATION -> "Population Report";
            case LAND_UTILISATION -> "Land Utilisation Report";
        };
    }
}