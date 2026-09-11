# If anyone is wondering what this is,
# it helps me insert/add new translation keys much faster
#
# "tiu" stands for "Translation Keys Insertion Util" btw
#
# The only annoying part is that i have to ask AI to generate the translated
# keys in a very specific layout:

# "<lang>_<LANG>": {                                                                 # use this as an example prompt for the ai if needed
#     "random.translation.key": "<prefix> <color:#FFF200>text text text</color>",
#     "translated.key": "<prefix> <color:#FFF200>Hello World :)</color>"
# },

# etc.
#
# So, if you fork EssentialsC and your changes include new translation keys,
# consider using this to speed up the process

import json
from pathlib import Path

INSERT_AFTER = '  "command.usage.banip"'# insert message *behind

# New translation keys
NEW_TRANSLATIONS = {
    "en_us": {
        "unbanip.success": "<prefix> <color:#FFF200>Unbanned IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> is not banned.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>was unbanned by</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Usage: /unban-ip <ip></color>"
    },
    "zh_cn": {
        "unbanip.success": "<prefix> <color:#FFF200>已解封 IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> 未被封禁。</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>已被</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>解封</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>用法: /unban-ip <ip></color>"
    },
    "vi_vn": {
        "unbanip.success": "<prefix> <color:#FFF200>Đã bỏ cấm IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> không bị cấm.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>đã được bỏ cấm bởi</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Cách dùng: /unban-ip <ip></color>"
    },
    "ur_pk": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> پر سے پابندی ہٹا دی گئی</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> پر پابندی نہیں ہے۔</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>سے پابندی</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>نے ہٹائی</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>استعمال: /unban-ip <ip></color>"
    },
    "uk_ua": {
        "unbanip.success": "<prefix> <color:#FFF200>Розблоковано IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> не заблоковано.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>було розблоковано гравцем</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Використання: /unban-ip <ip></color>"
    },
    "tr_tr": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> yasağı kaldırıldı</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> yasaklı değil.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>yasağı</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>tarafından kaldırıldı</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Kullanım: /unban-ip <ip></color>"
    },
    "th_th": {
        "unbanip.success": "<prefix> <color:#FFF200>ปลดแบน IP <color:#FFFFFF><ip></color> แล้ว</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> ไม่ได้ถูกแบน</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>ถูกปลดแบนโดย</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>วิธีใช้: /unban-ip <ip></color>"
    },
    "te_in": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> బ్యాన్ తొలగించబడింది</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> బ్యాన్ చేయబడలేదు.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>బ్యాన్‌ను</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>తొలగించారు</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>విధానం: /unban-ip <ip></color>"
    },
    "ta_in": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> மீதான தடை நீக்கப்பட்டது</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> தடை செய்யப்படவில்லை.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>தடையை</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>நீக்கினார்</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>பயன்பாடு: /unban-ip <ip></color>"
    },
    "sv_se": {
        "unbanip.success": "<prefix> <color:#FFF200>Avbanlyst IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> är inte bannlyst.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>avbanlystes av</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Användning: /unban-ip <ip></color>"
    },
    "ru_ru": {
        "unbanip.success": "<prefix> <color:#FFF200>Разбанен IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> не забанен.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>был разбанен игроком</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Использование: /unban-ip <ip></color>"
    },
    "pt_br": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> desbanido</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> não está banido.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>foi desbanido por</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Uso: /unban-ip <ip></color>"
    },
    "pl_pl": {
        "unbanip.success": "<prefix> <color:#FFF200>Odbanowano IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> nie jest zbanowane.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>zostało odbanowane przez</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Użycie: /unban-ip <ip></color>"
    },
    "nl_nl": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> ontbanned</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> is niet gebanned.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>werd ontbanned door</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Gebruik: /unban-ip <ip></color>"
    },
    "mr_in": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> वरून बंदी उठवली</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> बॅन नाही.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>ची बंदी</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>ने उठवली</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>वापर: /unban-ip <ip></color>"
    },
    "ko_kr": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> 차단이 해제되었습니다</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color>은(는) 차단되지 않았습니다.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>의 차단이</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>에 의해 해제되었습니다</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>사용법: /unban-ip <ip></color>"
    },
    "ja_jp": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> のバンを解除しました</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> はバンされていません。</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>が</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>によってバン解除されました</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>使用法: /unban-ip <ip></color>"
    },
    "it_it": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> sbannato</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>L'IP <color:#FFFFFF><ip></color> non è bannato.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>è stato sbannato da</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Uso: /unban-ip <ip></color>"
    },
    "id_id": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> telah dibuka blokirnya</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> tidak diblokir.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>telah dibuka blokirnya oleh</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Penggunaan: /unban-ip <ip></color>"
    },
    "hi_in": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> से प्रतिबंध हटा दिया गय</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> प्रतिबंधित नहीं ह</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>से प्रतिबंध</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>द्वारा हटा दिय गय</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>उपयोग: /unban-ip <ip></color>"
    },
    "gu_in": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> પરથી પ્રતિબંધ દૂર કરવામાં આવ્યો</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> પ્રતિબંધિત નથી.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>પરથી પ્રતિબંધ</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>દ્વારા દૂર કરવામાં આવ્યો</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>ઉપયોગ: /unban-ip <ip></color>"
    },
    "fr_fr": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> débannie</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>L'IP <color:#FFFFFF><ip></color> n'est pas bannie.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>a été débannie par</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Utilisation: /unban-ip <ip></color>"
    },
    "fil_ph": {
        "unbanip.success": "<prefix> <color:#FFF200>In-unban ang IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>Hindi ban ang IP <color:#FFFFFF><ip></color>.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>ay in-unban ni</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Paggamit: /unban-ip <ip></color>"
    },
    "es_es": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> desbaneada</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>La IP <color:#FFFFFF><ip></color> no está baneada.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>fue desbaneada por</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Uso: /unban-ip <ip></color>"
    },
    "en_gb": {
        "unbanip.success": "<prefix> <color:#FFF200>Unbanned IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> is not banned.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>was unbanned by</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Usage: /unban-ip <ip></color>"
    },
    "de_de": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> entbannt</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> ist nicht gebannt.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>wurde von</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>entbannt</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Nutzung: /unban-ip <ip></color>"
    },
    "de_ch": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> entbannt</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> ist nicht gebannt.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>wurde von</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>entbannt</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Nutzung: /unban-ip <ip></color>"
    },
    "bn_bd": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color>-এর নিষেধাজ্ঞা তুলে নেওয়া হয়েছে</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> নিষিদ্ধ নয়।</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>-এর নিষেধাজ্ঞা</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>তুলে নিয়েছে</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>ব্যবহার: /unban-ip <ip></color>"
    },
    "ar_sa": {
        "unbanip.success": "<prefix> <color:#FFF200>تم رفع الحظر عن IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> غير محظور.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>تم رفع الحظر عنه بواسطة</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>الاستخدام: /unban-ip <ip></color>"
    },
    "ar_eg": {
        "unbanip.success": "<prefix> <color:#FFF200>تم رفع الحظر عن IP <color:#FFFFFF><ip></color></color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> غير محظور.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>تم رفع الحظر عنه بواسطة</color> <color:#FFFFFF><unbanner></color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>الاستخدام: /unban-ip <ip></color>"
    },
    "lb_lu": {
        "unbanip.success": "<prefix> <color:#FFF200>IP <color:#FFFFFF><ip></color> fräigeschalt</color>",
        "unbanip.not_banned": "<prefix> <color:#FF0000>IP <color:#FFFFFF><ip></color> ass net gespaart.</color>",
        "unbanip.broadcast": "<prefix> <color:#FFFFFF><ip></color> <color:#FFF200>gouf vum</color> <color:#FFFFFF><unbanner></color> <color:#FFF200>fräigeschalt</color>",
        "command.usage.unbanip": "<prefix> <color:#FF0000>Benotzung: /unban-ip <ip></color>"
    }
}

current_folder = Path(".")

for lang_code, translations in NEW_TRANSLATIONS.items():

    file_path = current_folder / f"{lang_code}.json"

    if not file_path.exists():
        print(f"[SKIPPED] {file_path.name} not found") # skip if the file cannot be found
        continue

    with open(file_path, "r", encoding="utf-8") as file:
        lines = file.readlines()

    insert_index = None

    for i, line in enumerate(lines):
        if INSERT_AFTER in line:
            insert_index = i + 1


            break

    if insert_index is None:
        print(f"[SKIPPED] Key not found in {file_path.name}") #log if INSERT_AFTER is not found

        continue

    existing_content = "".join(lines)

    new_lines = []

    for key, value in translations.items():

        # check if key exists, if yes, replace
        if f'"{key}"' in existing_content:
            print(f"[OVERWRITE] {key} already exists in {file_path.name},  replacing...") #log

            lines = [line for line in lines if f'"{key}"' not in line]
            for j, line in enumerate (lines):

                if INSERT_AFTER in line:
                    insert_index = j + 1
                    break


        escaped_value = value.replace('"', '\"')

        new_lines.append(

            f'  "{key}": "{escaped_value}",\n'
        )

    if not new_lines: # if no lines, changed, log
        print(f"[NO CHANGES] {file_path.name}")
        continue

    lines[insert_index:insert_index] = new_lines

    with open(file_path, "w", encoding="utf-8") as file:

        file.writelines(lines)

    print(f"[UPDATED] {file_path.name}") #log updated files

print("Done.")