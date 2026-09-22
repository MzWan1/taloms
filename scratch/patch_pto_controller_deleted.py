import re
path = "src/main/java/za/co/taloms/pto/presentation/PTOPageController.java"
with open(path, "r") as f: c = f.read()

def patch_deleted(match):
    return """
            boolean scopedUser = scopeService.isCurrentUserChiefOrHeadsman();
            Set<Long> scopedVillageIds = scopedUser ? scopeService.scopedVillageIds() : null;
            
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<PTOResponse> pageObj;
            
            if (scopedUser) {
                if (scopedVillageIds == null || scopedVillageIds.isEmpty()) {
                    pageObj = new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), pageable, 0);
                } else {
                    pageObj = ptoService.findDeletedScoped(scopedVillageIds, pageable);
                }
            } else {
                pageObj = ptoService.findDeleted(pageable);
            }

            model.addAttribute("page", pageObj);
            model.addAttribute("ptos", pageObj.getContent());
            model.addAttribute("pageTitle", "Deleted PTOs");
"""

c = re.sub(r'            var deletedPtos = ptoService\.findDeleted\(\);.*?model\.addAttribute\("pageTitle", "Deleted PTOs"\);', patch_deleted, c, flags=re.DOTALL)

with open(path, "w") as f: f.write(c)
