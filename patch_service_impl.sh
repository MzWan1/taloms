#!/bin/bash
sed -i '' -e '/public List<PTOResponse> findByStatus(PTOStatus status) {/i\
    @Override\
    @Transactional(readOnly = true)\
    public List<PTOResponse> findTop5ByStatus(PTOStatus status) {\
        return repositoryPort.findTop5ByStatusOrderByCreatedAtDesc(status).stream()\
                .map(ptoMapper::toResponse)\
                .collect(Collectors.toList());\
    }\
\
    @Override\
    @Transactional(readOnly = true)\
    public List<PTOResponse> findTop5ByVillagesAndStatus(java.util.Set<Long> villageIds, PTOStatus status) {\
        if (villageIds == null || villageIds.isEmpty()) return new java.util.ArrayList<>();\
        return repositoryPort.findTop5ByVillageIdInAndStatusOrderByCreatedAtDesc(villageIds, status).stream()\
                .map(ptoMapper::toResponse)\
                .collect(Collectors.toList());\
    }\
\
' src/main/java/za/co/taloms/pto/application/service/PTOServiceImpl.java
