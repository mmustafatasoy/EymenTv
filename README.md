# EYMEN TV — Whale OS / Android TV Launcher

Görselleri onaylanan **EYMEN TV** ana ekranı, uygulamalar ve ayarlar tasarımını kullanan özel TV başlatıcısı. EYMENkids uygulamasından **ayrı** bir projedir.

## APK indirme

**[GitHub Actions → Build EYMEN TV APK](https://github.com/mmustafatasoy/EymenTv/actions/workflows/build.yml)**

1. Yukarıdaki Actions bağlantısını aç.
2. En üstteki **Build EYMEN TV APK** çalıştırmasını seç; yeşil **success** olmasını kontrol et.
3. Sayfanın altında **Artifacts → EymenTv-Android-TV-Launcher-APK** dosyasını indir.
4. İndirilen arşivden `app-debug.apk` dosyasını çıkar, USB belleğe aktar.
5. Televizyondaki dosya yöneticisinden APK'yı aç ve TV izin veriyorsa yükle.
6. EYMEN TV'yi önce **normal uygulama** olarak aç; kumanda yön, OK, Geri ve uygulama açmayı test et.

**ZIP kaynak dosyasını elle yüklemene gerek yok.** Onaylanan tasarımları içeren HTML5 ekran, Git deposunda 13 parça hâlinde tutuluyor. GitHub Actions derleme sırasında bu parçaları birleştirip doğrudan APK içerisine yerleştirir. Aynı HTML5 sayfa Whale TV geliştirici ortamında da kullanılabilir.

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
