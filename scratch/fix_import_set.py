path = "src/main/java/za/co/taloms/pto/application/service/PTOService.java"
with open(path, "r") as f: c = f.read()
if "import java.util.Set;" not in c:
    c = c.replace("import java.util.List;", "import java.util.List;\nimport java.util.Set;")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/pto/application/service/PTOServiceImpl.java"
with open(path, "r") as f: c = f.read()
if "import java.util.Set;" not in c:
    c = c.replace("import java.util.List;", "import java.util.List;\nimport java.util.Set;")
with open(path, "w") as f: f.write(c)

