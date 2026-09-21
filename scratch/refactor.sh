# 1. Update CompanyService & CompanyServiceImpl
sed -i '' 's/List<CompanyResponse> findAll();/za.co.taloms.common.PageResponse<CompanyResponse> findAll(Integer page, Integer size);/' src/main/java/za/co/taloms/company/application/service/CompanyService.java

# We need to replace findAll in CompanyServiceImpl
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/CompanyServiceImpl.patch
@@ -69,8 +69,17 @@
 
     @Override
     @Transactional(readOnly = true)
-    public List<CompanyResponse> findAll() {
-        return companyRepository.findAll().stream().map(this::toResponse).toList();
+    public za.co.taloms.common.PageResponse<CompanyResponse> findAll(Integer page, Integer size) {
+        org.springframework.data.domain.Pageable pageable = za.co.taloms.common.pagination.PageRequestUtils.toPageable(page, size, za.co.taloms.common.ApplicationConstants.DEFAULT_PAGE_SIZE, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
+        org.springframework.data.domain.Page<Company> entityPage = companyRepository.findAll(pageable);
+        return za.co.taloms.common.PageResponse.<CompanyResponse>builder()
+                .content(entityPage.getContent().stream().map(this::toResponse).toList())
+                .pageNumber(entityPage.getNumber() + 1)
+                .pageSize(entityPage.getSize())
+                .totalElements(entityPage.getTotalElements())
+                .totalPages(entityPage.getTotalPages())
+                .last(entityPage.isLast())
+                .build();
     }
 
     @Override
INNEREOF
patch src/main/java/za/co/taloms/company/application/service/CompanyServiceImpl.java < /Users/user/Documents/taloms/scratch/CompanyServiceImpl.patch
