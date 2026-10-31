# EYMEN TV — Whale OS / Android TV Launcher

Görselleri onaylanan **EYMEN TV** ana ekranı, uygulamalar ve ayarlar tasarımını kullanan özel TV başlatıcısı. EYMENkids uygulamasından **ayrı** bir projedir.

## APK indirme — GitHub Releases

**[⬇ EYMEN TV v1.0 Test APK indir (11,4 MB)](https://github.com/mmustafatasoy/EymenTv/releases/download/v1.0.0-test/EymenTv-v1.0.0-test.apk)**

**[📦 v1.0.0-test Releases sayfası](https://github.com/mmustafatasoy/EymenTv/releases/tag/v1.0.0-test)**

1. Yukarıdaki APK bağlantısını açarak `EymenTv-v1.0.0-test.apk` dosyasını indir.
2. APK'yı FAT32 biçimli USB belleğe kopyala.
3. Televizyonun dosya yöneticisinden APK dosyasını aç. Cihaz dışarıdan uygulama kurmayı destekliyorsa kurulum ekranı açılır.
4. EYMEN TV'yi önce **normal uygulama** olarak başlat ve kumanda yön/OK/Geri tuşlarını test et.
5. Yalnızca TV izin veriyorsa `Ayarlar → Varsayılan uygulamalar → Ana ekran` veya cihazın sunduğu HOME seçim ekranında EYMEN TV'yi seç.

**Derleme yeşil başarılı:** [GitHub Actions — Build EYMEN TV APK](https://github.com/mmustafatasoy/EymenTv/actions/workflows/build.yml).

APK, GitHub Actions depolama kotasından bağımsız olması için doğrudan **Releases** bölümüne yüklenir. **Kaynak ZIP yüklemen gerekmez**: Referans ekranların tamamı bu depoda saklanır ve derleme sırasında birleştirilir.

## Tasarım ve özellikler

- Düşen harflerle **EYMEN TV** animasyonu (uygulama açıldığında)
- Referans görsellerin aynısını içeren ana sayfa, uygulamalar ve ayarlar
- Kumanda yön / OK / Geri tuşlarıyla gezinme
- Android uyumlu cihazda kurulu uygulamaları listeleme, görsel kartlara eşleştirme, favoriler
- Uygulama içi parlaklık, kontrast ve görünüm tercihleri (TV panel ayarları değildir)
- Android izin verirse HOME (ana ekran) seçimi

## Teknik kaynaklar

- `web-parts/part-00.txt` — `part-12.txt`: Onaylanan ekranların PNG verilerini, CSS ve JavaScript'i içeren tam HTML5 arayüz
- `android-launcher/`: Android TV uygulama kabuğu, Java köprüsü ve derleme dosyaları
- `.github/workflows/build.yml`: Parçaları birleştirip Java 17 / Android Gradle ile APK oluşturur

## Whale OS ile ilgili sınırlamalar

**Normal bir uygulama, TV güç düğmesiyle açılırken gösterilen üretici logosunu veya Whale OS sistem başlatıcısını otomatik değiştiremez.** Android uyumlu ve üçüncü taraf HOME uygulamalarına izin veren cihazlarda varsayılan başlatıcı seçimi mümkün olabilir. Bunun Senna/Whale TV üzerinde desteklendiği henüz doğrulanmadı. HDMI girişleri ve TV'nin gerçek görüntü/ses ayarları için üretici yetkileri gerekir.

**Uygulama güvenliği:** Çıkan APK deneme/debug sürümüdür; kalıcı ve güvenli imza anahtarıyla yayın sürümüne geçmeden önce mevcut kurulumun verilerini koruma planı hazırlanmalıdır. Debug imzalı farklı APK'lar birbirinin üzerine güncellenemeyebilir. Orijinal TV sistem uygulamasını ve EYMENkids'i kaldırmayın.

**Telif hakları:** Referans tasarım görsellerindeki film/dizi afişleri örnektir. Bu uygulama hakları alınmış film, dizi veya canlı TV yayını sağlamaz.
