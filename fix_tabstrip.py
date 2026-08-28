import re

with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'r') as f:
    content = f.read()

bad_block = """          Text(
            text = "Incognito",
            fontSize = 12.sp,
            color = ThemeCyber.colors.textPrimary,
            fontWeight = FontWeight.Medium
          )
        }
    // 3. MAIN SCROLLABLE CONTENT (SPACES + CHIPS + TABS)"""

good_block = """          Text(
            text = "Incognito",
            fontSize = 12.sp,
            color = ThemeCyber.colors.textPrimary,
            fontWeight = FontWeight.Medium
          )
        }

        // Quick Tor Tab
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
    
    Spacer(modifier = Modifier.height(12.dp))

    // 3. MAIN SCROLLABLE CONTENT (SPACES + CHIPS + TABS)"""

if bad_block in content:
    content = content.replace(bad_block, good_block)
else:
    print("bad_block not found")

# And we also need to clean up the trailing Tor/Close all buttons at the bottom that are now syntactically invalid or causing issues.
# Wait, let's see what is at the bottom right now.
