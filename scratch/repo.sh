sed -i '' 's/List<Company> findAll();/org.springframework.data.domain.Page<Company> findAll(org.springframework.data.domain.Pageable pageable);/' src/main/java/za/co/taloms/company/domain/repository/CompanyRepositoryPort.java

sed -i '' 's/List<Company> findAll()/org.springframework.data.domain.Page<Company> findAll(org.springframework.data.domain.Pageable pageable)/' src/main/java/za/co/taloms/company/infrastructure/repository/CompanyRepositoryAdapter.java
sed -i '' 's/return jpaRepository.findAllOrderByCreatedAtDesc();/return jpaRepository.findAll(pageable);/' src/main/java/za/co/taloms/company/infrastructure/repository/CompanyRepositoryAdapter.java

sed -i '' '/@Query("SELECT c FROM Company c ORDER BY c.createdAt DESC")/d' src/main/java/za/co/taloms/company/infrastructure/repository/CompanyJpaRepository.java
sed -i '' 's/List<Company> findAllOrderByCreatedAtDesc();/org.springframework.data.domain.Page<Company> findAll(org.springframework.data.domain.Pageable pageable);/' src/main/java/za/co/taloms/company/infrastructure/repository/CompanyJpaRepository.java
