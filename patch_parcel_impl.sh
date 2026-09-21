#!/bin/bash
cat << 'INNER_EOF' > /tmp/parcel_patch.txt
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ParcelResponse> searchParcels(String query, ParcelStatus status, Long villageId, java.util.Set<Long> allowedVillageIds, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<za.co.taloms.parcel.domain.entity.Parcel> spec = (root, cq, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (query != null && !query.trim().isEmpty()) {
                String q = "%" + query.trim().toLowerCase() + "%";
                var qPred = cb.or(
                    cb.like(cb.lower(root.get("parcelNumber")), q),
                    cb.like(cb.lower(root.get("standNumber")), q),
                    cb.like(cb.lower(root.join("village", jakarta.persistence.criteria.JoinType.LEFT).get("villageName")), q)
                );
                predicates.add(qPred);
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (villageId != null) {
                predicates.add(cb.equal(root.get("village").get("id"), villageId));
            }
            if (allowedVillageIds != null) {
                if (allowedVillageIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("village").get("id").in(allowedVillageIds));
                }
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return parcelRepository.findAll(spec, pageable).map(this::toResponse);
    }
INNER_EOF
sed -i '' -e '/public List<ParcelResponse> search(String query) {/r /tmp/parcel_patch.txt' ./src/main/java/za/co/taloms/parcel/application/service/ParcelServiceImpl.java
