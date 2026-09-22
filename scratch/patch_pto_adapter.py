path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTORepositoryAdapter.java"
with open(path, "r") as f: c = f.read()

if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.Set;", "import java.util.Set;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")

# findAll()
c = c.replace("public List<PTO> findAll() {\n        return jpaRepository.findAllOrderByCreatedAtDesc();\n    }", "public List<PTO> findAll() {\n        return jpaRepository.findAllOrderByCreatedAtDesc();\n    }\n\n    @Override\n    public Page<PTO> findAll(Pageable pageable) {\n        return jpaRepository.findAllOrderByCreatedAtDesc(pageable);\n    }")

# search()
c = c.replace("public List<PTO> search(String holderName, String idNumber, String ptoNumber,\n                             PTOStatus status, PTOPurpose purpose, Long villageId, Long authorityId) {\n        return jpaRepository.search(holderName, idNumber, ptoNumber, status, purpose, villageId, authorityId);\n    }", "public Page<PTO> search(String holderName, String idNumber, String ptoNumber,\n                             PTOStatus status, PTOPurpose purpose, Long villageId, Long authorityId, Pageable pageable) {\n        return jpaRepository.search(holderName, idNumber, ptoNumber, status, purpose, villageId, authorityId, pageable);\n    }")

# findDeleted()
c = c.replace("public List<PTO> findDeleted() {\n        return jpaRepository.findDeleted();\n    }", "public Page<PTO> findDeleted(Pageable pageable) {\n        return jpaRepository.findDeleted(pageable);\n    }")

# findByTraditionalAuthorityId()
c = c.replace("public List<PTO> findByTraditionalAuthorityId(Long authorityId) {\n        return jpaRepository.findByTraditionalAuthorityId(authorityId);\n    }", "public Page<PTO> findByTraditionalAuthorityId(Long authorityId, Pageable pageable) {\n        return jpaRepository.findByTraditionalAuthorityId(authorityId, pageable);\n    }\n\n    @Override\n    public List<PTO> findByTraditionalAuthorityId(Long authorityId) {\n        return jpaRepository.findByTraditionalAuthorityId(authorityId);\n    }")

with open(path, "w") as f: f.write(c)
