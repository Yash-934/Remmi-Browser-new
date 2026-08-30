import re
path = "app/src/main/java/com/remmi/browser/MainActivity.kt"
with open(path, "r") as f:
    content = f.read()

content = content.replace("ScreenRoute.EMERGENCY_RECOVERY,\n  VAULT_RECOVERY", "ScreenRoute.EMERGENCY_RECOVERY")
with open(path, "w") as f:
    f.write(content)
