import os
import xml.etree.ElementTree as ET

strings_data = {
    "values": {
        "pharmacy_order_title": "Pharmacy Refill Order",
        "pharmacy_order_desc": "Generate a medicine order list for WhatsApp or your chemist",
        "pharmacy_order_scope_low_stock": "Low Stock Only",
        "pharmacy_order_scope_all": "All Active Medicines",
        "pharmacy_order_days_format": "%1$d Days",
        "pharmacy_order_share": "Share via WhatsApp / Apps",
        "pharmacy_order_copy": "Copy Text",
        "pharmacy_order_copied": "Refill order copied to clipboard",
        "pharmacy_order_empty": "No medications available to order",
        "pharmacy_order_current_stock": "Current stock: %1$d %2$s left",
        "pharmacy_order_qty_needed": "Order Quantity: %1$d %2$s",
        "pharmacy_order_patient_header": "Patient: %1$s",
        "pharmacy_order_date_header": "Date: %1$s",
        "pharmacy_order_supply_header": "Order for %1$d Days Supply:",
        "pharmacy_order_footer_confirm": "Please confirm availability and delivery. Thank you!",
        "pharmacy_order_footer_brand": "Generated via Dosezy",
        "menu_pharmacy_order": "Pharmacy Refill Order",
        "menu_pharmacy_order_desc": "Generate and share medication orders via WhatsApp"
    },
    "values-ar": {
        "pharmacy_order_title": "طلب إعادة تعبئة الصيدلية",
        "pharmacy_order_desc": "إنشاء قائمة طلب الأدوية لواتساب أو الصيدلي",
        "pharmacy_order_scope_low_stock": "المخزون المنخفض فقط",
        "pharmacy_order_scope_all": "جميع الأدوية النشطة",
        "pharmacy_order_days_format": "%1$d يومًا",
        "pharmacy_order_share": "مشاركة عبر واتساب / التطبيقات",
        "pharmacy_order_copy": "نسخ النص",
        "pharmacy_order_copied": "تم نسخ طلب الأدوية إلى الحافظة",
        "pharmacy_order_empty": "لا توجد أدوية متاحة للطلب",
        "pharmacy_order_current_stock": "المخزون الحالي: متبقي %1$d %2$s",
        "pharmacy_order_qty_needed": "الكمية المطلوبة: %1$d %2$s",
        "pharmacy_order_patient_header": "المريض: %1$s",
        "pharmacy_order_date_header": "التاريخ: %1$s",
        "pharmacy_order_supply_header": "طلب لإمداد %1$d يومًا:",
        "pharmacy_order_footer_confirm": "يرجى تأكيد التوفر وإمكانية التوصيل. شكراً لك!",
        "pharmacy_order_footer_brand": "تم الإنشاء عبر دوزيزي",
        "menu_pharmacy_order": "طلب إعادة تعبئة الصيدلية",
        "menu_pharmacy_order_desc": "إنشاء ومشاركة طلبات الأدوية عبر واتساب"
    },
    "values-bn": {
        "pharmacy_order_title": "ফার্মেসি রিফিল অর্ডার",
        "pharmacy_order_desc": "হোয়াটসঅ্যাপ বা ফার্মেসির জন্য ওষুধের অর্ডারের তালিকা তৈরি করুন",
        "pharmacy_order_scope_low_stock": "কেবল স্বল্প মজুত",
        "pharmacy_order_scope_all": "সকল সক্রিয় ওষুধ",
        "pharmacy_order_days_format": "%1$d দিন",
        "pharmacy_order_share": "হোয়াটসঅ্যাপ / অ্যাপে শেয়ার করুন",
        "pharmacy_order_copy": "টেক্সট কপি করুন",
        "pharmacy_order_copied": "রিফিল অর্ডার ক্লিপবোর্ডে কপি করা হয়েছে",
        "pharmacy_order_empty": "অর্ডার করার মতো কোনো ওষুধ নেই",
        "pharmacy_order_current_stock": "বর্তমান মজুত: %1$d %2$s বাকি",
        "pharmacy_order_qty_needed": "অর্ডারের পরিমাণ: %1$d %2$s",
        "pharmacy_order_patient_header": "রোগী: %1$s",
        "pharmacy_order_date_header": "তারিখ: %1$s",
        "pharmacy_order_supply_header": "%1$d দিনের জন্য অর্ডারের তালিকা:",
        "pharmacy_order_footer_confirm": "দয়া করে প্রাপ্যতা এবং ডেলিভারি নিশ্চিত করুন। ধন্যবাদ!",
        "pharmacy_order_footer_brand": "Dosezy দ্বারা তৈরি",
        "menu_pharmacy_order": "ফার্মেসি রিফিল অর্ডার",
        "menu_pharmacy_order_desc": "হোয়াটসঅ্যাপের মাধ্যমে ওষুধের অর্ডার তৈরি ও শেয়ার করুন"
    },
    "values-de": {
        "pharmacy_order_title": "Apotheken-Nachbestellung",
        "pharmacy_order_desc": "Medikamenten-Bestellliste für WhatsApp oder Ihre Apotheke erstellen",
        "pharmacy_order_scope_low_stock": "Nur geringer Bestand",
        "pharmacy_order_scope_all": "Alle aktiven Medikamente",
        "pharmacy_order_days_format": "%1$d Tage",
        "pharmacy_order_share": "Über WhatsApp / Apps teilen",
        "pharmacy_order_copy": "Text kopieren",
        "pharmacy_order_copied": "Nachbestellung in die Zwischenablage kopiert",
        "pharmacy_order_empty": "Keine Medikamente zur Bestellung verfügbar",
        "pharmacy_order_current_stock": "Aktueller Bestand: noch %1$d %2$s",
        "pharmacy_order_qty_needed": "Bestellmenge: %1$d %2$s",
        "pharmacy_order_patient_header": "Patient: %1$s",
        "pharmacy_order_date_header": "Datum: %1$s",
        "pharmacy_order_supply_header": "Bestellung für einen Bedarf von %1$d Tagen:",
        "pharmacy_order_footer_confirm": "Bitte Verfügbarkeit und Lieferung bestätigen. Vielen Dank!",
        "pharmacy_order_footer_brand": "Erstellt über Dosezy",
        "menu_pharmacy_order": "Apotheken-Nachbestellung",
        "menu_pharmacy_order_desc": "Medikamentenbestellungen erstellen und über WhatsApp teilen"
    },
    "values-es": {
        "pharmacy_order_title": "Pedido de farmacia",
        "pharmacy_order_desc": "Generar lista de medicamentos para WhatsApp o su farmacia",
        "pharmacy_order_scope_low_stock": "Solo poco stock",
        "pharmacy_order_scope_all": "Todos los medicamentos activos",
        "pharmacy_order_days_format": "%1$d días",
        "pharmacy_order_share": "Compartir por WhatsApp / Apps",
        "pharmacy_order_copy": "Copiar texto",
        "pharmacy_order_copied": "Pedido copiado al portapapeles",
        "pharmacy_order_empty": "No hay medicamentos disponibles para pedir",
        "pharmacy_order_current_stock": "Stock actual: quedan %1$d %2$s",
        "pharmacy_order_qty_needed": "Cantidad a pedir: %1$d %2$s",
        "pharmacy_order_patient_header": "Paciente: %1$s",
        "pharmacy_order_date_header": "Fecha: %1$s",
        "pharmacy_order_supply_header": "Pedido para suministro de %1$d días:",
        "pharmacy_order_footer_confirm": "Por favor confirmar disponibilidad y entrega. ¡Gracias!",
        "pharmacy_order_footer_brand": "Generado a través de Dosezy",
        "menu_pharmacy_order": "Pedido de farmacia",
        "menu_pharmacy_order_desc": "Generar y compartir pedidos de medicamentos por WhatsApp"
    },
    "values-fr": {
        "pharmacy_order_title": "Commande pharmacie",
        "pharmacy_order_desc": "Générer une liste de médicaments pour WhatsApp ou votre pharmacien",
        "pharmacy_order_scope_low_stock": "Stock faible uniquement",
        "pharmacy_order_scope_all": "Tous les médicaments actifs",
        "pharmacy_order_days_format": "%1$d jours",
        "pharmacy_order_share": "Partager via WhatsApp / Applis",
        "pharmacy_order_copy": "Copier le texte",
        "pharmacy_order_copied": "Commande copiée dans le presse-papiers",
        "pharmacy_order_empty": "Aucun médicament disponible pour commande",
        "pharmacy_order_current_stock": "Stock actuel: reste %1$d %2$s",
        "pharmacy_order_qty_needed": "Quantité à commander: %1$d %2$s",
        "pharmacy_order_patient_header": "Patient: %1$s",
        "pharmacy_order_date_header": "Date: %1$s",
        "pharmacy_order_supply_header": "Commande pour une réserve de %1$d jours:",
        "pharmacy_order_footer_confirm": "Merci de confirmer la disponibilité et la livraison. Merci !",
        "pharmacy_order_footer_brand": "Généré via Dosezy",
        "menu_pharmacy_order": "Commande pharmacie",
        "menu_pharmacy_order_desc": "Créer et partager des commandes de médicaments par WhatsApp"
    },
    "values-hi": {
        "pharmacy_order_title": "फार्मेसी रीफ़िल ऑर्डर",
        "pharmacy_order_desc": "व्हाट्सएप या मेडिकल स्टोर के लिए दवाओं के ऑर्डर की सूची बनाएं",
        "pharmacy_order_scope_low_stock": "केवल कम स्टॉक वाली दवाएं",
        "pharmacy_order_scope_all": "सभी सक्रिय दवाएं",
        "pharmacy_order_days_format": "%1$d दिन",
        "pharmacy_order_share": "व्हाट्सएप / ऐप्स पर शेयर करें",
        "pharmacy_order_copy": "टेक्स्ट कॉपी करें",
        "pharmacy_order_copied": "रीफ़िल ऑर्डर क्लिपबोर्ड पर कॉपी हो गया",
        "pharmacy_order_empty": "ऑर्डर करने के लिए कोई दवा उपलब्ध नहीं है",
        "pharmacy_order_current_stock": "वर्तमान स्टॉक: %1$d %2$s शेष",
        "pharmacy_order_qty_needed": "ऑर्डर की मात्रा: %1$d %2$s",
        "pharmacy_order_patient_header": "रोगी: %1$s",
        "pharmacy_order_date_header": "दिनांक: %1$s",
        "pharmacy_order_supply_header": "%1$d दिनों की आपूर्ति हेतु ऑर्डर सूची:",
        "pharmacy_order_footer_confirm": "कृपया दवाओं की उपलब्धता और डिलीवरी की पुष्टि करें। धन्यवाद!",
        "pharmacy_order_footer_brand": "Dosezy द्वारा तैयार",
        "menu_pharmacy_order": "फार्मेसी रीफ़िल ऑर्डर",
        "menu_pharmacy_order_desc": "व्हाट्सएप के ज़रिये दवा ऑर्डर बनाएं और शेयर करें"
    },
    "values-it": {
        "pharmacy_order_title": "Ordine farmacia",
        "pharmacy_order_desc": "Genera una lista di farmaci per WhatsApp o la tua farmacia",
        "pharmacy_order_scope_low_stock": "Solo scorte in esaurimento",
        "pharmacy_order_scope_all": "Tutti i farmaci attivi",
        "pharmacy_order_days_format": "%1$d giorni",
        "pharmacy_order_share": "Condividi su WhatsApp / App",
        "pharmacy_order_copy": "Copia testo",
        "pharmacy_order_copied": "Ordine copiato negli appunti",
        "pharmacy_order_empty": "Nessun farmaco disponibile da ordinare",
        "pharmacy_order_current_stock": "Scorte attuali: rimasti %1$d %2$s",
        "pharmacy_order_qty_needed": "Quantità da ordinare: %1$d %2$s",
        "pharmacy_order_patient_header": "Paziente: %1$s",
        "pharmacy_order_date_header": "Data: %1$s",
        "pharmacy_order_supply_header": "Ordine per una fornitura di %1$d giorni:",
        "pharmacy_order_footer_confirm": "Si prega di confermare disponibilità e consegna. Grazie!",
        "pharmacy_order_footer_brand": "Generato tramite Dosezy",
        "menu_pharmacy_order": "Ordine farmacia",
        "menu_pharmacy_order_desc": "Genera e condividi ordini di farmaci su WhatsApp"
    },
    "values-ja": {
        "pharmacy_order_title": "薬局への注文リスト",
        "pharmacy_order_desc": "WhatsAppや薬局向けの医薬品注文リストを作成",
        "pharmacy_order_scope_low_stock": "残量少のみ",
        "pharmacy_order_scope_all": "すべての服用中の薬",
        "pharmacy_order_days_format": "%1$d 日分",
        "pharmacy_order_share": "WhatsApp / アプリで共有",
        "pharmacy_order_copy": "テキストをコピー",
        "pharmacy_order_copied": "注文リストをクリップボードにコピーしました",
        "pharmacy_order_empty": "注文可能な医薬品がありません",
        "pharmacy_order_current_stock": "現在の残量: 残り %1$d %2$s",
        "pharmacy_order_qty_needed": "注文数量: %1$d %2$s",
        "pharmacy_order_patient_header": "患者名: %1$s",
        "pharmacy_order_date_header": "日付: %1$s",
        "pharmacy_order_supply_header": "%1$d 日分の処方注文:",
        "pharmacy_order_footer_confirm": "在庫の確認および配送の確認をお願い申し上げます。ありがとうございます。",
        "pharmacy_order_footer_brand": "Dosezy で生成",
        "menu_pharmacy_order": "薬局への注文リスト",
        "menu_pharmacy_order_desc": "医薬品の注文リストを作成してWhatsAppで共有"
    },
    "values-pt": {
        "pharmacy_order_title": "Pedido para farmácia",
        "pharmacy_order_desc": "Gerar lista de medicamentos para WhatsApp ou sua farmácia",
        "pharmacy_order_scope_low_stock": "Apenas estoque baixo",
        "pharmacy_order_scope_all": "Todos os medicamentos ativos",
        "pharmacy_order_days_format": "%1$d dias",
        "pharmacy_order_share": "Compartilhar via WhatsApp / Apps",
        "pharmacy_order_copy": "Copiar texto",
        "pharmacy_order_copied": "Pedido copiado para a área de transferência",
        "pharmacy_order_empty": "Nenhum medicamento disponível para pedir",
        "pharmacy_order_current_stock": "Estoque atual: restam %1$d %2$s",
        "pharmacy_order_qty_needed": "Quantidade a pedir: %1$d %2$s",
        "pharmacy_order_patient_header": "Paciente: %1$s",
        "pharmacy_order_date_header": "Data: %1$s",
        "pharmacy_order_supply_header": "Pedido para suprimento de %1$d dias:",
        "pharmacy_order_footer_confirm": "Por favor, confirme a disponibilidade e a entrega. Obrigado!",
        "pharmacy_order_footer_brand": "Gerado via Dosezy",
        "menu_pharmacy_order": "Pedido para farmácia",
        "menu_pharmacy_order_desc": "Gerar e compartilhar pedidos de medicamentos pelo WhatsApp"
    },
    "values-ru": {
        "pharmacy_order_title": "Заказ в аптеку",
        "pharmacy_order_desc": "Создать список лекарств для WhatsApp или аптеки",
        "pharmacy_order_scope_low_stock": "Только заканчивающиеся",
        "pharmacy_order_scope_all": "Все активные лекарства",
        "pharmacy_order_days_format": "%1$d дн.",
        "pharmacy_order_share": "Поделиться в WhatsApp / приложениях",
        "pharmacy_order_copy": "Скопировать текст",
        "pharmacy_order_copied": "Заказ скопирован в буфер обмена",
        "pharmacy_order_empty": "Нет доступных лекарств для заказа",
        "pharmacy_order_current_stock": "Текущий остаток: осталось %1$d %2$s",
        "pharmacy_order_qty_needed": "Количество для заказа: %1$d %2$s",
        "pharmacy_order_patient_header": "Пациент: %1$s",
        "pharmacy_order_date_header": "Дата: %1$s",
        "pharmacy_order_supply_header": "Заказ на %1$d дн. запаса:",
        "pharmacy_order_footer_confirm": "Пожалуйста, подтвердите наличие и доставку. Спасибо!",
        "pharmacy_order_footer_brand": "Сформировано в Dosezy",
        "menu_pharmacy_order": "Заказ в аптеку",
        "menu_pharmacy_order_desc": "Создать и отправить заказ лекарств через WhatsApp"
    },
    "values-zh": {
        "pharmacy_order_title": "药店补药清单",
        "pharmacy_order_desc": "生成适用于微信、WhatsApp 或药房的药品补购清单",
        "pharmacy_order_scope_low_stock": "仅余量不足药品",
        "pharmacy_order_scope_all": "全部在服药品",
        "pharmacy_order_days_format": "%1$d 天",
        "pharmacy_order_share": "通过微信/WhatsApp/应用分享",
        "pharmacy_order_copy": "复制文本",
        "pharmacy_order_copied": "补药清单已复制到剪贴板",
        "pharmacy_order_empty": "暂无需要订购的药品",
        "pharmacy_order_current_stock": "当前余量：剩余 %1$d %2$s",
        "pharmacy_order_qty_needed": "建议补订数量：%1$d %2$s",
        "pharmacy_order_patient_header": "患者：%1$s",
        "pharmacy_order_date_header": "日期：%1$s",
        "pharmacy_order_supply_header": "%1$d 天用量补购订单：",
        "pharmacy_order_footer_confirm": "请确认药品库存及配送安排，十分感谢！",
        "pharmacy_order_footer_brand": "由 Dosezy 生成",
        "menu_pharmacy_order": "药店补药清单",
        "menu_pharmacy_order_desc": "生成药品订单并通过微信或WhatsApp分享"
    }
}

base_res = r"c:\Users\UwU\Desktop\app-projects\dosezy\patient\android\app\src\main\res"

for folder, key_dict in strings_data.items():
    file_path = os.path.join(base_res, folder, "strings.xml")
    if not os.path.exists(file_path):
        print(f"Warning: {file_path} not found")
        continue

    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    new_strings = []
    for k, v in key_dict.items():
        if f'name="{k}"' not in content:
            # Escape single quotes in android xml
            escaped_v = v.replace("'", "\\'")
            new_strings.append(f'    <string name="{k}">{escaped_v}</string>')

    if new_strings:
        insert_idx = content.rfind("</resources>")
        if insert_idx != -1:
            updated_content = content[:insert_idx] + "\n".join(new_strings) + "\n" + content[insert_idx:]
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(updated_content)
            print(f"Added {len(new_strings)} strings to {folder}/strings.xml")
        else:
            print(f"Error: </resources> not found in {file_path}")
    else:
        print(f"All strings already present in {folder}/strings.xml")

print("Finished injecting pharmacy order strings across all 12 locales.")
