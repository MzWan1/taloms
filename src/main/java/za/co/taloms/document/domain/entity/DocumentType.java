package za.co.taloms.document.domain.entity;

public enum DocumentType {
    PTO_CERT,
    ID_COPY,
    SURVEY,
    PHOTO,
    OTHER;

    public String getDisplayName() {
        return switch (this) {
            case PTO_CERT -> "PTO Certificate";
            case ID_COPY -> "ID / Passport";
            case SURVEY -> "Survey Drawing";
            case PHOTO -> "Photo";
            case OTHER -> "Other";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case PTO_CERT -> "bg-success text-white";
            case ID_COPY -> "bg-info text-dark";
            case SURVEY -> "bg-primary text-white";
            case PHOTO -> "bg-warning text-dark";
            case OTHER -> "bg-secondary text-white";
        };
    }

    public String getIcon() {
        return switch (this) {
            case PTO_CERT -> "bi-file-earmark-pdf";
            case ID_COPY -> "bi-person-badge";
            case SURVEY -> "bi-map";
            case PHOTO -> "bi-image";
            case OTHER -> "bi-file-earmark";
        };
    }
}