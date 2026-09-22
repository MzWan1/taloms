path = "src/main/java/za/co/taloms/pto/application/service/PTOServiceImpl.java"
with open(path, "r") as f: c = f.read()

c = c.replace("@Override\n    @Transactional(readOnly = true)\n    \n    @Override\n    @Transactional(readOnly = true)\n    public Page<PTOResponse> findAll(Pageable pageable) {", "@Override\n    @Transactional(readOnly = true)\n    public Page<PTOResponse> findAll(Pageable pageable) {")

with open(path, "w") as f: f.write(c)
