import sys
with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "r") as f:
    content = f.read()

start_idx = content.find("fun test35_WipeVaultFalse_PreservesDbAndKeys")
if start_idx != -1:
    end_idx = content.find("fun test36_", start_idx)
    if end_idx != -1:
        start_idx = content.rfind("@Test", 0, start_idx)
        content = content[:start_idx] + "  @Test\n  " + content[end_idx:]

with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "w") as f:
    f.write(content)
