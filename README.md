# Nibras Scan (Android, Kotlin) — 1.0.0

Sənəd skaneri: kamera və ya qalereya → kənar düzəltmə → filtr → çoxsəhifəli PDF → OCR.

## 1.0.0 sürət
- Kəsim Skia `setPolyToPoly` ilə gedir (köhnə piksel döngüsü yoxdur).
- Çəkiliş `MINIMIZE_LATENCY`; ölçü PDF keyfiyyətinə uyğundur.
- Ağ-qara filtr ən çoxu 1600px üzərində, sonra böyüdülür.
- Səhifə önizləməsi keşlənir.
- Sənəd siyahısı thumbnail-i diskdə saxlayır.

## 1.1
OpenCV və ehtiyat kənar tapma, qalereya, flaş, yarımçıq skanın saxlanması, axtarış, tam çözünürlüklü PDF.
