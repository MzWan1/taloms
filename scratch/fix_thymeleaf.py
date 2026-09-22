import glob

for path in glob.glob("src/main/resources/templates/**/*.html", recursive=True):
    with open(path, "r") as f:
        c = f.read()
    if r"\')}" in c:
        c = c.replace(r"\')}", "')}")
        with open(path, "w") as f:
            f.write(c)
        print("Fixed", path)
