path = "src/main/java/za/co/taloms/dashboard/presentation/DashboardController.java"
with open(path, "r") as f: c = f.read()

c = c.replace("import za.co.taloms.traditionalauthority.application.service.AuthorityScopeService;", "import za.co.taloms.security.application.service.AuthorityScopeService;")

with open(path, "w") as f: f.write(c)
