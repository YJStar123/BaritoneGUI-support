import subprocess, os

BASE = r"E:/编程/baritoneGUI支持-Fabric/baritone-jars"
TARGETS = ["baritone-1.20-1.20.1.jar", "baritone-1.21-1.21.1.jar",
           "baritone-1.21.5.jar", "baritone-26.2.jar"]
CLASSES = [
    "baritone.api.command.manager.ICommandManager",
    "baritone.api.process.ICustomGoalProcess",
    "baritone.api.process.IMineProcess",
    "baritone.api.process.IFollowProcess",
    "baritone.api.process.IBuilderProcess",
    "baritone.api.process.IGetToBlockProcess",
]

def javap(jar, cls):
    try:
        out = subprocess.run(["javap", "-classpath", jar, "-p", cls],
                             capture_output=True, text=True, timeout=60)
        return out.stdout + out.stderr
    except Exception as e:
        return f"ERR {e}"

for t in TARGETS:
    jar = os.path.join(BASE, t)
    print("="*70); print("JAR:", t)
    for c in CLASSES:
        print("-"*50); print("CLASS:", c)
        txt = javap(jar, c)
        # only show public method lines
        for line in txt.splitlines():
            if "public abstract" in line or "public " in line and ("execute" in line or "goal" in line or "mine" in line or "follow" in line or "build" in line or "getToBlock" in line or "class " in line or "interface " in line):
                print(line.strip())
