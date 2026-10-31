# EYMEN TV — Whale OS / Android TV Başlatıcısı

**Bu proje EYMENkids'ten ayrıdır.** Onaylanan ana ekran, uygulamalar ve ayarlar görselleriyle, düşen **E Y M E N  T V** harflerinden oluşan açılış animasyonunu ve kumanda yön/OK/Geri kontrolünü içerir.

## Kurulum — ilk APK'yı oluşturma (telefondan da yapılabilir)

Bu depo özel (Private) olarak açıldı ve APK oluşturma iş akışı eklendi. **Tek gereken adım: Tam kaynak ZIP dosyasını deponun ana dizinine yüklemek.**

1. ChatGPT'de hazırlanmış `EYMEN-TV-WhaleOS-v1.0-KAYNAK-PROJE.zip` dosyasını indir. **ZIP'i açma ve adını değiştirme.**
2. GitHub'da [Add file → Upload files](https://github.com/mmustafatasoy/EymenTv/upload/main) bölümünü aç.
3. ZIP'i seç ve **Commit changes** düğmesine bas. GitHub tek bir dosya olarak yükler; bütün tasarımlar, kaynak kodlar ve grafikler arşivdedir.
4. [Actions](https://github.com/mmustafatasoy/EymenTv/actions/workflows/build.yml) bölümünden `EYMEN TV - Whale OS and Android Launcher` iş akışını aç. Dosya yüklemesiyle otomatik başlar; gerekirse **Run workflow** kullan.
5. İşlem yeşil olursa çalıştırmanın altındaki **Artifacts → EymenTv-Android-TV-Launcher-APK** dosyasını indir.
6. İnen ZIP'i açarak `app-debug.apk` dosyasını çıkar, FAT32 USB belleğe kopyala ve TV'nin dosya yöneticisinden açmayı dene.

**Lütfen önce EYMENkids'i veya TV'nin orijinal ana ekranını kaldırma.** EYMEN TV'yi normal uygulama olarak dene; izin verilirse sonradan varsayılan HOME olarak seçebilirsin.

## Gerçekte hangi sürüm çalışır?

| Özellik | Android uyumlu TV APK | Whale OS HTML5 |
|---|---|---|
| Birebir onaylanan ekran görselleri | Evet, APK içinde | Evet, HTTPS yayında |
| EYMEN TV düşen harf açılışı | Uygulama başlarken | Uygulama başlarken |
| Yön / OK / Geri | Evet, cihaz testi gerekli | Evet, cihaz testi gerekli |
| Kurulu uygulamaları listeleme/açma | Android izin verirse | Üretici entegrasyonu gerekir |
| Ana ekran (HOME) olarak seçme | Android izin verirse | Standart HTML5 izniyle mümkün değil |
| TV güç açılış logosunu değiştirme | Hayır | Hayır |
| HDMI görüntüsü / TV panel ayarları | Üretici API iznine bağlı | Üretici API iznine bağlı |

## Tasarım ve kaynak içerik

Kaynaktaki `assets/home.png`, `assets/apps.png`, `assets/settings.png` görselleri referans olarak onaylanan ekranlardır; üstlerindeki görünmez tuş seçim alanları JavaScript ile kontrol edilir. Görseller demo/temsili film afişleri içerir; gerçek yayın hakları veya video akışları bu projeyle sağlanmaz. Uygulama geliştirici özelleştirmesi dışında TV sistem bölümünü değiştirmez.

## Teknik yapı

- `index.html`, `styles.css`, `app.js`: HTML5 arayüz, açılış ve kumanda
- `config.json`: bağlantı ayarları
- `assets/*.png`: onaylanan ekran görselleri
- `android-launcher/`: Android `com.eymen.tv` APK ve uygulama listesi Java köprüsü
- `tests/check.mjs`: ekran/klavye doğrulama
- `.github/workflows/build.yml`: APK derleme

Kaynak ZIP yüklenirse iş akışı arşivi otomatik açıp bu dosyalarla derleme yapar. Böylece dosyaların tek tek yüklenmesi gerekmez.

**İmza uyarısı:** GitHub Actions'ın ürettiği deneme APK'sı debug imzalıdır; farklı CI çalıştırmaları arasında imza değişebilir. Güncelleme reddedilirse önce imza durumu ve uygulama verileri kontrol edilmelidir. İleride sorunsuz güncelleme için kalıcı ve gizli Android imzalama anahtarı kullanılmalıdır.

**Whale OS sınırı:** Whale OS'nin Android APK kurulumunu veya üçüncü taraf HOME seçimini kabul edip etmeyeceği TV modelinin firmware sürümüne bağlıdır. Android APK kurulumu desteklenmiyorsa HTML5 sürümünü Whale geliştirici portalından dağıtmak gerekir.
