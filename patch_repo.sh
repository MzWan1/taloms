#!/bin/bash
sed -i '' -e '/List<PTO> findByStatus(PTOStatus status);/a\
\
    List<PTO> findTop5ByStatusOrderByCreatedAtDesc(PTOStatus status);\
\
    List<PTO> findTop5ByVillageIdInAndStatusOrderByCreatedAtDesc(Set<Long> villageIds, PTOStatus status);\
' src/main/java/za/co/taloms/pto/infrastructure/repository/PTOJpaRepository.java
