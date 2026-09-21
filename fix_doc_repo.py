with open('/Users/user/Documents/taloms/src/main/java/za/co/taloms/document/domain/repository/DocumentRepositoryPort.java', 'r') as f:
    text = f.read()
text = text.replace('List<Document> findAll();', 'List<Document> findAll();\n    org.springframework.data.domain.Page<Document> findAll(org.springframework.data.domain.Pageable pageable);')
with open('/Users/user/Documents/taloms/src/main/java/za/co/taloms/document/domain/repository/DocumentRepositoryPort.java', 'w') as f:
    f.write(text)
