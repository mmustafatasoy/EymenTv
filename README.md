# EYMEN TV 2.0

Android TV için baştan yazılmış canlı yayın uygulaması. TV'nin uydu kanallarını (TV Input Framework / DTVKit) tek, hızlı ve düzenli bir arayüzde toplar. Uydu sinyali kesilince aynı kanal internetten devam eder.

## Kurulum

TV'deki **Downloader** uygulamasına şu adresi yazın:

```
https://github.com/mmustafatasoy/EymenTv/releases/latest/download/EYMEN-TV.apk
```

v0.12 ile aynı paket adı ve imzayı kullanır; eski sürümün üzerine güncelleme olarak kurulur. Gizlenen kanallar, favoriler, kanal adları, sıra ve özel logolar korunur.

## Özellikler

- Açılışta EYMEN TV logosu ve M. Muhammet Atasoy imzası
- Kanal bandı: şimdi ve sonraki program, ilerleme, uydu türü, çözünürlük, ses dili, altyazı
- Kanal rehberi: kategoriler (Haber, Spor, Çocuk, Belgesel, Müzik, Film, Dini, Radyo, İnternet), arama
- Yayın akışı ızgarası, canlı önizleme ve program hatırlatıcıları
- Ana ekran: kaldığın yerden, favoriler, hatırlatıcılar
- Ayarlar ve kanal yönetimi: sırala, gizle, toplu işaretle, yeniden adlandır, logo, kategori, PIN kilidi
- Uydu yoksa internetten devam et (M3U listesi adresi veya USB'den dosya)
- Telefon kumandası: TV'deki QR kodu okutun, telefonun tarayıcısında açılır
- Uygulama içinden güncelleme denetimi (GitHub Releases)

## Kumanda

| Tuş | Canlı yayında | Kanal rehberinde | Yayın akışında |
| --- | --- | --- | --- |
| OK | Kanal rehberi | Kanalı aç | İzle / hatırlatıcı kur |
| ▲ ▼ / CH± | Kanal değiştir | Kanallarda gez | Kanallarda gez |
| ◀ ▶ | Rehber / kanal bandı | Kategori | Programlarda gez |
| Kırmızı | Yayın akışı | Yayın akışı | Önceki gün |
| Yeşil | Favori | Favori | Favori |
| Sarı | Ayrıntı | Ara | Sonraki gün |
| Mavi | Ses · altyazı · internet yayını | | Kategori |
| MENU | Ayarlar | Ayarlar | Ayarlar |
| Geri | Bandı kapat / ana ekran | Rehberi kapat | Canlı yayın |

## Geliştirme

Kotlin + Jetpack Compose, tek modül (`app/`). GitHub Actions her `main` gönderiminde imzalı APK'yı derler ve GitHub Releases'a koyar (`.github/workflows/build.yml`). Derleme sonucu ve günlüğü `ci-logs` dalına yazılır.

Eski v0.12 kaynağı `eymen-uydu-kontrol/` klasöründe duruyor; imza anahtarı da oradan okunur.

Atasoy Yazılım · atasoyyazilim.com
