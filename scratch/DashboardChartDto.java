package za.co.taloms.dashboard.presentation;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardChartDto {
    private List<String> labels; // e.g. ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
    private Map<String, List<Integer>> currentWeekData; // e.g. "Parcels": [0, 2, 1, ...], "PTOs": [1, 0, 0, ...]
    private Map<String, List<Integer>> previousWeekData;
}
