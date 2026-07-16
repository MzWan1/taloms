package za.co.taloms.audit.domain.entity;

public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    APPROVE,
    REVOKE,
    SUSPEND,
    REACTIVATE,
    REINSTATE,
    ACTIVATE,
    DEACTIVATE,
    LOGIN,
    LOGOUT,
    DOWNLOAD,
    UPLOAD;

    public String getDisplayName() {
        return switch (this) {
            case CREATE -> "Created";
            case UPDATE -> "Updated";
            case DELETE -> "Deleted";
            case APPROVE -> "Approved";
            case REVOKE -> "Revoked";
            case SUSPEND -> "Suspended";
            case REACTIVATE -> "Reactivated";
            case REINSTATE -> "Reinstated";
            case ACTIVATE -> "Activated";
            case DEACTIVATE -> "Deactivated";
            case LOGIN -> "Logged In";
            case LOGOUT -> "Logged Out";
            case DOWNLOAD -> "Downloaded";
            case UPLOAD -> "Uploaded";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case CREATE -> "bg-success text-white";
            case UPDATE -> "bg-primary text-white";
            case DELETE -> "bg-danger text-white";
            case APPROVE -> "bg-success text-white";
            case REVOKE -> "bg-danger text-white";
            case SUSPEND -> "bg-warning text-dark";
            case REACTIVATE -> "bg-success text-white";
            case REINSTATE -> "bg-info text-white";
            case ACTIVATE -> "bg-success text-white";
            case DEACTIVATE -> "bg-secondary text-white";
            case LOGIN -> "bg-info text-white";
            case LOGOUT -> "bg-secondary text-white";
            case DOWNLOAD -> "bg-primary text-white";
            case UPLOAD -> "bg-success text-white";
        };
    }
}