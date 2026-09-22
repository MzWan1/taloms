path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTOJpaRepository.java"
with open(path, "r") as f: c = f.read()

import re

# Add Page and Pageable imports if not present
if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Set;", "import java.util.Set;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")

# Change search to Page
c = c.replace("List<PTO> search(@Param(\"holderName\") String holderName,", "Page<PTO> search(@Param(\"holderName\") String holderName,")
c = c.replace("@Param(\"authorityId\") Long authorityId);", "@Param(\"authorityId\") Long authorityId, Pageable pageable);")

# Change findAllOrderByCreatedAtDesc to Page
c = c.replace("List<PTO> findAllOrderByCreatedAtDesc();", "Page<PTO> findAllOrderByCreatedAtDesc(Pageable pageable);")

# Change findDeleted
c = c.replace("List<PTO> findDeleted();", "Page<PTO> findDeleted(Pageable pageable);")

# Change findByTraditionalAuthorityId
c = c.replace("List<PTO> findByTraditionalAuthorityId(@Param(\"authorityId\") Long authorityId);", "Page<PTO> findByTraditionalAuthorityId(@Param(\"authorityId\") Long authorityId, Pageable pageable);")

with open(path, "w") as f: f.write(c)
