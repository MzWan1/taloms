package za.co.taloms.dashboard.application.service;

import za.co.taloms.dashboard.application.dto.DashboardSummaryDto;

public interface DashboardService {
    DashboardSummaryDto getDashboardSummary();
    DashboardSummaryDto getDashboardSummaryForAuthority(Long authorityId);
}