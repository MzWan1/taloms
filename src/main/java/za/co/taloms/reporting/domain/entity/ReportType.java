package za.co.taloms.reporting.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

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

    @JsonCreator
    public static ReportType fromJsonValue(String value) {
        return switch (value) {
            case "PTO" -> PTO_OCCUPANCY_REGISTER;
            case "PARCEL" -> LAND_PARCEL_UTILISATION;
            case "POPULATION" -> VILLAGE_POPULATION;
            case "LAND_UTILISATION" -> ECONOMIC_ACTIVITY;
            default -> ReportType.valueOf(value);
        };
    }

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

    public String getEndpoint() {
        return switch (this) {
            case PTO_OCCUPANCY_REGISTER -> "/pto";
            case LAND_PARCEL_UTILISATION -> "/parcel";
            case STAND_ALLOCATION -> "/stand-allocation";
            case VILLAGE_POPULATION -> "/village-population";
            case HOUSEHOLD_REGISTER -> "/household-register";
            case RESIDENT_DEMOGRAPHICS -> "/resident-demographics";
            case BUSINESS_OCCUPANCY_REGISTER -> "/business-occupancy";
            case ECONOMIC_ACTIVITY -> "/economic-activity";
            case USER_ACTIVITY_AUDIT -> "/user-activity-audit";
            case DOCUMENT_MANAGEMENT -> "/document-management";
            case PERFORMANCE_DASHBOARD -> "/performance-dashboard";
            case LAND_BOUNDARY -> "/land-boundary";
        };
    }

    public String getIcon() {
        return switch (this) {
            case PTO_OCCUPANCY_REGISTER -> "bi-file-earmark-text";
            case LAND_PARCEL_UTILISATION -> "bi-map";
            case STAND_ALLOCATION -> "bi-grid-1x2";
            case VILLAGE_POPULATION -> "bi-people";
            case HOUSEHOLD_REGISTER -> "bi-house-door";
            case RESIDENT_DEMOGRAPHICS -> "bi-person-lines-fill";
            case BUSINESS_OCCUPANCY_REGISTER -> "bi-shop";
            case ECONOMIC_ACTIVITY -> "bi-graph-up-arrow";
            case USER_ACTIVITY_AUDIT -> "bi-shield-check";
            case DOCUMENT_MANAGEMENT -> "bi-folder-fill";
            case PERFORMANCE_DASHBOARD -> "bi-speedometer2";
            case LAND_BOUNDARY -> "bi-pin-map-fill";
        };
    }

    public String getColor() {
        return switch (this) {
            case PTO_OCCUPANCY_REGISTER -> "primary";
            case LAND_PARCEL_UTILISATION -> "success";
            case STAND_ALLOCATION -> "info";
            case VILLAGE_POPULATION -> "warning";
            case HOUSEHOLD_REGISTER -> "danger";
            case RESIDENT_DEMOGRAPHICS -> "dark";
            case BUSINESS_OCCUPANCY_REGISTER -> "primary";
            case ECONOMIC_ACTIVITY -> "success";
            case USER_ACTIVITY_AUDIT -> "warning";
            case DOCUMENT_MANAGEMENT -> "info";
            case PERFORMANCE_DASHBOARD -> "danger";
            case LAND_BOUNDARY -> "dark";
        };
    }
}