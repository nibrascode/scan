# Builds framed store screenshots (1080x1920) and feature graphics (1024x500) per language.
import os, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
SRC = sys.argv[1]; OUT = sys.argv[2]; ICON = sys.argv[3]
FONT_B = "/usr/share/fonts/truetype/sand-box/google/Noto Sans/NotoSans-VariableFont_wdth,wght.ttf"
AR = "/usr/share/fonts/truetype/sand-box/google/Noto Sans Arabic/NotoSansArabic-VariableFont_wdth,wght.ttf"
LANGS = {'az':'az-AZ','en':'en-US','tr':'tr-TR','ru':'ru-RU','ar':'ar'}
CAP = {
 'en':[("Scan documents in seconds","Auto edge detection, crop and clean filters"),
       ("Your library, organised","Folders, sorting and search inside text"),
       ("Edit every page","Reorder, rotate, crop, B/W, grayscale, enhanced"),
       ("Extract text (OCR)","Latin and Arabic, on your device — copy or share"),
       ("Sign with your finger","Your signature is saved for next time"),
       ("Place it anywhere","Drag and pinch your signature onto any page"),
       ("QR & barcode scanner","Every scan kept in a private history"),
       ("100% offline & private","No account, no ads, nothing uploaded")],
 'az':[("Sənədləri saniyələrə skan et","Avtomatik kənar tapma, kəsmə və filtrlər"),
       ("Kitabxanan qaydasında","Qovluqlar, sıralama və mətn içində axtarış"),
       ("Hər səhifəni düzəlt","Sırala, döndər, kəs, ağ-qara, boz, gücləndir"),
       ("Mətni tanı (OCR)","Latın və ərəb, cihazda — kopyala, paylaş"),
       ("Barmağınla imza at","İmzan növbəti dəfə üçün yadda qalır"),
       ("İstədiyin yerə qoy","İmzanı sürüşdür, böyüt, istənilən səhifəyə qoy"),
       ("QR və barkod skaneri","Bütün skanlar şəxsi tarixçədə"),
       ("100% oflayn və məxfi","Hesab yox, reklam yox, heç nə yüklənmir")],
 'tr':[("Belgeleri saniyeler içinde tara","Otomatik kenar algılama, kırpma ve filtreler"),
       ("Kitaplığın düzenli","Klasörler, sıralama ve metin içinde arama"),
       ("Her sayfayı düzenle","Sırala, döndür, kırp, siyah-beyaz, gri, iyileştir"),
       ("Metni tanı (OCR)","Latin ve Arapça, cihazda — kopyala veya paylaş"),
       ("Parmağınla imzala","İmzan bir sonraki sefer için kaydedilir"),
       ("İstediğin yere yerleştir","İmzayı sürükle, büyüt, her sayfaya koy"),
       ("QR ve barkod tarayıcı","Tüm taramalar özel geçmişte"),
       ("%100 çevrimdışı ve gizli","Hesap yok, reklam yok, hiçbir şey yüklenmez")],
 'ru':[("Сканируйте за секунды","Автопоиск краёв, обрезка и фильтры"),
       ("Библиотека в порядке","Папки, сортировка и поиск по тексту"),
       ("Редактируйте страницы","Порядок, поворот, обрезка, Ч/Б, серый, улучшение"),
       ("Распознавание текста","Латиница и арабский на устройстве"),
       ("Подпись пальцем","Подпись сохраняется на следующий раз"),
       ("Поставьте где угодно","Перетащите и масштабируйте подпись"),
       ("Сканер QR и штрихкодов","Все сканы в личной истории"),
       ("100% офлайн и приватно","Без аккаунта, без рекламы, без загрузок")],
 'ar':[("امسح المستندات في ثوانٍ","كشف الحواف تلقائيًا والقص والفلاتر"),
       ("مكتبتك منظّمة","مجلدات وترتيب وبحث داخل النص"),
       ("حرّر كل صفحة","ترتيب وتدوير وقص وأبيض وأسود ورمادي وتحسين"),
       ("استخراج النص (OCR)","اللاتينية والعربية على جهازك — انسخ أو شارك"),
       ("وقّع بإصبعك","يُحفظ توقيعك للمرة القادمة"),
       ("ضعه في أي مكان","اسحب التوقيع وكبّره على أي صفحة"),
       ("ماسح QR والباركود","كل عمليات المسح في سجل خاص"),
       ("دون اتصال وخصوصية تامة","بلا حساب ولا إعلانات ولا رفع أي ملف")],
}
FG = {'en':("Nibras Scan","Scan · OCR · Sign · Merge · QR — offline"),
      'az':("Nibras Scan","Skan · OCR · İmza · Birləşdir · QR — oflayn"),
      'tr':("Nibras Scan","Tara · OCR · İmza · Birleştir · QR — çevrimdışı"),
      'ru':("Nibras Scan","Скан · OCR · Подпись · PDF · QR — офлайн"),
      'ar':("Nibras Scan","مسح · OCR · توقيع · دمج · QR — دون اتصال")}

def font(lang, size, weight=700):
    f = ImageFont.truetype(AR if lang == 'ar' else FONT_B, size)
    f.set_variation_by_axes([weight, 100])
    return f

def grad(w, h):
    top = (10, 17, 40); bot = (21, 70, 150)
    im = Image.new('RGB', (w, h))
    d = ImageDraw.Draw(im)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)))
    glow = Image.new('RGB', (w, h), (0, 0, 0)); gd = ImageDraw.Draw(glow)
    gd.ellipse([w * 0.55, -h * 0.1, w * 1.3, h * 0.35], fill=(47, 176, 255))
    glow = glow.filter(ImageFilter.GaussianBlur(160))
    return Image.blend(im, Image.composite(glow, im, glow.convert('L')), 0.35)

def wrap(d, text, f, maxw):
    words = text.split(); lines = []; cur = ''
    for w in words:
        t = (cur + ' ' + w).strip()
        if d.textlength(t, font=f) <= maxw: cur = t
        else: lines.append(cur); cur = w
    lines.append(cur); return lines

def centered(d, y, text, f, fill, W, lang):
    for line in wrap(d, text, f, W - 120):
        kw = {'direction': 'rtl'} if lang == 'ar' else {}
        try: tw = d.textlength(line, font=f, **kw)
        except Exception: tw = d.textlength(line, font=f); kw = {}
        d.text(((W - tw) / 2, y), line, font=f, fill=fill, **kw)
        y += int(f.size * 1.25)
    return y

for lang, code in LANGS.items():
    od = os.path.join(OUT, 'screenshots', code); os.makedirs(od, exist_ok=True)
    for i in range(8):
        W, H = 1080, 1920
        bg = grad(W, H); d = ImageDraw.Draw(bg)
        t, s = CAP[lang][i]
        y = centered(d, 90, t, font(lang, 70, 800), (255, 255, 255), W, lang)
        y = centered(d, y + 14, s, font(lang, 38, 500), (190, 220, 255), W, lang)
        shot = Image.open(os.path.join(SRC, lang, f'0{i+1}.png')).convert('RGB')
        pw = 760; ph = int(shot.height * pw / shot.width)
        shot = shot.resize((pw, ph), Image.LANCZOS)
        top = y + 60
        frame_w = pw + 36
        fx = (W - frame_w) // 2
        frame = Image.new('RGBA', (frame_w, ph + 36), (0, 0, 0, 0)); fd = ImageDraw.Draw(frame)
        fd.rounded_rectangle([0, 0, frame_w - 1, ph + 35], 70, fill=(12, 14, 22, 255), outline=(70, 90, 130, 255), width=3)
        mask = Image.new('L', (pw, ph), 0); ImageDraw.Draw(mask).rounded_rectangle([0, 0, pw - 1, ph - 1], 54, fill=255)
        frame.paste(shot, (18, 18), mask)
        shadow = Image.new('RGBA', (W, H), (0, 0, 0, 0)); ImageDraw.Draw(shadow).rounded_rectangle([fx + 10, top + 30, fx + frame_w + 10, top + ph + 70], 70, fill=(0, 0, 0, 140))
        shadow = shadow.filter(ImageFilter.GaussianBlur(30))
        bg = Image.alpha_composite(bg.convert('RGBA'), shadow)
        bg.paste(frame, (fx, top), frame)
        bg.convert('RGB').save(os.path.join(od, f'0{i+1}.png'), optimize=True)
    # feature graphic
    W, H = 1024, 500
    fg = grad(W, H).convert('RGBA'); d = ImageDraw.Draw(fg)
    icon = Image.open(ICON).convert('RGBA').resize((150, 150), Image.LANCZOS)
    m = Image.new('L', (150, 150), 0); ImageDraw.Draw(m).rounded_rectangle([0, 0, 149, 149], 36, fill=255)
    fg.paste(icon, (70, 80), m)
    d.text((70, 250), FG[lang][0], font=font('en', 72, 800), fill=(255, 255, 255))
    sub = FG[lang][1]
    kw = {'direction': 'rtl'} if lang == 'ar' else {}
    sz = 30
    while d.textlength(sub, font=font(lang, sz, 600), **kw) > 430 and sz > 16: sz -= 1
    d.text((72, 352), sub, font=font(lang, sz, 600), fill=(170, 215, 255), **kw)
    shot = Image.open(os.path.join(SRC, lang, '01.png')).convert('RGB')
    pw = 200; ph = int(shot.height * pw / shot.width); shot = shot.resize((pw, ph), Image.LANCZOS)
    mk = Image.new('L', (pw, ph), 0); ImageDraw.Draw(mk).rounded_rectangle([0, 0, pw - 1, ph - 1], 28, fill=255)
    fg.paste(shot, (W - pw - 90, 40), mk)
    shot2 = Image.open(os.path.join(SRC, lang, '04.png')).convert('RGB').resize((pw, ph), Image.LANCZOS)
    fg.paste(shot2, (W - 2 * pw - 100, 90), mk)
    fg.convert('RGB').save(os.path.join(OUT, f'feature_graphic_{code}.png'))
print('done')
