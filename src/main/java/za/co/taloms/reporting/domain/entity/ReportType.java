package za.co.taloms.reporting.domain.entity;

public enum ReportType {
    PTO_OCCUPANCY_REGISTER,
    LAND_PARCEL_UTILISATION,
    STAND_ALLOCATION,
    VILLAGE_POPULATION,
    HOUSEHOLD_REGISTER,
    RESIDENT_DEMOGRAPHICS,
    BUSINESS_OCCUPANCY_REGISTER,
    ECONOMIC_ACTIVITY,
    USER_ACTIVITY_AUDIT,
    DOCUMENT_MANAGEMENT,
    PERFORMANCE_DASHBOARD,
    LAND_BOUNDARY;

    public String getDisplayName() {
        return switch (this) {
            case PTO_OCCUPANCY_REGISTER -> "PTO Occupancy Register";
            case LAND_PARCEL_UTILISATION -> "Land Parcel Utilisation Report";
            case STAND_ALLOCATION -> "Stand Allocation Summary";
            case VILLAGE_POPULATION -> "Village Population Report";
            case HOUSEHOLD_REGISTER -> "Household Register";
            case RESIDENT_DEMOGRAPHICS -> "Resident Demographics Report";
            case BUSINESS_OCCUPANCY_REGISTER -> "Business Occupancy Register";
            case ECONOMIC_ACTIVITY -> "Economic Activity Summary";
            case USER_ACTIVITY_AUDIT -> "User Activity and Audit Report";
            case DOCUMENT_MANAGEMENT -> "Document Management Report";
            case PERFORMANCE_DASHBOARD -> "Authority Performance Dashboard";
            case LAND_BOUNDARY -> "Land Boundary Report";
        };
    }
}