path = "src/main/java/za/co/taloms/pto/application/service/PTOService.java"
with open(path, "r") as f: c = f.read()

if "org.springframework.data.domain.Page" not in c:
    c = c.replace("import java.util.List;", "import java.util.List;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;")

c = c.replace("List<PTOResponse> findAll();", "List<PTOResponse> findAll();\n    Page<PTOResponse> findAll(Pageable pageable);")
c = c.replace("List<PTOResponse> search(PTOSearchCriteria criteria);", "List<PTOResponse> search(PTOSearchCriteria criteria);\n    Page<PTOResponse> search(PTOSearchCriteria criteria, Pageable pageable);")
c = c.replace("List<PTOResponse> findByAuthority(Long authorityId);", "List<PTOResponse> findByAuthority(Long authorityId);\n    Page<PTOResponse> findByAuthority(Long authorityId, Pageable pageable);")
c = c.replace("List<PTOResponse> findDeleted();", "List<PTOResponse> findDeleted();\n    Page<PTOResponse> findDeleted(Pageable pageable);")

with open(path, "w") as f: f.write(c)
