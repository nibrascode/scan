# New strings for 1.1.0: key -> (az, en, tr, ru, ar)
S = {
'tools_title': ("Alətlər","Tools","Araçlar","Инструменты","الأدوات"),
'tool_ocr': ("Mətni tanı","Extract text","Metni tanı","Распознать текст","استخراج النص"),
'tool_qr': ("QR / barkod","QR / barcode","QR / barkod","QR / штрихкод","QR / باركود"),
'tool_merge': ("PDF birləşdir","Merge PDFs","PDF birleştir","Объединить PDF","دمج PDF"),
'tool_sign': ("İmza at","Sign","İmzala","Подписать","توقيع"),
'tool_pdf_tools': ("PDF alətləri","PDF tools","PDF araçları","Инструменты PDF","أدوات PDF"),
'session_replace_confirm': ("Yarımçıq skan var. Onu silib davam edilsin?","You have an unfinished scan. Discard it and continue?","Yarım kalmış bir tarama var. Silinip devam edilsin mi?","Есть незавершённое сканирование. Удалить его и продолжить?","لديك مسح غير مكتمل. هل تريد حذفه والمتابعة؟"),
'from_library': ("Kitabxanadan","From library","Kitaplıktan","Из библиотеки","من المكتبة"),
'from_device': ("Cihazdan PDF seç","Pick PDF from device","Cihazdan PDF seç","Выбрать PDF на устройстве","اختيار PDF من الجهاز"),
'merge_choose': ("Birləşdiriləcək sənədləri seçin","Choose documents to merge","Birleştirilecek belgeleri seçin","Выберите документы для объединения","اختر المستندات للدمج"),
'merge_need_two': ("Ən azı 2 sənəd seçin","Select at least 2 documents","En az 2 belge seçin","Выберите не менее 2 документов","اختر مستندين على الأقل"),
'merged_name_prefix': ("Birləşmiş","Merged","Birlestirilmis","Объединённый","مدمج"),
'choose_document': ("Sənəd seçin","Choose a document","Belge seçin","Выберите документ","اختر مستندًا"),
'sort': ("Sırala","Sort","Sırala","Сортировка","ترتيب"),
'sort_newest': ("Ən yeni","Newest first","En yeni","Сначала новые","الأحدث أولاً"),
'sort_oldest': ("Ən köhnə","Oldest first","En eski","Сначала старые","الأقدم أولاً"),
'sort_name_az': ("Ad (A–Z)","Name (A–Z)","Ad (A–Z)","Имя (А–Я)","الاسم (أ–ي)"),
'sort_name_za': ("Ad (Z–A)","Name (Z–A)","Ad (Z–A)","Имя (Я–А)","الاسم (ي–أ)"),
'sort_size': ("Ölçü (böyükdən)","Size (largest)","Boyut (büyükten)","Размер (больше)","الحجم (الأكبر)"),
'folder_all': ("Hamısı","All","Tümü","Все","الكل"),
'new_folder': ("Yeni qovluq","New folder","Yeni klasör","Новая папка","مجلد جديد"),
'delete_folder': ("Qovluğu sil","Delete folder","Klasörü sil","Удалить папку","حذف المجلد"),
'delete_folder_confirm': ("Qovluq silinsin? İçindəki sənədlər əsas siyahıya köçürüləcək.","Delete this folder? Its documents will be moved to the main list.","Klasör silinsin mi? İçindeki belgeler ana listeye taşınacak.","Удалить папку? Документы будут перемещены в общий список.","حذف هذا المجلد؟ سيتم نقل مستنداته إلى القائمة الرئيسية."),
'folder_root': ("Əsas siyahı (qovluqsuz)","Main list (no folder)","Ana liste (klasörsüz)","Общий список (без папки)","القائمة الرئيسية (بلا مجلد)"),
'move_to_folder': ("Qovluğa köçür","Move to folder","Klasöre taşı","Переместить в папку","نقل إلى مجلد"),
'pdf_tools_more': ("Alətlər…","Tools…","Araçlar…","Инструменты…","أدوات…"),
'edit_pages': ("Səhifələri düzəlt (sırala, döndər, sil)","Edit pages (reorder, rotate, delete)","Sayfaları düzenle (sırala, döndür, sil)","Изменить страницы (порядок, поворот, удаление)","تحرير الصفحات (ترتيب، تدوير، حذف)"),
'add_watermark': ("Su nişanı əlavə et","Add watermark","Filigran ekle","Добавить водяной знак","إضافة علامة مائية"),
'watermark_default': ("MƏXFİ","CONFIDENTIAL","GİZLİ","КОНФИДЕНЦИАЛЬНО","سري"),
'protect_pdf': ("Parolla qoru","Protect with password","Parolayla koru","Защитить паролем","حماية بكلمة مرور"),
'password_hint': ("Parol","Password","Parola","Пароль","كلمة المرور"),
'password_note': ("Parolu yadda saxlayın — itsə, faylı açmaq mümkün olmayacaq. Yeni nüsxə yaradılır.","Remember the password — if lost, the file cannot be opened. A new copy is created.","Parolayı unutmayın — kaybolursa dosya açılamaz. Yeni bir kopya oluşturulur.","Запомните пароль — без него файл не открыть. Создаётся новая копия.","احفظ كلمة المرور — إذا فُقدت لا يمكن فتح الملف. سيتم إنشاء نسخة جديدة."),
'password_short': ("Parol ən azı 4 simvol olmalıdır","Password must be at least 4 characters","Parola en az 4 karakter olmalı","Пароль — не менее 4 символов","يجب أن تكون كلمة المرور 4 أحرف على الأقل"),
'compress_pdf': ("PDF-i sıxışdır","Compress PDF","PDF'yi sıkıştır","Сжать PDF","ضغط PDF"),
'export_jpg': ("JPG kimi ixrac et","Export as JPG","JPG olarak dışa aktar","Экспорт в JPG","تصدير كـ JPG"),
'export_txt': ("TXT kimi ixrac et","Export as TXT","TXT olarak dışa aktar","Экспорт в TXT","تصدير كـ TXT"),
'saved_as': ("Yadda saxlanıldı: %1$s","Saved: %1$s","Kaydedildi: %1$s","Сохранено: %1$s","تم الحفظ: %1$s"),
'suffix_signed': ("imzalı","signed","imzali","подписан","موقع"),
'suffix_watermark': ("nişanlı","watermark","filigranli","водяной знак","علامة مائية"),
'suffix_protected': ("qorunan","protected","korumali","защищён","محمي"),
'suffix_compressed': ("sıxılmış","compressed","sikistirilmis","сжатый","مضغوط"),
'pdf_unreadable': ("PDF oxunmadı (zədəli və ya parollu ola bilər)","Could not read the PDF (it may be damaged or password-protected)","PDF okunamadı (bozuk veya parolalı olabilir)","Не удалось прочитать PDF (возможно, повреждён или защищён паролем)","تعذّرت قراءة ملف PDF (قد يكون تالفًا أو محميًا بكلمة مرور)"),
'sign_draw_hint': ("Barmağınızla aşağıda imzanızı çəkin","Draw your signature below with your finger","İmzanızı parmağınızla aşağıya çizin","Нарисуйте подпись пальцем ниже","ارسم توقيعك بإصبعك في الأسفل"),
'undo': ("Geri al","Undo","Geri al","Отменить","تراجع"),
'clear': ("Təmizlə","Clear","Temizle","Очистить","مسح"),
'sign_use_saved': ("Saxlanmış imzadan istifadə et","Use saved signature","Kayıtlı imzayı kullan","Использовать сохранённую подпись","استخدام التوقيع المحفوظ"),
'sign_next': ("Davam et","Continue","Devam et","Далее","متابعة"),
'sign_empty': ("Əvvəlcə imza çəkin","Draw a signature first","Önce imza çizin","Сначала нарисуйте подпись","ارسم التوقيع أولاً"),
'sign_place_hint': ("İmzanı sürüşdürün, iki barmaqla böyüdün/kiçildin","Drag the signature, pinch to resize","İmzayı sürükleyin, iki parmakla boyutlandırın","Перетащите подпись, меняйте размер двумя пальцами","اسحب التوقيع، وكبّره أو صغّره بإصبعين"),
'sign_apply': ("İmzanı yerləşdir və saxla","Place signature and save","İmzayı yerleştir ve kaydet","Поставить подпись и сохранить","وضع التوقيع والحفظ"),
'page_of': ("Səhifə %1$d / %2$d","Page %1$d of %2$d","Sayfa %1$d / %2$d","Страница %1$d из %2$d","الصفحة %1$d من %2$d"),
'qr_result': ("Skan nəticəsi","Scan result","Tarama sonucu","Результат сканирования","نتيجة المسح"),
'open_link': ("Linki aç","Open link","Bağlantıyı aç","Открыть ссылку","فتح الرابط"),
'qr_none': ("Şəkildə QR və ya barkod tapılmadı","No QR code or barcode found in the image","Görselde QR veya barkod bulunamadı","На изображении нет QR-кода или штрихкода","لم يُعثر على رمز QR أو باركود في الصورة"),
'qr_hint': ("Kameranı QR koda və ya barkoda tutun","Point the camera at a QR code or barcode","Kamerayı QR koda veya barkoda tutun","Наведите камеру на QR-код или штрихкод","وجّه الكاميرا نحو رمز QR أو باركود"),
'qr_history': ("Tarixçə","History","Geçmiş","История","السجل"),
'qr_clear_confirm': ("Bütün skan tarixçəsi silinsin?","Clear the whole scan history?","Tüm tarama geçmişi silinsin mi?","Очистить всю историю сканирования?","مسح سجل المسح بالكامل؟"),
'qr_history_empty': ("Hələ skan edilmiş kod yoxdur","No scanned codes yet","Henüz taranmış kod yok","Пока нет отсканированных кодов","لا توجد رموز ممسوحة بعد"),
# updated existing
'search_hint': ("Ad və ya mətn üzrə axtar","Search by name or text","Ad veya metne göre ara","Поиск по имени или тексту","البحث بالاسم أو النص"),
'search_empty': ("Uyğun sənəd tapılmadı","No matching documents","Eşleşen belge yok","Подходящих документов нет","لا توجد مستندات مطابقة"),
'share_app_text': ("Nibras Scan — skan et, PDF hazırla, mətni tanı, imza at, QR oxu. Hamısı oflayn.","Nibras Scan — scan, make PDFs, extract text, sign, read QR codes. All offline.","Nibras Scan — tara, PDF oluştur, metni tanı, imzala, QR oku. Tamamen çevrimdışı.","Nibras Scan — сканируйте, создавайте PDF, распознавайте текст, подписывайте, читайте QR. Всё офлайн.","Nibras Scan — امسح، أنشئ PDF، استخرج النص، وقّع، واقرأ رموز QR. كل ذلك دون اتصال."),
}
LANGS = ['values','values-en','values-tr','values-ru','values-ar']
import re, html, sys
base = sys.argv[1]
def esc(v):
    v = v.replace('&','&amp;').replace('<','&lt;').replace('>','&gt;').replace("'", "\\'").replace('"','\\"')
    return v
for i, d in enumerate(LANGS):
    p = f'{base}/{d}/strings.xml'
    s = open(p, encoding='utf-8').read()
    for k, vals in S.items():
        line = f'    <string name="{k}">{esc(vals[i])}</string>'
        pat = re.compile(rf'\s*<string name="{k}"[^>]*>.*?</string>', re.S)
        if pat.search(s):
            s = pat.sub('\n' + line, s, count=1)
        else:
            s = s.replace('</resources>', line + '\n</resources>')
    open(p, 'w', encoding='utf-8').write(s)
print('ok')
