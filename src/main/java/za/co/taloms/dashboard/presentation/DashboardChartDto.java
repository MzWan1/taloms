package za.co.taloms.dashboard.presentation;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardChartDto {
    private List<String> labels; // e.g. ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
    private Map<String, List<Integer>> currentWeek;
    private Map<String, List<Integer>> previousWeek;
}
