package za.co.taloms.document.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.document.application.dto.DocumentResponse;
import za.co.taloms.document.application.dto.DocumentUploadRequest;
import za.co.taloms.document.application.service.DocumentService;
import za.co.taloms.document.domain.entity.DocumentType;
import za.co.taloms.document.domain.entity.EntityType;

import java.util.Collections;

@Slf4j
@Controller
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentPageController {

    private final DocumentService documentService;

    @GetMapping
    public String list(Model model) {
        try {
            var documents = documentService.findAll();
            model.addAttribute("documents", documents);
            model.addAttribute("totalCount", documentService.countAll());
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
    }

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        try {
            if (!model.containsAttribute("request")) {
                model.addAttribute("request", new DocumentUploadRequest());
            }

            model.addAttribute("documentTypes", DocumentType.values());
            model.addAttribute("entityTypes", EntityType.values());
            model.addAttribute("pageTitle", "Upload Document");
            model.addAttribute("currentPage", "documents");
            return "documents/upload";
        } catch (Exception e) {
            log.error("Error loading upload form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            model.addAttribute("documentTypes", DocumentType.values());
            model.addAttribute("entityTypes", EntityType.values());
            model.addAttribute("pageTitle", "Upload Document");
            model.addAttribute("currentPage", "documents");
            return "documents/upload";
        }
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public String listByEntity(@PathVariable String entityType,
                               @PathVariable Long entityId,
                               Model model) {
        try {
            var entityTypeEnum = EntityType.valueOf(entityType);
            var documents = documentService.findByRelatedEntity(entityTypeEnum, entityId);
            model.addAttribute("documents", documents);
            model.addAttribute("entityType", entityType);
            model.addAttribute("entityId", entityId);
            model.addAttribute("totalCount", documents.size());
            model.addAttribute("documentTypes", DocumentType.values());
            model.addAttribute("entityTypes", EntityType.values());
            model.addAttribute("pageTitle", "Documents - " + entityType + " #" + entityId);
            model.addAttribute("currentPage", "documents");
            return "documents/list";
        } catch (Exception e) {
            log.error("Error loading documents by entity: {}", e.getMessage(), e);
            return "redirect:/documents";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            var document = documentService.findById(id);
            var accessLogs = documentService.getDocumentAccessLogs(id);
            model.addAttribute("document", document);
            model.addAttribute("accessLogs", accessLogs);
            model.addAttribute("pageTitle", "Document - " + document.getOriginalFilename());
            model.addAttribute("currentPage", "documents");
            return "documents/detail";
        } catch (Exception e) {
            log.error("Error loading document detail: {}", e.getMessage(), e);
            return "redirect:/documents";
        }
    }

    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes ra) {
        try {
            var response = documentService.deactivateDocument(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Document deactivated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/documents/" + id;
    }

    @PostMapping("/activate/{id}")
    public String activate(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes ra) {
        try {
            var response = documentService.activateDocument(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Document activated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/documents/" + id;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes ra) {
        try {
            documentService.deleteDocument(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Document deleted successfully.");
            return "redirect:/documents";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
            return "redirect:/documents/" + id;
        }
    }
}