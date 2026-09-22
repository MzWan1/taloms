path = "src/main/java/za/co/taloms/parcel/application/service/ParcelServiceImpl.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Pageable" not in c:
    c = c.replace("import lombok.RequiredArgsConstructor;", "import lombok.RequiredArgsConstructor;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport java.util.Set;\nimport java.util.stream.Collectors;")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/parcel/application/service/ParcelService.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Pageable" not in c:
    c = c.replace("import java.util.List;", "import java.util.List;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport java.util.Set;")
with open(path, "w") as f: f.write(c)
