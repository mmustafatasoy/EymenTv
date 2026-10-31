# EYMEN TV — Uydu Kanal Listesi

**EYMEN TV artık yalnızca uydu/TV kanal listesi uygulamasıdır.** Eski TV ana ekranı (launcher) projesi iptal edilmiştir ve bu deponun `main` dalında bulunmaz.

## 📥 APK indir

**[EYMEN TV — Kanal Listesi v0.4 APK](https://github.com/mmustafatasoy/EymenTv/releases/download/uydu-tv-v0.4/EYMEN-TV-Kanal-Listesi-v0.4.apk)**

**[GitHub Releases — Kanal Listesi](https://github.com/mmustafatasoy/EymenTv/releases/tag/uydu-tv-v0.4)**

Bu depo özel (Private) ise indirmek için depoya erişim yetkisi bulunan GitHub hesabıyla giriş yapılmalıdır.

## Özellikler ve sınırlar

- Kanal listesinde kumandayla yön/OK/Geri kontrolü ve **10 kanallık sayfalar** arasında hızlı geçiş hedeflenir.
- Kanal ekleme, düzenleme, silme ve kanallara özel logo tanımlama arayüzü bulunur.
- Kanal listesi üzerinde şeffaf menü, kanal bilgi katmanı ve **EYMEN TV / atasoyyazılım.com** görsel markalaması geliştirme kapsamındadır.
- **DTVKit ve uydu alıcısı entegrasyonu; gerçek tuner, kanal geçişi, görüntü ve ses kontrolü** cihazın izin ve API'lerine bağlıdır. Senna/Whale OS üzerinde doğrulanmadan tüm kanalların açıldığı veya sesin çalıştığı iddia edilmemelidir.
- Bu APK **Android uyumlu TV'ler için test sürümüdür**. Whale OS üzerinde APK kurulum ve sistem kanal uygulamasına erişim ayrıca test gerektirir.
- EYMENkids bu depodan bağımsızdır ve değiştirilmez.

## Kaynak kod ve otomatik derleme

Proje kaynakları `eymen-uydu-kontrol/` içinde bulunur. Bu proje ilk aktarımda `uydu-packages/eymen-tv-0.4.b64` arşivinden güvenli biçimde açılarak ana dala yerleştirilir; kaynak aktarıldığında arşiv kaldırılır.

- **[Actions — Build EYMEN Uydu TV](https://github.com/mmustafatasoy/EymenTv/actions/workflows/build.yml)**
- İş akışı, kanal sayfaları için Java testlerini çalıştırır; Android API 33 ile APK derler.
- Testten geçen kanal sürümü **Releases** sayfasında yayımlanır. Arşivden kaynak aktarımı ilk başarılı derlemeden sonra tamamlanır.

## Televizyona kurulum

1. Yukarıdaki APK'yı indirip USB belleğe kopyala.
2. TV harici APK yüklemeye izin veriyorsa dosya yöneticisinden açıp kur.
3. Uygulamayı normal TV uygulaması olarak başlat; **varsayılan HOME/launcher olarak seçme**.
4. Kanal sayfalarını, kumandayı ve sesi test et. Cihaz üreticisi uydu tuneri erişimini engelliyorsa uygulama orijinal uydu yazılımının yerine geçemeyebilir.

**Uyarı:** APK'nın imzası kurulu farklı bir sürümle uyuşmuyorsa Android güncellemeyi reddedebilir. Verileri korumak için mevcut uygulamayı kaldırmadan önce bir yedekleme planı yap. TV'nin sistem uygulamalarını veya EYMENkids'i kaldırma.

## Geliştirici

Muhammet Atasoy — EYMEN TV
