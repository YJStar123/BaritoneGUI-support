import subprocess, glob, os

JARS = sorted(glob.glob(r"E:/编程/baritoneGUI支持-Fabric/baritone-jars/*.jar"))
CLASSES = [
    "baritone.api.BaritoneAPI",
    "baritone.api.IBaritoneProvider",
    "baritone.api.IBaritone",
    "baritone.api.command.IBaritoneChatControl",
]
TARGETS = ["baritone-1.20-1.20.1.jar", "baritone-1.21-1.21.1.jar",
           "baritone-1.21.5.jar", "baritone-26.2.jar"]

def javap(jar, cls):
    try:
        out = subprocess.run(["javap", "-classpath", jar, "-p", cls],
                             capture_output=True, text=True, timeout=60)
        return out.stdout + out.stderr
    except Exception as e:
        return f"ERR {e}"

for t in TARGETS:
    jar = os.path.join(r"E:/编程/baritoneGUI支持-Fabric/baritone-jars", t)
    if not os.path.exists(jar):
        print("MISSING", t); continue
    print("="*70)
    print("JAR:", t)
    for c in CLASSES:
        print("-"*60)
        print("CLASS:", c)
        print(javap(jar, c))
