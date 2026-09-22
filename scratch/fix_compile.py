path = "src/main/java/za/co/taloms/pto/presentation/PTOPageController.java"
with open(path, "r") as f: c = f.read()
import re
c = re.sub(r'            model\.addAttribute\("activeCount", ptos\.stream\(\)\n\s*\.filter\(p -> p\.getStatus\(\) == PTOStatus\.ACTIVE\)\.count\(\)\);\n\s*model\.addAttribute\("revokedCount", ptos\.stream\(\)\n\s*\.filter\(p -> p\.getStatus\(\) == PTOStatus\.REVOKED\)\.count\(\)\);\n', '', c)
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/pto/infrastructure/repository/PTORepositoryAdapter.java"
with open(path, "r") as f: c = f.read()
c = c.replace("public List<PTO> findAll() {\n        return jpaRepository.findAllOrderByCreatedAtDesc();", "public List<PTO> findAll() {\n        return jpaRepository.findAllOrderByCreatedAtDesc(org.springframework.data.domain.Pageable.unpaged()).getContent();")
c = c.replace("public List<PTO> findByTraditionalAuthorityId(Long authorityId) {\n        return jpaRepository.findByTraditionalAuthorityId(authorityId);\n    }", "public List<PTO> findByTraditionalAuthorityId(Long authorityId) {\n        return jpaRepository.findByTraditionalAuthorityId(authorityId, org.springframework.data.domain.Pageable.unpaged()).getContent();\n    }")
c = c.replace("public List<PTO> search(String holderName, String idNumber, String ptoNumber,\n                             PTOStatus status, PTOPurpose purpose, Set<Long> villageIds, Long authorityId) {\n        return jpaRepository.search(holderName, idNumber, ptoNumber, status, purpose, (villageIds != null && villageIds.isEmpty()) ? null : villageIds, authorityId);", "public List<PTO> search(String holderName, String idNumber, String ptoNumber,\n                             PTOStatus status, PTOPurpose purpose, Set<Long> villageIds, Long authorityId) {\n        return jpaRepository.search(holderName, idNumber, ptoNumber, status, purpose, (villageIds != null && villageIds.isEmpty()) ? null : villageIds, authorityId, org.springframework.data.domain.Pageable.unpaged()).getContent();")

with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/pto/application/service/PTOServiceImpl.java"
with open(path, "r") as f: c = f.read()
c = c.replace("return ptoRepository.findByTraditionalAuthorityId(authorityId).stream()", "return ptoRepository.findByTraditionalAuthorityId(authorityId, org.springframework.data.domain.Pageable.unpaged()).getContent().stream()")
c = c.replace("return ptoRepository.search(\n                criteria.getHolderName(),\n                criteria.getIdNumber(),\n                criteria.getPtoNumber(),\n                criteria.getStatus(),\n                criteria.getPurpose(),\n                criteria.getVillageIds(),\n                criteria.getAuthorityId()\n        ).stream()", "return ptoRepository.search(\n                criteria.getHolderName(),\n                criteria.getIdNumber(),\n                criteria.getPtoNumber(),\n                criteria.getStatus(),\n                criteria.getPurpose(),\n                criteria.getVillageIds(),\n                criteria.getAuthorityId(),\n                org.springframework.data.domain.Pageable.unpaged()\n        ).getContent().stream()")
c = c.replace("return ptoRepository.findDeleted().stream()", "return ptoRepository.findDeleted(org.springframework.data.domain.Pageable.unpaged()).getContent().stream()")
with open(path, "w") as f: f.write(c)
