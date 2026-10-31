import os
OUT='/workspace/nibrasscan-release/store'
T = {
'en-US': dict(
name="Nibras Scan: PDF & OCR",
brief="Scan, OCR, sign & PDF",
short="Offline document scanner: PDF, OCR text, e-signature, merge, QR & folders",
new="""Version 1.1.0 — a big update:
• New home screen with all tools as cards
• Extract text (OCR) from whole PDFs; copy, share or export as TXT
• Search documents by name AND by the text inside them
• Folders, move to folder, sorting (date, name, size)
• Sign documents: draw your signature and place it on any page
• Merge PDFs, edit pages of existing PDFs (reorder, rotate, delete)
• Compress PDF (3 quality levels), watermark, password protection (AES)
• Export pages as JPG
• QR & barcode scanner with history (camera or image)
• Smaller, JPEG-compressed PDFs""",
full="""Nibras Scan turns your phone into a complete, private document office. Everything — scanning, text recognition, signing and PDF editing — runs on your device. No account, no ads, nothing is uploaded.

SCAN
• Camera scanning with automatic document edge detection and perspective correction
• Manual crop handles, rotate, and filters: Original, Auto-enhanced, Black & White, Grayscale
• Import photos from the gallery; multi-page documents

TEXT RECOGNITION (OCR)
• Extract text from a photo or from every page of a PDF
• Latin-script languages (English, Azerbaijani, Turkish, …) and Arabic
• Copy, share or export the text as a TXT file
• Recognised text is stored privately so you can search inside your documents

PDF TOOLS
• Create multi-page PDFs with three quality / size levels
• Merge several PDFs into one
• Edit pages of any PDF: reorder by drag, rotate, delete, re-crop, apply filters
• Compress PDF, add a text watermark, protect with a password (AES)
• Export pages as JPG images, share as PDF

E-SIGNATURE
• Draw your signature with your finger (3 ink colours), saved for next time
• Drag and pinch to place it exactly where you need on any page

LIBRARY
• Folders, move documents between folders, rename, delete
• Sort by newest, oldest, name or size
• Search by document name and by the text inside

QR & BARCODE SCANNER
• Reads QR codes and common barcodes (EAN, UPC, Code 128, Data Matrix, PDF417, Aztec…)
• Scan with the camera or from an image
• Private scan history: copy, share or open links

PRIVACY
• Works fully offline; no personal data is collected
• Camera is used only to scan documents and codes
• Files stay in the app's storage on your device until you share them

Languages: English, Azərbaycan, Türkçe, Русский, العربية.
Made by Nibras Code — nibrascode.com"""),
'az-AZ': dict(
name="Nibras Scan: PDF və OCR",
brief="Skan, OCR, imza, PDF",
short="Oflayn sənəd skaneri: PDF, mətn tanıma, e-imza, birləşdirmə, QR, qovluqlar",
new="""Versiya 1.1.0 — böyük yenilənmə:
• Bütün alətlər kartlarla göstərilən yeni ana ekran
• Bütöv PDF-dən mətn tanıma (OCR); kopyala, paylaş, TXT kimi ixrac et
• Sənədləri həm ada, həm də içindəki mətnə görə axtar
• Qovluqlar, qovluğa köçürmə, sıralama (tarix, ad, ölçü)
• İmza: imzanı çək və istənilən səhifəyə yerləşdir
• PDF birləşdirmə, mövcud PDF-in səhifələrini düzəltmə (sırala, döndər, sil)
• PDF sıxışdırma (3 səviyyə), su nişanı, parolla qoruma (AES)
• Səhifələri JPG kimi ixrac
• Tarixçəli QR və barkod skaneri (kamera və ya şəkil)
• Daha kiçik, JPEG ilə sıxılmış PDF-lər""",
full="""Nibras Scan telefonunu tam və məxfi sənəd ofisinə çevirir. Skan, mətn tanıma, imza və PDF redaktəsi — hamısı cihazında işləyir. Hesab yoxdur, reklam yoxdur, heç nə yüklənmir.

SKAN
• Sənəd kənarlarının avtomatik tapılması və perspektiv düzəlişi ilə kamera skanı
• Əl ilə kəsmə, döndərmə və filtrlər: Original, Avto-gücləndirmə, Ağ-qara, Boz ton
• Qalereyadan şəkil idxalı; çoxsəhifəli sənədlər

MƏTN TANIMA (OCR)
• Şəkildən və ya PDF-in bütün səhifələrindən mətn çıxarma
• Latın qrafikalı dillər (Azərbaycan, ingilis, türk…) və ərəb dili
• Mətni kopyala, paylaş və ya TXT faylı kimi ixrac et
• Tanınan mətn cihazda saxlanır ki, sənədlərin içində axtara biləsən

PDF ALƏTLƏRİ
• Üç keyfiyyət/ölçü səviyyəsi ilə çoxsəhifəli PDF
• Bir neçə PDF-i birinə birləşdirmə
• İstənilən PDF-in səhifələrini düzəltmə: sürüşdürərək sırala, döndər, sil, yenidən kəs, filtr tətbiq et
• PDF sıxışdırma, mətn su nişanı, parolla qoruma (AES)
• Səhifələri JPG kimi ixrac, PDF kimi paylaşma

ELEKTRON İMZA
• İmzanı barmağınla çək (3 rəng), növbəti dəfə üçün yadda qalır
• Sürüşdür və iki barmaqla ölçüsünü dəyiş — istənilən səhifədə dəqiq yerə qoy

KİTABXANA
• Qovluqlar, sənədləri qovluqlar arasında köçürmə, ad dəyişmə, silmə
• Ən yeni, ən köhnə, ad və ya ölçüyə görə sıralama
• Sənəd adına və içindəki mətnə görə axtarış

QR VƏ BARKOD SKANERİ
• QR kodlar və geniş yayılmış barkodlar (EAN, UPC, Code 128, Data Matrix, PDF417, Aztec…)
• Kamera ilə və ya şəkildən oxuma
• Məxfi skan tarixçəsi: kopyala, paylaş, linki aç

MƏXFİLİK
• Tam oflayn işləyir, şəxsi məlumat toplanmır
• Kamera yalnız sənəd və kod skanı üçündür
• Fayllar sən paylaşana qədər cihazında, tətbiqin yaddaşında qalır

Dillər: Azərbaycan, English, Türkçe, Русский, العربية.
Hazırlayan: Nibras Code — nibrascode.com"""),
'tr-TR': dict(
name="Nibras Scan: PDF ve OCR",
brief="Tara, OCR, imza, PDF",
short="Çevrimdışı belge tarayıcı: PDF, metin tanıma, e-imza, birleştirme, QR",
new="""Sürüm 1.1.0 — büyük güncelleme:
• Tüm araçların kartlarla gösterildiği yeni ana ekran
• Tüm PDF'ten metin tanıma (OCR); kopyala, paylaş, TXT olarak dışa aktar
• Belgelerde hem ada hem içindeki metne göre arama
• Klasörler, klasöre taşıma, sıralama (tarih, ad, boyut)
• İmza: imzanızı çizin ve herhangi bir sayfaya yerleştirin
• PDF birleştirme, mevcut PDF sayfalarını düzenleme (sırala, döndür, sil)
• PDF sıkıştırma (3 seviye), filigran, parola koruması (AES)
• Sayfaları JPG olarak dışa aktarma
• Geçmişli QR ve barkod tarayıcı (kamera veya görsel)
• Daha küçük, JPEG ile sıkıştırılmış PDF'ler""",
full="""Nibras Scan telefonunuzu eksiksiz ve gizli bir belge ofisine dönüştürür. Tarama, metin tanıma, imza ve PDF düzenleme tamamen cihazınızda çalışır. Hesap yok, reklam yok, hiçbir şey yüklenmez.

TARAMA
• Otomatik kenar algılama ve perspektif düzeltme ile kamera taraması
• Elle kırpma, döndürme ve filtreler: Orijinal, Otomatik iyileştirme, Siyah-Beyaz, Gri ton
• Galeriden içe aktarma; çok sayfalı belgeler

METİN TANIMA (OCR)
• Fotoğraftan veya PDF'in tüm sayfalarından metin çıkarma
• Latin alfabeli diller (Türkçe, İngilizce, Azerbaycanca…) ve Arapça
• Metni kopyalayın, paylaşın veya TXT olarak dışa aktarın
• Tanınan metin cihazda saklanır, böylece belgelerin içinde arama yapabilirsiniz

PDF ARAÇLARI
• Üç kalite/boyut seviyesiyle çok sayfalı PDF
• Birden çok PDF'i birleştirme
• Her PDF'in sayfalarını düzenleme: sürükleyerek sırala, döndür, sil, yeniden kırp, filtre uygula
• PDF sıkıştırma, metin filigranı, parola koruması (AES)
• Sayfaları JPG olarak dışa aktarma, PDF olarak paylaşma

E-İMZA
• İmzanızı parmağınızla çizin (3 renk), bir sonraki sefer için kaydedilir
• Sürükleyip iki parmakla boyutlandırarak istediğiniz sayfaya yerleştirin

KİTAPLIK
• Klasörler, belgeleri klasörler arasında taşıma, yeniden adlandırma, silme
• En yeni, en eski, ad veya boyuta göre sıralama
• Belge adına ve içindeki metne göre arama

QR VE BARKOD TARAYICI
• QR kodları ve yaygın barkodlar (EAN, UPC, Code 128, Data Matrix, PDF417, Aztec…)
• Kamerayla veya görselden okuma
• Gizli tarama geçmişi: kopyala, paylaş, bağlantıyı aç

GİZLİLİK
• Tamamen çevrimdışı çalışır, kişisel veri toplanmaz
• Kamera yalnızca belge ve kod taramak için kullanılır
• Dosyalar siz paylaşana kadar cihazınızda kalır

Diller: Türkçe, English, Azərbaycan, Русский, العربية.
Geliştirici: Nibras Code — nibrascode.com"""),
'ru-RU': dict(
name="Nibras Scan: PDF и OCR",
brief="Скан, OCR, подпись, PDF",
short="Офлайн-сканер документов: PDF, распознавание текста, подпись, объединение, QR",
new="""Версия 1.1.0 — большое обновление:
• Новый главный экран: все инструменты в виде карточек
• Распознавание текста (OCR) во всём PDF; копирование, отправка, экспорт в TXT
• Поиск документов по имени и по тексту внутри
• Папки, перемещение, сортировка (дата, имя, размер)
• Подпись: нарисуйте и поставьте на любую страницу
• Объединение PDF, редактирование страниц (порядок, поворот, удаление)
• Сжатие PDF (3 уровня), водяной знак, защита паролем (AES)
• Экспорт страниц в JPG
• Сканер QR и штрихкодов с историей (камера или изображение)
• Более компактные PDF со сжатием JPEG""",
full="""Nibras Scan превращает телефон в полноценный и приватный офис для документов. Сканирование, распознавание текста, подпись и редактирование PDF работают прямо на устройстве. Без аккаунта, без рекламы, ничего не загружается в сеть.

СКАНИРОВАНИЕ
• Сканирование камерой с автоматическим поиском краёв и исправлением перспективы
• Ручная обрезка, поворот и фильтры: Оригинал, Автоулучшение, Ч/Б, Оттенки серого
• Импорт из галереи; многостраничные документы

РАСПОЗНАВАНИЕ ТЕКСТА (OCR)
• Текст с фото или со всех страниц PDF
• Языки на латинице (английский, турецкий, азербайджанский…) и арабский
• Копирование, отправка или экспорт в TXT
• Распознанный текст хранится на устройстве — можно искать внутри документов

ИНСТРУМЕНТЫ PDF
• Многостраничные PDF с тремя уровнями качества/размера
• Объединение нескольких PDF в один
• Редактирование страниц любого PDF: перетаскивание, поворот, удаление, обрезка, фильтры
• Сжатие PDF, текстовый водяной знак, защита паролем (AES)
• Экспорт страниц в JPG, отправка в PDF

ЭЛЕКТРОННАЯ ПОДПИСЬ
• Нарисуйте подпись пальцем (3 цвета), она сохранится на будущее
• Перетаскивайте и масштабируйте, чтобы поставить её точно на нужной странице

БИБЛИОТЕКА
• Папки, перемещение между папками, переименование, удаление
• Сортировка: новые, старые, по имени или размеру
• Поиск по имени и по тексту внутри документа

СКАНЕР QR И ШТРИХКОДОВ
• QR-коды и популярные штрихкоды (EAN, UPC, Code 128, Data Matrix, PDF417, Aztec…)
• С камеры или из изображения
• Личная история сканирования: копировать, отправить, открыть ссылку

КОНФИДЕНЦИАЛЬНОСТЬ
• Полностью офлайн, персональные данные не собираются
• Камера используется только для сканирования документов и кодов
• Файлы остаются на устройстве, пока вы сами ими не поделитесь

Языки: Русский, English, Azərbaycan, Türkçe, العربية.
Разработчик: Nibras Code — nibrascode.com"""),
'ar': dict(
name="Nibras Scan: PDF و OCR",
brief="مسح، OCR، توقيع، PDF",
short="ماسح مستندات دون اتصال: PDF، استخراج النص، توقيع، دمج، QR ومجلدات",
new="""الإصدار 1.1.0 — تحديث كبير:
• شاشة رئيسية جديدة تعرض كل الأدوات على شكل بطاقات
• استخراج النص (OCR) من ملف PDF كامل؛ نسخ أو مشاركة أو تصدير TXT
• البحث في المستندات بالاسم وبالنص الموجود داخلها
• مجلدات ونقل إلى مجلد وترتيب (التاريخ، الاسم، الحجم)
• التوقيع: ارسم توقيعك وضعه على أي صفحة
• دمج ملفات PDF وتحرير صفحات PDF موجود (ترتيب، تدوير، حذف)
• ضغط PDF (3 مستويات)، علامة مائية، حماية بكلمة مرور (AES)
• تصدير الصفحات بصيغة JPG
• ماسح QR والباركود مع سجل (بالكاميرا أو من صورة)
• ملفات PDF أصغر بضغط JPEG""",
full="""يحوّل Nibras Scan هاتفك إلى مكتب مستندات متكامل وخاص. المسح واستخراج النص والتوقيع وتحرير PDF — كل ذلك يعمل على جهازك. بلا حساب، بلا إعلانات، ولا يُرفع أي شيء.

المسح
• مسح بالكاميرا مع كشف حواف المستند تلقائيًا وتصحيح المنظور
• قص يدوي وتدوير وفلاتر: الأصلي، تحسين تلقائي، أبيض وأسود، رمادي
• استيراد من المعرض؛ مستندات متعددة الصفحات

استخراج النص (OCR)
• استخراج النص من صورة أو من جميع صفحات PDF
• اللغات بالحروف اللاتينية (الإنجليزية، التركية، الأذربيجانية…) والعربية
• نسخ النص أو مشاركته أو تصديره كملف TXT
• يُحفظ النص على جهازك لتتمكن من البحث داخل مستنداتك

أدوات PDF
• إنشاء PDF متعدد الصفحات بثلاثة مستويات للجودة والحجم
• دمج عدة ملفات PDF في ملف واحد
• تحرير صفحات أي PDF: ترتيب بالسحب، تدوير، حذف، إعادة القص، تطبيق الفلاتر
• ضغط PDF، علامة مائية نصية، حماية بكلمة مرور (AES)
• تصدير الصفحات كصور JPG ومشاركتها كـ PDF

التوقيع الإلكتروني
• ارسم توقيعك بإصبعك (3 ألوان) ويُحفظ للمرة القادمة
• اسحبه وغيّر حجمه بإصبعين لتضعه بدقة على أي صفحة

المكتبة
• مجلدات، نقل المستندات بين المجلدات، إعادة التسمية، الحذف
• الترتيب حسب الأحدث أو الأقدم أو الاسم أو الحجم
• البحث باسم المستند وبالنص الموجود داخله

ماسح QR والباركود
• رموز QR والباركود الشائعة (EAN، UPC، Code 128، Data Matrix، PDF417، Aztec…)
• بالكاميرا أو من صورة
• سجل مسح خاص: نسخ، مشاركة، فتح الروابط

الخصوصية
• يعمل دون اتصال بالكامل، ولا تُجمع أي بيانات شخصية
• تُستخدم الكاميرا فقط لمسح المستندات والرموز
• تبقى الملفات على جهازك حتى تشاركها بنفسك

اللغات: العربية، English، Azərbaycan، Türkçe، Русский.
تطوير: Nibras Code — nibrascode.com"""),
}
os.makedirs(OUT, exist_ok=True)
for code, d in T.items():
    L=d['new'].split('\n')[:-1]
    L=[l for l in L if 'JPG' not in l] if len('\n'.join(L))>500 else L
    L[0]='v1.1.0:'
    d['new']='\n'.join(L)
    assert len(d['new'])<=500,(code,len(d['new']))
    assert len(d['name']) <= 30, (code, len(d['name']))
    assert len(d['brief']) <= 25, (code, d['brief'], len(d['brief']))
    assert len(d['short']) <= 80, (code, len(d['short']))
    assert len(d['full']) <= 4000, code
    print(code,'new',len(d['new']))
    open(f'{OUT}/listing_{code}.txt','w').write(f"""Language: {code}  (Google Play)

== App name ({len(d['name'])}/30) ==
{d['name']}

== Short description ({len(d['short'])}/80) ==
{d['short']}

== Full description ({len(d['full'])}/4000) ==
{d['full']}

== Release notes / What's new ({len(d['new'])}/500) ==
{d['new']}
""")
    open(f'{OUT}/appgallery_{code}.txt','w').write(f"""Language: {code}  (Huawei AppGallery)

== App name ==
Nibras Scan

== Brief introduction ({len(d['brief'])}/25) ==
{d['brief']}

== Introduction / full description ({len(d['full'])}/8000) ==
{d['full']}

== New features (1.1.0) ({len(d['new'])}/500) ==
{d['new']}
""")
    print(code, len(d['brief']), len(d['short']), len(d['full']), len(d['new']))
