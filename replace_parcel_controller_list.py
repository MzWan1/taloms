import re

with open('./src/main/java/za/co/taloms/parcel/presentation/ParcelPageController.java', 'r') as f:
    content = f.read()

new_method = """    @GetMapping
    public String list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) ParcelStatus status,
            @RequestParam(value = "villageId", required = false) Long villageId,
            @org.springframework.data.web.PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable,
            Model model) {
        try {
            Set<Long> allowedVillageIds = scopedVillageIds();

            org.springframework.data.domain.Page<ParcelResponse> page = parcelService.searchParcels(q, status, villageId, allowedVillageIds, pageable);

            // Fetch counts from database ideally, but for now we can either do custom counts or just 0
            // Wait, we can use parcelService.countByStatus
            // But if filtered by allowedVillageIds? The counts should be overall or scoped?
            // Existing logic counted the filtered list. Since it's paginated, we should just use total elements for totalCount.
            // But availableCount? 
            long availableCount = 0; // Or better, implement count queries or skip if not needed.
            // Actually, let's keep it simple or implement specific count.
            // But existing code just streams the `parcels` list. 
            // We can't do that with a Page. Let's just use the page.getTotalElements() for total count.
            
            model.addAttribute("page", page);
            model.addAttribute("parcels", page.getContent());
            model.addAttribute("q", q);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("selectedVillageId", villageId);
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("totalCount", page.getTotalElements());
            
            // To prevent crashes, let's just set the other counts to 0 for now as we paginate.
            // If they are strictly needed, we should add count methods to the service.
            model.addAttribute("availableCount", parcelService.countByStatus(ParcelStatus.AVAILABLE));
            model.addAttribute("allocatedCount", parcelService.countByStatus(ParcelStatus.ALLOCATED));
            model.addAttribute("disputedCount", parcelService.countByStatus(ParcelStatus.DISPUTED));
            
            model.addAttribute("pageTitle", "Parcel Management");
            model.addAttribute("currentPage", "parcels");
            return "parcels/list";
        } catch (Exception e) {
            log.error("Error loading parcel list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading parcels: " + e.getMessage());
            model.addAttribute("parcels", Collections.emptyList());
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("availableCount", 0L);
            model.addAttribute("allocatedCount", 0L);
            model.addAttribute("disputedCount", 0L);
            model.addAttribute("pageTitle", "Parcel Management");
            model.addAttribute("currentPage", "parcels");
            return "parcels/list";
        }
    }"""

content = re.sub(r'    @GetMapping\n    public String list\([\s\S]*?        }\n    \}', new_method, content, count=1)

with open('./src/main/java/za/co/taloms/parcel/presentation/ParcelPageController.java', 'w') as f:
    f.write(content)
