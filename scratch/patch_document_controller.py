path = "src/main/java/za/co/taloms/document/presentation/DocumentPageController.java"
with open(path, "r") as f: c = f.read()

import re

if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Collections;", "import java.util.Collections;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport org.springframework.web.bind.annotation.RequestParam;\nimport za.co.taloms.common.pagination.PageRequestUtils;")

# Patch list()
c = c.replace("public String list(Model model) {", "public String list(Model model, @RequestParam(required = false, defaultValue = \"1\") Integer page) {")

def patch_list(match):
    return """
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<za.co.taloms.document.application.dto.DocumentResponse> pageObj = documentService.findAll(pageable);
            model.addAttribute("page", pageObj);
            model.addAttribute("documents", pageObj.getContent());
            model.addAttribute("totalCount", pageObj.getTotalElements());
"""

c = re.sub(r'            var documents = documentService\.findAll\(\);\s*model\.addAttribute\("documents", documents\);\s*model\.addAttribute\("totalCount", documentService\.countAll\(\)\);', patch_list, c, flags=re.DOTALL)

# Patch listByEntity()
c = c.replace("public String listByEntity(@PathVariable String entityType,\n                               @PathVariable Long entityId,\n                               Model model) {", "public String listByEntity(@PathVariable String entityType,\n                               @PathVariable Long entityId,\n                               Model model, @RequestParam(required = false, defaultValue = \"1\") Integer page) {")

def patch_listByEntity(match):
    return """
            var entityTypeEnum = EntityType.valueOf(entityType);
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<za.co.taloms.document.application.dto.DocumentResponse> pageObj = documentService.findByRelatedEntity(entityTypeEnum, entityId, pageable);
            model.addAttribute("page", pageObj);
            model.addAttribute("documents", pageObj.getContent());
            model.addAttribute("entityType", entityType);
            model.addAttribute("entityId", entityId);
            model.addAttribute("totalCount", pageObj.getTotalElements());
"""

c = re.sub(r'            var entityTypeEnum = EntityType\.valueOf\(entityType\);\s*var documents = documentService\.findByRelatedEntity\(entityTypeEnum, entityId\);\s*model\.addAttribute\("documents", documents\);\s*model\.addAttribute\("entityType", entityType\);\s*model\.addAttribute\("entityId", entityId\);\s*model\.addAttribute\("totalCount", documents\.size\(\)\);', patch_listByEntity, c, flags=re.DOTALL)

with open(path, "w") as f: f.write(c)
