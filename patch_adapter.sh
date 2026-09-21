#!/bin/bash
sed -i '' -e '/public List<PTO> findByStatus(PTOStatus status) {/i\
    @Override\
    public List<PTO> findTop5ByStatusOrderByCreatedAtDesc(PTOStatus status) {\
        return jpaRepository.findTop5ByStatusOrderByCreatedAtDesc(status);\
    }\
\
    @Override\
    public List<PTO> findTop5ByVillageIdInAndStatusOrderByCreatedAtDesc(Set<Long> villageIds, PTOStatus status) {\
        return jpaRepository.findTop5ByVillageIdInAndStatusOrderByCreatedAtDesc(villageIds, status);\
    }\
\
' src/main/java/za/co/taloms/pto/infrastructure/repository/PTORepositoryAdapter.java
