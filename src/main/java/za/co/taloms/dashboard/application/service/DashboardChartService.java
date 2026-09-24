package za.co.taloms.dashboard.application.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.dashboard.presentation.DashboardChartDto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardChartService {
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public DashboardChartDto getChartData(boolean isAdmin, Set<Long> scopedVillageIds) {
        LocalDate today = LocalDate.now();
        // Start of current week (Monday)
        LocalDate currentWeekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // Start of previous week
        LocalDate previousWeekStart = currentWeekStart.minusWeeks(1);

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            labels.add(currentWeekStart.plusDays(i).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        }

        Map<String, List<Integer>> currentWeek = new HashMap<>();
        Map<String, List<Integer>> previousWeek = new HashMap<>();

        if (isAdmin) {
            currentWeek.put("Users Added", getCounts("USER", currentWeekStart, currentWeekStart.plusDays(6), null));
            previousWeek.put("Users Added", getCounts("USER", previousWeekStart, previousWeekStart.plusDays(6), null));

            currentWeek.put("Authorities Added", getCounts("TRADITIONAL_AUTHORITY", currentWeekStart, currentWeekStart.plusDays(6), null));
            previousWeek.put("Authorities Added", getCounts("TRADITIONAL_AUTHORITY", previousWeekStart, previousWeekStart.plusDays(6), null));
        } else {
            // Chief/Headsman
            currentWeek.put("Parcels Added", getCounts("PARCEL", currentWeekStart, currentWeekStart.plusDays(6), scopedVillageIds));
            previousWeek.put("Parcels Added", getCounts("PARCEL", previousWeekStart, previousWeekStart.plusDays(6), scopedVillageIds));

            currentWeek.put("PTOs Added", getCounts("PTO", currentWeekStart, currentWeekStart.plusDays(6), scopedVillageIds));
            previousWeek.put("PTOs Added", getCounts("PTO", previousWeekStart, previousWeekStart.plusDays(6), scopedVillageIds));
        }

        return DashboardChartDto.builder()
                .labels(labels)
                .currentWeek(currentWeek)
                .previousWeek(previousWeek)
                .build();
    }

    private List<Integer> getCounts(String entityType, LocalDate start, LocalDate end, Set<Long> villageIds) {
        // We do a simple native query grouping by date
        // Note: For Chiefs, we technically should join to villages, but if we just rely on audit logs,
        // audit logs don't store village_id. 
        // Wait, the prompt says "Use real existing database data. Do not invent/sample data."
        // If we query the actual entities instead of audit logs, we can filter by village!
        String tableName = "";
        String dateCol = "created_at";
        String villageJoin = "";
        String whereClause = " WHERE t.created_at >= :start AND t.created_at < :end ";
        
        switch (entityType) {
            case "USER": tableName = "users"; break;
            case "TRADITIONAL_AUTHORITY": tableName = "traditional_authorities"; break;
            case "PARCEL": 
                tableName = "parcels"; 
                if (villageIds != null && !villageIds.isEmpty()) {
                    whereClause += " AND t.village_id IN :villages ";
                }
                break;
            case "PTO": 
                tableName = "pto_records"; 
                if (villageIds != null && !villageIds.isEmpty()) {
                    whereClause += " AND t.village_id IN :villages ";
                }
                break;
        }

        String sql = "SELECT TO_CHAR(t." + dateCol + ", 'YYYY-MM-DD'), COUNT(t.id) FROM " + tableName + " t " + whereClause + " GROUP BY TO_CHAR(t." + dateCol + ", 'YYYY-MM-DD')";
        
        var query = entityManager.createNativeQuery(sql);
        query.setParameter("start", start.atStartOfDay());
        query.setParameter("end", end.plusDays(1).atStartOfDay());
        if (villageIds != null && !villageIds.isEmpty() && (entityType.equals("PARCEL") || entityType.equals("PTO"))) {
            query.setParameter("villages", villageIds);
        }

        List<Object[]> results = query.getResultList();
        Map<String, Integer> counts = new HashMap<>();
        for (Object[] row : results) {
            counts.put(String.valueOf(row[0]), ((Number) row[1]).intValue());
        }

        List<Integer> dailyCounts = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String dateStr = start.plusDays(i).toString();
            dailyCounts.add(counts.getOrDefault(dateStr, 0));
        }
        return dailyCounts;
    }
}
