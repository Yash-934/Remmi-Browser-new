import os
import re

replacements = [
    (r'com\.netrunner\.', 'com.remmi.'),
    (r'com/netrunner/', 'com/remmi/'),
    (r'com_netrunner_', 'com_remmi_'),
    (r'NetRunnerDatabase', 'RemmiDatabase'),
    (r'netrunner_vault\.db', 'remmi_vault.db'),
    (r'netrunner_database\.wal', 'remmi_database.wal'),
    (r'netrunner_database\.shm', 'remmi_database.shm'),
    (r'netrunner_db_master_key', 'remmi_db_master_key'),
    (r'netrunner_vault_backup', 'remmi_vault_backup'),
    (r'NetRunner_Vault_Backup_HMAC_Integrity', 'Remmi_Vault_Backup_HMAC_Integrity'),
    (r'secops@netrunner\.local', 'secops@remmi.local'),
    (r't=netrunner', 't=remmi'),
    (r'netrunner-release-apks', 'remmi-release-apks'),
    (r'adblock-rust-[0-9\.]+-netrunner', 'adblock-rust-0.8.0-remmi'),
    (r'Netrunner', 'Remmi'),
    (r'NetRunner', 'Remmi'),
    (r'NETRUNNER', 'REMMI'),
    (r'netrunner', 'remmi'),
    (r'net_runner', 'remmi'),
    (r'net-runner', 'remmi-browser'),
]

url_replacement = (r'https://raw\.githubusercontent\.com/netrunner/filters/main/unbreak\.txt', 'https://easylist.to/easylist/easylist.txt')

def process_file(filepath):
    if not os.path.isfile(filepath):
        return
    if filepath.endswith('.py') or filepath.endswith('.so') or '.git' in filepath or '/build/' in filepath or '.gradle' in filepath:
        return
        
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except:
        return

    original = content
    content = re.sub(url_replacement[0], url_replacement[1], content)
    for pattern, repl in replacements:
        content = re.sub(pattern, repl, content)
        
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, dirs, files in os.walk('.'):
    for name in files:
        process_file(os.path.join(root, name))

