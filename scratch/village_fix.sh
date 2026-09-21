# Service
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/village_service.patch
@@ -8,6 +8,8 @@
     VillageResponse update(Long id, VillageRequest request);
     VillageResponse findById(Long id);
     List<VillageResponse> findAll();
+    za.co.taloms.common.PageResponse<VillageResponse> findAll(Integer page, Integer size);
+    za.co.taloms.common.PageResponse<VillageResponse> findByAuthority(Long authorityId, Integer page, Integer size);
     List<VillageResponse> findByAuthority(Long authorityId);
     List<VillageResponse> findByHeadmanId(Long headmanId);
     List<VillageResponse> searchByName(String name);
INNEREOF
patch src/main/java/za/co/taloms/traditionalauthority/application/service/VillageService.java < /Users/user/Documents/taloms/scratch/village_service.patch

# Port
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/village_port.patch
@@ -10,6 +10,8 @@
     Optional<Village> findById(Long id);
     List<Village> findAll();
     List<Village> findByTraditionalAuthorityId(Long authorityId);
+    org.springframework.data.domain.Page<Village> findAll(org.springframework.data.domain.Pageable pageable);
+    org.springframework.data.domain.Page<Village> findByTraditionalAuthorityId(Long authorityId, org.springframework.data.domain.Pageable pageable);
     List<Village> findByHeadmanId(Long headmanId);
     boolean existsByVillageName(String name);
     boolean existsByVillageNameAndIdNot(String name, Long id);
INNEREOF
patch src/main/java/za/co/taloms/traditionalauthority/domain/repository/VillageRepositoryPort.java < /Users/user/Documents/taloms/scratch/village_port.patch

# Adapter
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/village_adapter.patch
@@ -32,6 +32,16 @@
     }
 
     @Override
+    public org.springframework.data.domain.Page<Village> findAll(org.springframework.data.domain.Pageable pageable) {
+        return jpaRepository.findAll(pageable);
+    }
+
+    @Override
+    public org.springframework.data.domain.Page<Village> findByTraditionalAuthorityId(Long authorityId, org.springframework.data.domain.Pageable pageable) {
+        return jpaRepository.findByTraditionalAuthorityId(authorityId, pageable);
+    }
+
+    @Override
     public List<Village> findByTraditionalAuthorityId(Long authorityId) {
         return jpaRepository.findByTraditionalAuthorityIdOrderByVillageNameAsc(authorityId);
     }
INNEREOF
patch src/main/java/za/co/taloms/traditionalauthority/infrastructure/repository/VillageRepositoryAdapter.java < /Users/user/Documents/taloms/scratch/village_adapter.patch

# JpaRepository
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/village_jpa.patch
@@ -10,6 +10,7 @@
 public interface VillageJpaRepository extends JpaRepository<Village, Long> {
     Optional<Village> findByVillageName(String villageName);
     List<Village> findByTraditionalAuthorityIdOrderByVillageNameAsc(Long authorityId);
+    org.springframework.data.domain.Page<Village> findByTraditionalAuthorityId(Long authorityId, org.springframework.data.domain.Pageable pageable);
     List<Village> findByHeadmanId(Long headmanId);
     boolean existsByVillageName(String villageName);
     @Query("SELECT COUNT(v) > 0 FROM Village v WHERE v.headmanId = :headmanId AND v.id <> :excludeId")
INNEREOF
patch src/main/java/za/co/taloms/traditionalauthority/infrastructure/repository/VillageJpaRepository.java < /Users/user/Documents/taloms/scratch/village_jpa.patch

# Impl
cat << 'INNEREOF' > /Users/user/Documents/taloms/scratch/village_impl.patch
@@ -128,6 +128,34 @@
 
     @Override
     @Transactional(readOnly = true)
+    public za.co.taloms.common.PageResponse<VillageResponse> findAll(Integer page, Integer size) {
+        org.springframework.data.domain.Pageable pageable = za.co.taloms.common.pagination.PageRequestUtils.toPageable(page, size, za.co.taloms.common.ApplicationConstants.DEFAULT_PAGE_SIZE, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
+        org.springframework.data.domain.Page<Village> entityPage = villageRepository.findAll(pageable);
+        return za.co.taloms.common.PageResponse.<VillageResponse>builder()
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
+    public za.co.taloms.common.PageResponse<VillageResponse> findByAuthority(Long authorityId, Integer page, Integer size) {
+        org.springframework.data.domain.Pageable pageable = za.co.taloms.common.pagination.PageRequestUtils.toPageable(page, size, za.co.taloms.common.ApplicationConstants.DEFAULT_PAGE_SIZE, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
+        org.springframework.data.domain.Page<Village> entityPage = villageRepository.findByTraditionalAuthorityId(authorityId, pageable);
+        return za.co.taloms.common.PageResponse.<VillageResponse>builder()
+                .content(entityPage.getContent().stream().map(this::toResponse).toList())
+                .pageNumber(entityPage.getNumber() + 1)
+                .pageSize(entityPage.getSize())
+                .totalElements(entityPage.getTotalElements())
+                .totalPages(entityPage.getTotalPages())
+                .last(entityPage.isLast())
+                .build();
+    }
+
     public List<VillageResponse> findAll() {
         return villageRepository.findAll().stream()
                 .map(this::toResponse)
INNEREOF
patch src/main/java/za/co/taloms/traditionalauthority/application/service/VillageServiceImpl.java < /Users/user/Documents/taloms/scratch/village_impl.patch

