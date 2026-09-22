path = "src/main/java/za/co/taloms/pto/domain/repository/PTORepositoryPort.java"
with open(path, "r") as f: c = f.read()

if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Set;", "import java.util.Set;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")

c = c.replace("List<PTO> search(String holderName, String idNumber, String ptoNumber, PTOStatus status,\n                     za.co.taloms.pto.domain.entity.PTOPurpose purpose, Long villageId, Long authorityId);", "Page<PTO> search(String holderName, String idNumber, String ptoNumber, PTOStatus status,\n                     za.co.taloms.pto.domain.entity.PTOPurpose purpose, Long villageId, Long authorityId, Pageable pageable);")

c = c.replace("List<PTO> findDeleted();", "Page<PTO> findDeleted(Pageable pageable);")

c = c.replace("List<PTO> findByTraditionalAuthorityId(Long authorityId);", "Page<PTO> findByTraditionalAuthorityId(Long authorityId, Pageable pageable);")

with open(path, "w") as f: f.write(c)
