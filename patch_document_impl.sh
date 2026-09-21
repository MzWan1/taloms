#!/bin/bash
cat << 'INNER_EOF' > /tmp/document_patch.txt
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DocumentResponse> findAll(org.springframework.data.domain.Pageable pageable) {
        return documentRepository.findAll(pageable).map(this::toResponse);
    }
INNER_EOF
sed -i '' -e '/public List<DocumentResponse> findAll() {/r /tmp/document_patch.txt' ./src/main/java/za/co/taloms/document/application/service/DocumentServiceImpl.java
