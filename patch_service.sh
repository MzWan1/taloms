#!/bin/bash
sed -i '' -e '/List<PTOResponse> findByStatus(PTOStatus status);/a\
    List<PTOResponse> findTop5ByStatus(PTOStatus status);\
    List<PTOResponse> findTop5ByVillagesAndStatus(java.util.Set<Long> villageIds, PTOStatus status);\
' src/main/java/za/co/taloms/pto/application/service/PTOService.java
