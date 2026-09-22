path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTORepositoryAdapter.java"
with open(path, "r") as f: c = f.read()
c = c.replace("    @Override\n    public List<PTO> findByTraditionalAuthorityId(Long authorityId) {\n        return jpaRepository.findByTraditionalAuthorityId(authorityId, org.springframework.data.domain.Pageable.unpaged()).getContent();\n    }", "")
with open(path, "w") as f: f.write(c)
