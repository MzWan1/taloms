package za.co.taloms.document.domain.entity;

public enum EntityType {
    PTO,
    PARCEL,
    RESIDENT,
    HOUSEHOLD,
    BUSINESS;

    public String getDisplayName() {
        return switch (this) {
            case PTO -> "PTO";
            case PARCEL -> "Parcel";
            case RESIDENT -> "Resident";
            case HOUSEHOLD -> "Household";
            case BUSINESS -> "Business";
        };
    }
}