import re

with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'r') as f:
    content = f.read()

# Remove BOTTOM ACTION BAR
bottom_bar_start = content.find('// 4. BOTTOM ACTION BAR')
if bottom_bar_start == -1:
    print("Error finding BOTTOM ACTION BAR")
    exit(1)
    
# Find the end of the Row that contains the bottom action bar.
# It ends right before } // end ModalBottomSheet
bottom_bar_end = content.find('  } // End of Column', bottom_bar_start)
if bottom_bar_end == -1:
    # Let's find just the end of the bottom action bar
    pass

