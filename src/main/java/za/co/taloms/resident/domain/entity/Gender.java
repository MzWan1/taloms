package za.co.taloms.resident.domain.entity;

public enum Gender {
    MALE,
    FEMALE,
    OTHER;

    public String getDisplayName() {
        return switch (this) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            case OTHER -> "Other";
        };
    }
}