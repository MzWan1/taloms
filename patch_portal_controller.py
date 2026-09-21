import re

with open('./src/main/java/za/co/taloms/useraccess/presentation/UserPortalController.java', 'r') as f:
    content = f.read()

new_method = """    @GetMapping
    public String index(Model model,
                        @RequestParam(required = false) Long ptoId,
                        @RequestParam(required = false, defaultValue = "my") String tab,
                        @RequestParam(required = false) String q,
                        @org.springframework.data.web.PageableDefault(size = 10) org.springframework.data.domain.Pageable pageable) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }

        List<PTOResponse> accessibleAll = userAccessService
                .findAccessiblePtoIds(user.getId()).stream()
                .map(ptoService::findById)
                .toList();
                
        List<PTOResponse> ownedAll = userAccessService
                .findOwnedPtoIds(user.getId()).stream()
                .map(ptoService::findById)
                .toList();
                
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        
        int aStart = Math.min(page * size, accessibleAll.size());
        int aEnd = Math.min((page + 1) * size, accessibleAll.size());
        org.springframework.data.domain.Page<PTOResponse> accessible = new org.springframework.data.domain.PageImpl<>(accessibleAll.subList(aStart, aEnd), pageable, accessibleAll.size());

        int oStart = Math.min(page * size, ownedAll.size());
        int oEnd = Math.min((page + 1) * size, ownedAll.size());
        org.springframework.data.domain.Page<PTOResponse> owned = new org.springframework.data.domain.PageImpl<>(ownedAll.subList(oStart, oEnd), pageable, ownedAll.size());

        if (ptoId != null && "my".equals(tab)) {
            tab = "manage";
        }
        Long selectedId = null;
        PTOResponse selectedPto = null;
        if (ptoId != null && ownedAll.stream().anyMatch(p -> p.getId().equals(ptoId))) {
            selectedId = ptoId;
            selectedPto = ownedAll.stream()
                    .filter(p -> p.getId().equals(ptoId)).findFirst().orElse(null);
        } else if (!ownedAll.isEmpty()) {
            selectedId = ownedAll.get(0).getId();
            selectedPto = ownedAll.get(0);
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("accessiblePage", accessible);
        model.addAttribute("accessiblePtos", accessible.getContent());
        model.addAttribute("ownedPage", owned);
        model.addAttribute("ownedPtos", owned.getContent());
        model.addAttribute("selectedPtoId", selectedId);
        model.addAttribute("selectedPto", selectedPto);
        if (selectedId != null) {
            model.addAttribute("linkedUsers",
                    userAccessService.listLinkedUsers(selectedId, user.getId()));
        } else {
            model.addAttribute("linkedUsers", List.of());
        }
        model.addAttribute("pageTitle", "My Proofs & PTOs");
        model.addAttribute("currentPage", "portal");
        model.addAttribute("activeTab", "manage".equalsIgnoreCase(tab) ? "manage" : "my");
        model.addAttribute("initialSearch", q == null ? "" : q.trim());
        return "portal/index";
    }"""

content = re.sub(r'    @GetMapping\n    public String index\(Model model,[\s\S]*?        return "portal/index";\n    }', new_method, content, count=1)

with open('./src/main/java/za/co/taloms/useraccess/presentation/UserPortalController.java', 'w') as f:
    f.write(content)
