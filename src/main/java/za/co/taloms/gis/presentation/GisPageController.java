package za.co.taloms.gis.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import za.co.taloms.security.application.service.AuthorityScopeService;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

@Slf4j
@Controller
@RequestMapping("/gis")
@RequiredArgsConstructor
public class GisPageController {

    private final ParcelService parcelService;
    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;
    private final AuthorityScopeService scopeService;

    @GetMapping
    public String index(Model model) {
        try {
            java.util.Set<Long> currentUserAuthorityIds =
                    scopeService.getCurrentUserAuthorityIds();
            var scopedUser = scopeService.isCurrentUserChiefOrHeadsman();
            var isAdmin = scopeService.isCurrentUserAdmin();

            var authorityList = scopedUser
                    ? authorityService.findAllActive().stream()
                        .filter(a -> currentUserAuthorityIds.contains(a.getId()))
                        .toList()
                    : authorityService.findAllActive();

            var villages = scopedUser
                    ? currentUserAuthorityIds.stream()
                        .flatMap(id -> villageService.findByAuthority(id).stream())
                        .toList()
                    : villageService.findAll();

            model.addAttribute("authorities", authorityList);
            model.addAttribute("villages", villages);

            // Show parcel count based on authority scope
            long totalParcels;
            if (isAdmin) {
                totalParcels = parcelService.countAll();
            } else if (!currentUserAuthorityIds.isEmpty()) {
                totalParcels = currentUserAuthorityIds.stream()
                        .mapToLong(id -> parcelService.findByAuthorityId(id).size())
                        .sum();
            } else {
                totalParcels = 0L;
            }
            model.addAttribute("totalParcels", totalParcels);

            model.addAttribute("scopedAuthorityId",
                    currentUserAuthorityIds.size() == 1
                            ? currentUserAuthorityIds.iterator().next()
                            : null);
            model.addAttribute("pageTitle", "GIS Map");
            model.addAttribute("currentPage", "gis");
            return "gis/index";
        } catch (Exception e) {
            log.error("Error loading GIS page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading GIS: " + e.getMessage());
            model.addAttribute("authorities", java.util.Collections.emptyList());
            model.addAttribute("villages", java.util.Collections.emptyList());
            model.addAttribute("totalParcels", 0L);
            model.addAttribute("scopedAuthorityId", null);
            model.addAttribute("pageTitle", "GIS Map");
            model.addAttribute("currentPage", "gis");
            return "gis/index";
        }
    }

    @GetMapping("/village/{villageId}")
    public String villageMap(@PathVariable Long villageId, Model model) {
        try {
            var village = villageService.findById(villageId);
            var parcels = parcelService.findByVillage(villageId);

            model.addAttribute("village", village);
            model.addAttribute("parcels", parcels);
            model.addAttribute("pageTitle", "GIS - " + village.getVillageName());
            model.addAttribute("currentPage", "gis");
            return "gis/village";
        } catch (Exception e) {
            log.error("Error loading village map: {}", e.getMessage(), e);
            return "redirect:/gis";
        }
    }
}

