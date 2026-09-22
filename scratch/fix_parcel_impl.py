path = "src/main/java/za/co/taloms/parcel/application/service/ParcelServiceImpl.java"
with open(path, "r") as f: c = f.read()

import re
c = re.sub(r'@Override\s*@Transactional\(readOnly = true\)\s*@Override\s*@Transactional\(readOnly = true\)\s*public Page<ParcelResponse> searchParcels', 
           r'@Override\n    @Transactional(readOnly = true)\n    public Page<ParcelResponse> searchParcels', c)

c = c.replace("    public List<ParcelResponse> search(String query) {", "    @Override\n    @Transactional(readOnly = true)\n    public List<ParcelResponse> search(String query) {")

with open(path, "w") as f: f.write(c)
