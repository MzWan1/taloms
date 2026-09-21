with open('/Users/user/Documents/taloms/src/main/java/za/co/taloms/document/infrastructure/repository/DocumentRepositoryAdapter.java', 'r') as f:
    text = f.read()

text = text.replace('public List<Document> findAll() {', """@Override
    public org.springframework.data.domain.Page<Document> findAll(org.springframework.data.domain.Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public List<Document> findAll() {""")
with open('/Users/user/Documents/taloms/src/main/java/za/co/taloms/document/infrastructure/repository/DocumentRepositoryAdapter.java', 'w') as f:
    f.write(text)
