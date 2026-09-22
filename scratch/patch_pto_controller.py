path = "src/main/java/za/co/taloms/pto/presentation/PTOPageController.java"
with open(path, "r") as f: c = f.read()

import re

# Add Page/Pageable/PageRequestUtils imports if missing
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.stream.Collectors;", "import java.util.stream.Collectors;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport za.co.taloms.common.pagination.PageRequestUtils;")

# Patch list()
def patch_list(match):
    return """
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<PTOResponse> pageObj;

            if (scopedUser && (scopedVillageIds == null || scopedVillageIds.isEmpty())) {
                // Chief/headman scoped to no village sees nothing
                pageObj = new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), pageable, 0);
            } else if ((status != null && !status.isBlank())
                    || (search != null && !search.isBlank())
                    || scopedVillageIds != null) {
                if (scopedVillageIds != null) {
                    criteria.villageIds(scopedVillageIds);
                }
                pageObj = ptoService.search(criteria.build(), pageable);
            } else {
                pageObj = ptoService.findAll(pageable);
            }

            model.addAttribute("page", pageObj);
            model.addAttribute("ptos", pageObj.getContent());
            model.addAttribute("statuses", PTOStatus.values());
            model.addAttribute("purposes", PTOPurpose.values());
            model.addAttribute("totalCount", pageObj.getTotalElements());
            // pendingCount cannot be easily computed from just the page, so we use count queries
            model.addAttribute("pendingCount", scopedUser ? 
                (scopedVillageIds != null && !scopedVillageIds.isEmpty() ? 
                    scopedVillageIds.stream().mapToLong(vid -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.PENDING)).sum() : 0) 
                : ptoService.countByStatus(PTOStatus.PENDING));
            model.addAttribute("activeCount", scopedUser ? 
                (scopedVillageIds != null && !scopedVillageIds.isEmpty() ? 
                    scopedVillageIds.stream().mapToLong(vid -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.ACTIVE)).sum() : 0) 
                : ptoService.countByStatus(PTOStatus.ACTIVE));
            model.addAttribute("revokedCount", scopedUser ? 
                (scopedVillageIds != null && !scopedVillageIds.isEmpty() ? 
                    scopedVillageIds.stream().mapToLong(vid -> ptoService.countByVillageIdAndStatus(vid, PTOStatus.REVOKED)).sum() : 0) 
                : ptoService.countByStatus(PTOStatus.REVOKED));
"""

c = re.sub(r'            List<PTOResponse> ptos;.*?\.count\(\)\);', patch_list, c, flags=re.DOTALL)

# Patch deletedList()
def patch_deleted(match):
    return """
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<PTOResponse> pageObj = ptoService.findDeleted(pageable);
            model.addAttribute("page", pageObj);
            model.addAttribute("ptos", pageObj.getContent());
"""

c = re.sub(r'            var ptos = ptoService\.findDeleted\(\);.*?model\.addAttribute\("ptos", pageObj\.getContent\(\)\);', patch_deleted, c, flags=re.DOTALL)


# Patch byAuthority()
# wait, byAuthority doesn't have page parameter! We need to add it.
c = c.replace("public String byAuthority(@PathVariable Long authorityId, Model model, RedirectAttributes ra) {", "public String byAuthority(@PathVariable Long authorityId, @RequestParam(required = false, defaultValue = \"1\") Integer page, Model model, RedirectAttributes ra) {")

def patch_auth(match):
    return """
            var authority = authorityService.findById(authorityId);
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<PTOResponse> pageObj = ptoService.findByAuthority(authorityId, pageable);
            model.addAttribute("page", pageObj);
            model.addAttribute("ptos", pageObj.getContent());
"""

c = re.sub(r'            var authority = authorityService\.findById\(authorityId\);\s+model\.addAttribute\("ptos", ptoService\.findByAuthority\(authorityId\)\);', patch_auth, c, flags=re.DOTALL)

with open(path, "w") as f: f.write(c)
