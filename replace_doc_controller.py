import re

with open('./src/main/java/za/co/taloms/document/presentation/DocumentPageController.java', 'r') as f:
    content = f.read()

new_method = """    @GetMapping
    public String list(@org.springframework.data.web.PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable, Model model) {
        try {
            org.springframework.data.domain.Page<DocumentResponse> page = documentService.findAll(pageable);
            model.addAttribute("page", page);
            model.addAttribute("documents", page.getContent());
            model.addAttribute("totalCount", page.getTotalElements());
            model.addAttribute("documentTypes", DocumentType.values());
            model.addAttribute("entityTypes", EntityType.values());
            model.addAttribute("pageTitle", "Document Management");
            model.addAttribute("currentPage", "documents");
            return "documents/list";
        } catch (Exception e) {
            log.error("Error loading document list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading documents: " + e.getMessage());
            model.addAttribute("documents", Collections.emptyList());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("documentTypes", DocumentType.values());
            model.addAttribute("entityTypes", EntityType.values());
            model.addAttribute("pageTitle", "Document Management");
            model.addAttribute("currentPage", "documents");
            return "documents/list";
        }
    }"""

content = re.sub(r'    @GetMapping\n    public String list\(Model model\) \{[\s\S]*?        }\n    \}', new_method, content, count=1)

with open('./src/main/java/za/co/taloms/document/presentation/DocumentPageController.java', 'w') as f:
    f.write(content)
