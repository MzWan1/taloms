path = "src/main/java/za/co/taloms/parcel/presentation/ParcelPageController.java"
with open(path, "r") as f: c = f.read()
if "org.springframework.data.domain.Pageable" not in c:
    c = c.replace("import org.springframework.web.bind.annotation.*;", "import org.springframework.web.bind.annotation.*;\nimport org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\nimport za.co.taloms.common.pagination.PageRequestUtils;")
with open(path, "w") as f: f.write(c)
