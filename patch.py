import re

with open('app/src/main/java/com/remmi/browser/ui/components/UrlBar.kt', 'r') as f:
    content = f.read()

# Add BackHandler import if not exists
if 'import androidx.activity.compose.BackHandler' not in content:
    content = content.replace('import androidx.compose.runtime.*', 'import androidx.compose.runtime.*\nimport androidx.activity.compose.BackHandler')

# Add BackHandler inside TerminalUrlBar
terminal_body_start = content.find('fun TerminalUrlBar(')
if terminal_body_start == -1:
    print("Error finding TerminalUrlBar")
    exit(1)

# Find the first LaunchedEffect to insert BackHandler before it
launched_effect_pos = content.find('LaunchedEffect', terminal_body_start)

back_handler_code = """
  BackHandler(enabled = isEditing) {
    isEditing = false
    editText = url
  }
"""

content = content[:launched_effect_pos] + back_handler_code + content[launched_effect_pos:]

# Update the Clear button logic
old_clear = """              if (editText.isNotEmpty()) {
                IconButton(
                  onClick = { editText = "" },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear text",
                    tint = ThemeCyber.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }"""

new_clear = """              IconButton(
                onClick = { 
                  if (editText.isNotEmpty()) {
                    editText = ""
                  } else {
                    isEditing = false
                    editText = url
                  }
                },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  Icons.Default.Clear,
                  contentDescription = "Clear or cancel",
                  tint = ThemeCyber.colors.textSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }"""

if old_clear in content:
    content = content.replace(old_clear, new_clear)
else:
    print("Warning: old clear button not found exactly")

with open('app/src/main/java/com/remmi/browser/ui/components/UrlBar.kt', 'w') as f:
    f.write(content)

print("Success")
