package za.co.taloms.reporting.application.dto;

import java.util.List;
import java.util.Map;

public record ReportSection(
    String sectionTitle,
    List<Map<String, String>> metrics,
    List<String> tableHeaders,
    List<List<String>> tableRows
) {}
