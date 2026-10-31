# EYMEN TV — Uydu Kanal Listesi

**EYMEN TV artık yalnızca uydu/TV kanal listesi uygulamasıdır.** Eski TV ana ekranı (launcher) projesi iptal edilmiştir ve bu deponun `main` dalında bulunmaz.

## 📥 APK indir

**[⬇ EYMEN TV v0.5 — Saatlik Animasyonlu Alt Bant Reklamı APK](https://github.com/mmustafatasoy/EymenTv/releases/download/uydu-tv-v0.5/EYMEN-TV-Kanal-Listesi-v0.5.apk)**

[GitHub Releases — Kanal Listesi](https://github.com/mmustafatasoy/EymenTv/releases)

Önceki v0.4 sürümü Releases'de geri dönüş amacıyla korunur.

## Özellikler ve sınırlar

- Kanal listesinde kumandayla yön/OK/Geri kontrolü ve **10 kanallık sayfalar** arasında hızlı geçiş hedeflenir.
- Kanal ekleme, düzenleme, silme ve kanallara özel logo tanımlama arayüzü bulunur.
- Kanal listesi üzerinde şeffaf menü, kanal bilgi katmanı ve **EYMEN TV / atasoyyazılım.com** görsel markalaması geliştirme kapsamındadır.
- **DTVKit ve uydu alıcısı entegrasyonu; gerçek tuner, kanal geçişi, görüntü ve ses kontrolü** cihazın izin ve API'lerine bağlıdır. Senna/Whale OS üzerinde doğrulanmadan tüm kanalların açıldığı veya sesin çalıştığı iddia edilmemelidir.
- Bu APK **Android uyumlu TV'ler için test sürümüdür**. Whale OS üzerinde APK kurulum ve sistem kanal uygulamasına erişim ayrıca test gerektirir.
- EYMENkids bu depodan bağımsızdır ve değiştirilmez.

## Yayın içi reklam — v0.5

- Reklam **yayının alt kısmında tam genişlikte**, klasik TV alt bant reklamı gibi görünür; sağ altta bağımsız bir kutu değildir.
- Aile fotoğrafı önce alttan kayarak açılır; ardından **www.atasoyyazılım.com**, **0531 221 89 65**, **MUHAMMET ATASOY** yazıları sıralı şekilde belirir, en son EYMEN TV logosu animasyonla gelir.
- **Yalnızca uygulama yayını açık ve kanal listesi kapalı iken**, son gösterimden **en az 60 dakika** sonra görünür ve **15 saniye** sürer.
- Zamanlamanın uygulama kapatılıp açıldığında da korunması için SharedPreferences kullanılır.
- İlk otomatik gösterim ilk çalıştırmadan 60 dakika sonradır; **Ayarlar → Reklamı şimdi göster (test)** ile hemen test edilebilir.
- Fotoğraf `eymen-uydu-kontrol/assets/promo_family.jpg` dosyasındadır. Şu an APK boyutunu azaltmak için düşük boyutlu önizleme fotoğrafı kullanılır; aynı dosya yolu üzerine daha yüksek çözünürlüklü fotoğraf yüklenebilir.
- Bu bant TV işletim sisteminin tüm kanallarında değil **EYMEN TV uygulamasında açılan yayınlarda** çalışır. Sistem yayın katmanında ayrı uygulamalara reklam bindirebilme garanti edilmez.

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
