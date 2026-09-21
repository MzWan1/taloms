# Port
sed -i '' 's/org.springframework.data.domain.Page<TraditionalAuthority> findAll(org.springframework.data.domain.Pageable pageable);/org.springframework.data.domain.Page<TraditionalAuthority> findAll(org.springframework.data.domain.Pageable pageable);\n    List<TraditionalAuthority> findAll();/' src/main/java/za/co/taloms/traditionalauthority/domain/repository/TraditionalAuthorityRepositoryPort.java

# Adapter
sed -i '' 's/org.springframework.data.domain.Page<TraditionalAuthority> findAll(org.springframework.data.domain.Pageable pageable)/org.springframework.data.domain.Page<TraditionalAuthority> findAll(org.springframework.data.domain.Pageable pageable)/' src/main/java/za/co/taloms/traditionalauthority/infrastructure/repository/TraditionalAuthorityRepositoryAdapter.java
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/adapter.patch
@@ -34,6 +34,11 @@
     }
 
     @Override
+    public List<TraditionalAuthority> findAll() {
+        return jpaRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
+    }
+
+    @Override
     public List<TraditionalAuthority> findAllActive() {
         return jpaRepository.findByActiveTrueOrderByAuthorityNameAsc();
     }
INNEREOF
patch src/main/java/za/co/taloms/traditionalauthority/infrastructure/repository/TraditionalAuthorityRepositoryAdapter.java < /Users/user/Documents/taloms/scratch/adapter.patch

# JpaRepo: restore the old one for findAll if needed? No, JpaRepository already has findAll(). But wait, `findAll(Sort sort)` is supported.

# Service
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/service.patch
@@ -21,7 +21,7 @@
      *
      * @return A list of all traditional authorities, sorted newest first.
      */
-    za.co.taloms.common.PageResponse<TraditionalAuthorityResponse> findAll(Integer page, Integer size);
+    za.co.taloms.common.PageResponse<TraditionalAuthorityResponse> findAll(Integer page, Integer size);
     
+    List<TraditionalAuthorityResponse> findAll();
+
INNEREOF
patch src/main/java/za/co/taloms/traditionalauthority/application/service/TraditionalAuthorityService.java < /Users/user/Documents/taloms/scratch/service.patch

# ServiceImpl
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/impl.patch
@@ -171,6 +171,22 @@
     @Override
     @Transactional(readOnly = true)
     @Cacheable("authorities")
+    public za.co.taloms.common.PageResponse<TraditionalAuthorityResponse> findAll(Integer page, Integer size) {
+        org.springframework.data.domain.Pageable pageable = za.co.taloms.common.pagination.PageRequestUtils.toPageable(page, size, za.co.taloms.common.ApplicationConstants.DEFAULT_PAGE_SIZE, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
+        org.springframework.data.domain.Page<TraditionalAuthority> entityPage = authorityRepository.findAll(pageable);
+        return za.co.taloms.common.PageResponse.<TraditionalAuthorityResponse>builder()
+                .content(entityPage.getContent().stream().map(this::toResponse).toList())
+                .pageNumber(entityPage.getNumber() + 1)
+                .pageSize(entityPage.getSize())
+                .totalElements(entityPage.getTotalElements())
+                .totalPages(entityPage.getTotalPages())
+                .last(entityPage.isLast())
+                .build();
+    }
+
+    @Override
+    @Transactional(readOnly = true)
+    @Cacheable("authoritiesList")
     public List<TraditionalAuthorityResponse> findAll() {
         List<TraditionalAuthority> all = authorityRepository.findAll();
INNEREOF
patch src/main/java/za/co/taloms/traditionalauthority/application/service/TraditionalAuthorityServiceImpl.java < /Users/user/Documents/taloms/scratch/impl.patch
