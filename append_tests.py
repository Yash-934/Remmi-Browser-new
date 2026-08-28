with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "r") as f:
    content = f.read()

idx = content.rfind("}")
if idx != -1:
    with open("deterministic_tests.kt", "r") as nf:
        new_tests_content = nf.read()
    
    content = content[:idx] + new_tests_content + "\n}"
    
    with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "w") as f:
        f.write(content)
