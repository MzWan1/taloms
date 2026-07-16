package za.co.taloms.resident.domain.entity;

public enum RelationshipType {
    HOUSEHOLD_HEAD,
    SPOUSE,
    CHILD,
    PARENT,
    SIBLING,
    GRANDPARENT,
    GRANDCHILD,
    OTHER;

    public String getDisplayName() {
        return switch (this) {
            case HOUSEHOLD_HEAD -> "Household Head";
            case SPOUSE -> "Spouse";
            case CHILD -> "Child";
            case PARENT -> "Parent";
            case SIBLING -> "Sibling";
            case GRANDPARENT -> "Grandparent";
            case GRANDCHILD -> "Grandchild";
            case OTHER -> "Other";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case HOUSEHOLD_HEAD -> "bg-primary text-white";
            case SPOUSE -> "bg-info text-dark";
            case CHILD -> "bg-success text-white";
            case PARENT -> "bg-warning text-dark";
            case SIBLING -> "bg-secondary text-white";
            case GRANDPARENT -> "bg-dark text-white";
            case GRANDCHILD -> "bg-success text-white";
            case OTHER -> "bg-light text-dark";
        };
    }
}