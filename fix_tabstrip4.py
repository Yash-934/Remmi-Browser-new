import re

with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'r') as f:
    content = f.read()

trailing_block = """        // Quick Tor Tab
        OutlinedButton(
          onClick = {
            onNewTab(PrivacyProfile.GHOST, null)
            onDismiss()
          },
          border = BorderStroke(1.dp, ThemeCyber.colors.torPurple.copy(alpha = 0.6f)),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = ThemeCyber.colors.torPurple.copy(alpha = 0.08f)),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = "New Tor Tab",
            tint = ThemeCyber.colors.torPurple,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Tor",
            fontSize = 12.sp,
            color = ThemeCyber.colors.torPurple,
            fontWeight = FontWeight.Bold
          )
        }
      }
      // Close All Tabs Action
      TextButton(
        onClick = {
          onCloseAllTabs()
          onDismiss()
        },
        colors = ButtonDefaults.textButtonColors(contentColor = ThemeCyber.colors.dangerRed)
      ) {
        Text(
          text = "Close All",
          fontSize = 12.5.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }

  // --- DIALOGS ---"""

if trailing_block in content:
    content = content.replace(trailing_block, "  }\n\n  // --- DIALOGS ---")
else:
    print("Not found! Trying regex.")
    trailing_pattern = r"        // Quick Tor Tab\n        OutlinedButton\([\s\S]*?    \}\n  \}\n\n  // --- DIALOGS ---"
    match = re.search(trailing_pattern, content)
    if match:
        content = content.replace(match.group(0), "  }\n\n  // --- DIALOGS ---")
    else:
        print("Regex not found.")

with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'w') as f:
    f.write(content)

print("fixed bottom")
