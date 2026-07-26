package za.co.taloms.parcel.domain.entity;

public enum CaptureMode {
    MANUAL_TAP,
    AUTO_WALK,
    HYBRID,
    DRONE_ASSIST;

    public String getDisplayName() {
        return switch (this) {
            case MANUAL_TAP -> "Manual Tap";
            case AUTO_WALK -> "Auto-Walk (GPS Trace)";
            case HYBRID -> "Hybrid (Auto + Manual)";
            case DRONE_ASSIST -> "Drone Assist";
        };
    }

    public String getDescription() {
        return switch (this) {
            case MANUAL_TAP -> "Tap once at each corner of the parcel";
            case AUTO_WALK -> "Walk the perimeter — GPS captures automatically every 2-3 seconds";
            case HYBRID -> "Auto-walk with manual correction taps at corners";
            case DRONE_ASSIST -> "UAV flies perimeter — coordinates extracted automatically (Phase 3)";
        };
    }
}
