path = "src/main/java/za/co/taloms/security/presentation/UserPageController.java"
with open(path, "r") as f: c = f.read()
c = c.replace("u.isEnabled()", "(u.getEnabled() != null && u.getEnabled())")
c = c.replace("!u.isEnabled()", "(u.getEnabled() == null || !u.getEnabled())")
c = c.replace("u.isAccountLocked()", "(u.getAccountLocked() != null && u.getAccountLocked())")
c = c.replace("!u.isAccountLocked()", "(u.getAccountLocked() == null || !u.getAccountLocked())")
with open(path, "w") as f: f.write(c)

path = "src/main/java/za/co/taloms/traditionalauthority/presentation/TraditionalAuthorityPageController.java"
with open(path, "r") as f: c = f.read()
c = c.replace("a.getName()", "a.getAuthorityName()")
c = c.replace("a.isActive()", "(a.getActive() != null && a.getActive())")
c = c.replace("!a.isActive()", "(a.getActive() == null || !a.getActive())")
with open(path, "w") as f: f.write(c)
