package za.co.taloms.notification.domain.entity;

public enum NotificationChannel {
    EMAIL,
    SMS,
    IN_APP;

    public String getDisplayName() {
        return switch (this) {
            case EMAIL -> "Email";
            case SMS -> "SMS";
            case IN_APP -> "In-App";
        };
    }
}
