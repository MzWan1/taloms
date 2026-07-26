package za.co.taloms.notification.domain.entity;

public enum NotificationType {
    PTO_CREATED,
    PTO_APPROVED,
    PTO_REVOKED,
    PTO_SUSPENDED,
    PTO_REACTIVATED,
    PTO_REINSTATED,
    PTO_EXPIRED,
    DOCUMENT_UPLOADED,
    SYSTEM_ALERT;

    public String getDisplayName() {
        return switch (this) {
            case PTO_CREATED -> "PTO Created";
            case PTO_APPROVED -> "PTO Approved";
            case PTO_REVOKED -> "PTO Revoked";
            case PTO_SUSPENDED -> "PTO Suspended";
            case PTO_REACTIVATED -> "TO Reactivated";
            case PTO_REINSTATED -> "PTO Reinstated";
            case PTO_EXPIRED -> "PTO Expired";
            case DOCUMENT_UPLOADED -> "Document Uploaded";
            case SYSTEM_ALERT -> "System Alert";
        };
    }
}


