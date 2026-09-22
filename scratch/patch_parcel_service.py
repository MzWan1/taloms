path = "src/main/java/za/co/taloms/parcel/application/service/ParcelService.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Optional;", "import java.util.Optional;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport java.util.Set;\nimport za.co.taloms.parcel.domain.entity.ParcelStatus;")
c = c.replace("List<ParcelResponse> search(String query);", "List<ParcelResponse> search(String query);\n    Page<ParcelResponse> searchParcels(String q, ParcelStatus status, Long villageId, Set<Long> allowedVillageIds, Pageable pageable);")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/parcel/application/service/ParcelServiceImpl.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.stream.Collectors;", "import java.util.stream.Collectors;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport java.util.Set;\nimport za.co.taloms.parcel.domain.entity.ParcelStatus;")
c = c.replace("public List<ParcelResponse> search(String query) {\n        return parcelRepository.search(query).stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }", "public List<ParcelResponse> search(String query) {\n        return parcelRepository.search(query).stream()\n                .map(this::toResponse)\n                .collect(Collectors.toList());\n    }\n\n    @Override\n    @Transactional(readOnly = true)\n    public Page<ParcelResponse> searchParcels(String q, ParcelStatus status, Long villageId, Set<Long> allowedVillageIds, Pageable pageable) {\n        return parcelRepository.searchParcels(q, status, villageId, allowedVillageIds, pageable).map(this::toResponse);\n    }")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/parcel/presentation/ParcelPageController.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import org.springframework.web.bind.annotation.GetMapping;", "import org.springframework.web.bind.annotation.GetMapping;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport za.co.taloms.common.pagination.PageRequestUtils;")

import re

c = c.replace("public String list(\n            @RequestParam(value = \"q\", required = false) String q,\n            @RequestParam(value = \"status\", required = false) ParcelStatus status,\n            @RequestParam(value = \"villageId\", required = false) Long villageId,\n            Model model) {", "public String list(\n            @RequestParam(value = \"q\", required = false) String q,\n            @RequestParam(value = \"status\", required = false) ParcelStatus status,\n            @RequestParam(value = \"villageId\", required = false) Long villageId,\n            @RequestParam(value = \"page\", required = false, defaultValue = \"1\") Integer page,\n            Model model) {")

def patch_list(match):
    return """
            Set<Long> allowedVillageIds = scopedVillageIds();
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            
            Page<za.co.taloms.parcel.application.dto.ParcelResponse> pageObj = parcelService.searchParcels(
                    q != null ? q.trim() : null,
                    status,
                    villageId,
                    allowedVillageIds,
                    pageable
            );

            long availableCount = parcelService.countByStatus(ParcelStatus.AVAILABLE);
            long allocatedCount = parcelService.countByStatus(ParcelStatus.ALLOCATED);
            long disputedCount = parcelService.countByStatus(ParcelStatus.DISPUTED);

            if (allowedVillageIds != null && !allowedVillageIds.isEmpty()) {
                availableCount = allowedVillageIds.stream().mapToLong(vid -> parcelService.countByStatusAndVillageId(ParcelStatus.AVAILABLE, vid)).sum();
                allocatedCount = allowedVillageIds.stream().mapToLong(vid -> parcelService.countByStatusAndVillageId(ParcelStatus.ALLOCATED, vid)).sum();
                disputedCount = allowedVillageIds.stream().mapToLong(vid -> parcelService.countByStatusAndVillageId(ParcelStatus.DISPUTED, vid)).sum();
            } else if (allowedVillageIds != null && allowedVillageIds.isEmpty()) {
                availableCount = 0;
                allocatedCount = 0;
                disputedCount = 0;
            }

            model.addAttribute("page", pageObj);
            model.addAttribute("parcels", pageObj.getContent());
"""

c = re.sub(r'            Set<Long> allowedVillageIds = scopedVillageIds\(\);.*?model\.addAttribute\("parcels", parcels\);', patch_list, c, flags=re.DOTALL)

with open(path, "w") as f: f.write(c)
