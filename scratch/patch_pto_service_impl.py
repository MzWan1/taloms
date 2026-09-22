path = "src/main/java/za/co/taloms/pto/application/service/PTOServiceImpl.java"
with open(path, "r") as f: c = f.read()

if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.stream.Collectors;", "import java.util.stream.Collectors;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport org.springframework.data.domain.PageImpl;")

additions = """
    @Override
    @Transactional(readOnly = true)
    public Page<PTOResponse> findAll(Pageable pageable) {
        return repositoryPort.findAll(pageable).map(ptoMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PTOResponse> search(PTOSearchCriteria criteria, Pageable pageable) {
        return repositoryPort.search(
                criteria.getHolderName(),
                criteria.getIdNumber(),
                criteria.getPtoNumber(),
                criteria.getStatus(),
                criteria.getPurpose(),
                criteria.getVillageId(),
                criteria.getAuthorityId(),
                pageable
        ).map(ptoMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PTOResponse> findByAuthority(Long authorityId, Pageable pageable) {
        return repositoryPort.findByTraditionalAuthorityId(authorityId, pageable).map(ptoMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PTOResponse> findDeleted(Pageable pageable) {
        return repositoryPort.findDeleted(pageable).map(ptoMapper::toDto);
    }
"""

c = c.replace("public List<PTOResponse> findAll() {", additions + "\n    @Override\n    @Transactional(readOnly = true)\n    public List<PTOResponse> findAll() {")

with open(path, "w") as f: f.write(c)
