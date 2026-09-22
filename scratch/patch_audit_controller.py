path = "src/main/java/za/co/taloms/audit/presentation/AuditPageController.java"
with open(path, "r") as f: c = f.read()

import re

if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import org.springframework.web.bind.annotation.GetMapping;", "import org.springframework.web.bind.annotation.GetMapping;\nimport org.springframework.web.bind.annotation.RequestParam;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport za.co.taloms.common.pagination.PageRequestUtils;")

# Patch index()
c = c.replace("public String index(Model model) {", "public String index(Model model, @RequestParam(required = false, defaultValue = \"1\") Integer page) {")

def patch_index(match):
    return """
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<za.co.taloms.audit.application.dto.AuditLogResponse> pageObj = auditService.findAll(pageable);
            model.addAttribute("page", pageObj);
            model.addAttribute("logs", pageObj.getContent());
            model.addAttribute("totalCount", pageObj.getTotalElements());
"""

c = re.sub(r'            var logs = auditService\.findAll\(\);\s*model\.addAttribute\("logs", logs\);\s*model\.addAttribute\("totalCount", auditService\.countAll\(\)\);', patch_index, c, flags=re.DOTALL)

with open(path, "w") as f: f.write(c)
