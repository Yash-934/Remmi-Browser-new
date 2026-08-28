import re

with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'r') as f:
    content = f.read()

# 1. Extract the bottom action bar
bottom_bar_pattern = r"    // 4\. BOTTOM ACTION BAR \(Incognito, Tor, Close All\)\n.*?    }\n"
bottom_bar_match = re.search(bottom_bar_pattern, content, re.DOTALL)
if not bottom_bar_match:
    print("Could not find bottom action bar")
    exit(1)

bottom_bar_code = bottom_bar_match.group(0)

# Remove the bottom action bar from its original place
content = content.replace(bottom_bar_code, "")

# 2. Insert it just before // 3. MAIN SCROLLABLE CONTENT
insert_target = "    // 3. MAIN SCROLLABLE CONTENT (SPACES + CHIPS + TABS)"
if insert_target not in content:
    print("Could not find insertion target")
    exit(1)

new_bar_code = bottom_bar_code.replace("// 4. BOTTOM ACTION BAR (Incognito, Tor, Close All)", "// Quick Actions Bar (Incognito, Tor, Close All)")
content = content.replace(insert_target, new_bar_code + "\n" + insert_target)

# 3. Add Privacy Profile label to ModernTabCard
# Find where the tab title is rendered.
title_pattern = r"""          Text\(\n            text = tab\.title\.ifEmpty \{ cleanDomain \},\n            color = ThemeCyber\.colors\.textPrimary,\n            fontSize = 11\.5\.sp,\n            fontWeight = FontWeight\.SemiBold,\n            maxLines = 1,\n            overflow = TextOverflow\.Ellipsis\n          \)"""
title_match = re.search(title_pattern, content)
if not title_match:
    print("Could not find tab title in ModernTabCard")
    exit(1)

new_title_code = """          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = tab.title.ifEmpty { cleanDomain },
              color = ThemeCyber.colors.textPrimary,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            val profileText = when (tab.profile) {
              PrivacyProfile.GHOST -> "Tor"
              PrivacyProfile.INCOGNITO -> "Incognito"
              else -> "Shield"
            }
            Text(
              text = profileText,
              color = profileColor,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = CyberMonoFamily
            )
          }"""
content = content.replace(title_match.group(0), new_title_code)

# Let's fix the weight modifier issue. The Row that contained the Status Dot and Tab Title was:
# Row(
#   verticalAlignment = Alignment.CenterVertically,
#   modifier = Modifier.weight(1f)
# ) {
#   Box(status dot...)
#   Spacer(...)
#   Text(...) <-- we replaced this with Column.
# }
# Since we put weight(1f) on Column, it will expand correctly.

# 4. Remove wireframe lines and replace with a cleaner look
wireframe_pattern = r"""            // Stylized Website Visual Lines\n            Column\(\n              verticalArrangement = Arrangement\.spacedBy\(3\.dp\),\n              modifier = Modifier\.fillMaxWidth\(\)\n            \) \{\n              Box\(\n                modifier = Modifier\n                  \.fillMaxWidth\(0\.9f\)\n                  \.height\(4\.dp\)\n                  \.clip\(RoundedCornerShape\(2\.dp\)\)\n                  \.background\(ThemeCyber\.colors\.surfaceBorder\)\n              \)\n              Box\(\n                modifier = Modifier\n                  \.fillMaxWidth\(0\.65f\)\n                  \.height\(4\.dp\)\n                  \.clip\(RoundedCornerShape\(2\.dp\)\)\n                  \.background\(ThemeCyber\.colors\.surfaceBorder\.copy\(alpha = 0\.6f\)\)\n              \)\n            \}"""

wireframe_match = re.search(wireframe_pattern, content, re.DOTALL)
if wireframe_match:
    # Instead of wireframe, let's put an AsyncImage for the favicon, very large.
    # We already have `faviconUrl` in ModernTabCard!
    # Wait, do we need AsyncImage?
    # We can just use an icon or something. Let's use Coil.
    replacement = """            Box(
              modifier = Modifier.fillMaxWidth().weight(1f),
              contentAlignment = Alignment.Center
            ) {
              coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                  .data(faviconUrl)
                  .crossfade(true)
                  .build(),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
              )
            }"""
    content = content.replace(wireframe_match.group(0), replacement)
else:
    print("Could not find wireframe")
    
with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'w') as f:
    f.write(content)

print("Success")

