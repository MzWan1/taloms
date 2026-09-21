# 1. TraditionalAuthorityService
sed -i '' 's/List<TraditionalAuthorityResponse> findAll();/za.co.taloms.common.PageResponse<TraditionalAuthorityResponse> findAll(Integer page, Integer size);/' src/main/java/za/co/taloms/traditionalauthority/application/service/TraditionalAuthorityService.java

# 2. TraditionalAuthorityServiceImpl
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/ta_impl.patch
@@ -51,8 +51,17 @@
 
     @Override
     @Transactional(readOnly = true)
-    public List<TraditionalAuthorityResponse> findAll() {
-        return repository.findAll().stream().map(this::toResponse).toList();
+    public za.co.taloms.common.PageResponse<TraditionalAuthorityResponse> findAll(Integer page, Integer size) {
+        org.springframework.data.domain.Pageable pageable = za.co.taloms.common.pagination.PageRequestUtils.toPageable(page, size, za.co.taloms.common.ApplicationConstants.DEFAULT_PAGE_SIZE, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
+        org.springframework.data.domain.Page<TraditionalAuthority> entityPage = repository.findAll(pageable);
+        return za.co.taloms.common.PageResponse.<TraditionalAuthorityResponse>builder()
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
patch src/main/java/za/co/taloms/traditionalauthority/application/service/TraditionalAuthorityServiceImpl.java < /Users/user/Documents/taloms/scratch/ta_impl.patch

# 3. Port
sed -i '' 's/List<TraditionalAuthority> findAll();/org.springframework.data.domain.Page<TraditionalAuthority> findAll(org.springframework.data.domain.Pageable pageable);/' src/main/java/za/co/taloms/traditionalauthority/domain/repository/TraditionalAuthorityRepositoryPort.java

# 4. Adapter
sed -i '' 's/List<TraditionalAuthority> findAll()/org.springframework.data.domain.Page<TraditionalAuthority> findAll(org.springframework.data.domain.Pageable pageable)/' src/main/java/za/co/taloms/traditionalauthority/infrastructure/repository/TraditionalAuthorityRepositoryAdapter.java
sed -i '' 's/return jpaRepository.findAllOrderByCreatedAtDesc();/return jpaRepository.findAll(pageable);/' src/main/java/za/co/taloms/traditionalauthority/infrastructure/repository/TraditionalAuthorityRepositoryAdapter.java

# 5. JpaRepo
sed -i '' '/@Query("SELECT ta FROM TraditionalAuthority ta ORDER BY ta.createdAt DESC")/d' src/main/java/za/co/taloms/traditionalauthority/infrastructure/repository/TraditionalAuthorityJpaRepository.java
sed -i '' 's/List<TraditionalAuthority> findAllOrderByCreatedAtDesc();/org.springframework.data.domain.Page<TraditionalAuthority> findAll(org.springframework.data.domain.Pageable pageable);/' src/main/java/za/co/taloms/traditionalauthority/infrastructure/repository/TraditionalAuthorityJpaRepository.java

