package za.co.taloms.reporting.domain.entity;

public enum ReportFormat {
    PDF,
    EXCEL;

    public String getDisplayName() {
        return switch (this) {
            case PDF -> "PDF";
            case EXCEL -> "Excel";
        };
    }

    public String getExtension() {
        return switch (this) {
            case PDF -> ".pdf";
            case EXCEL -> ".xlsx";
        };
    }
}