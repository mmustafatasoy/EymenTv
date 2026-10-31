# EYMEN TV — Uydu Kanal Listesi

**EYMEN TV artık yalnızca uydu/TV kanal listesi uygulamasıdır.** Eski TV ana ekranı (launcher) projesi iptal edilmiştir ve bu deponun `main` dalında bulunmaz.

## 📥 APK indir

**[⬇ EYMEN TV v0.10 — Saatlik Animasyonlu Alt Bant Reklamı APK](https://github.com/mmustafatasoy/EymenTv/releases/download/uydu-tv-v0.10/EYMEN-TV-Kanal-Listesi-v0.10.apk)**

[GitHub Releases — Kanal Listesi](https://github.com/mmustafatasoy/EymenTv/releases)

Önceki v0.4 ve v0.5 sürümleri Releases'de geri dönüş amacıyla korunur.

## Dönen şeffaf logo ve toplu kanal silme — v0.10

- Dünya küresi bulunmayan yeni şeffaf vektör EYMEN TV logosu ile web adresi sağ üst köşede 3D perspektif hissiyle sürekli dönüyor.
- Performans için uygulama içine optimize edilmiş şeffaf `assets/brand_3d.svg / brand_3d.png` eklenmiştir. Ayrı **7680×4320 şeffaf PNG** SVG vektör kaynaktan doğrudan oluşturulur ve GitHub Releases'e APK ile birlikte yüklenir.
- `MENU → Uygulama ayarları → Toplu kanal sil (işaretleyerek)` seçeneğiyle istenen sayıda kanal işaretlenir; ikinci onaydan sonra birlikte EYMEN TV listesinden kaldırılır.
- Kaldırılan kanallar `Silinen kanalları geri getir` aracılığıyla geri getirilebilir. TV'nin gerçek kanal veritabanı kesinlikle silinmez.
- Eski kanal menüsü, kumanda, kanal sayfaları ve saatlik reklam aynı şekilde kalır.

## Vestel ve diğer TV'lerde kanal desteği — v0.10

- Whale TV'de mevcut DTVKit yayın kaynağı aynen tercih edilir.
- DTVKit bulunmadığında Vestel dahil farklı üreticilerin erişime açtığı `TvContract.Channels` kayıtları da kanal listesinde gösterilir.
- Kanal veritabanı boş veya erişime kapalıysa uygulama kaç kayıt ve kaç TV girişi görülebildiğini gösterir.
- **TV üreticisinin sistem kayıtlarını gizlemesi durumunda normal APK ile bunu aşmak mümkün olmayabilir.** Görünen bir kanalın oynatılabilmesi de tuner izinlerine bağlıdır.
- **Kanal menüsü, kumanda kısayolları ve reklam çizimi değiştirilmedi.**

## Özellikler ve sınırlar

- Kanal listesinde kumandayla yön/OK/Geri kontrolü ve **10 kanallık sayfalar** arasında hızlı geçiş hedeflenir.
- Kanal ekleme, düzenleme, silme ve kanallara özel logo tanımlama arayüzü bulunur.
- Kanal listesi üzerinde şeffaf menü, kanal bilgi katmanı ve **EYMEN TV / atasoyyazılım.com** görsel markalaması geliştirme kapsamındadır.
- **DTVKit ve uydu alıcısı entegrasyonu; gerçek tuner, kanal geçişi, görüntü ve ses kontrolü** cihazın izin ve API'lerine bağlıdır. Senna/Whale OS üzerinde doğrulanmadan tüm kanalların açıldığı veya sesin çalıştığı iddia edilmemelidir.
- Bu APK **Android uyumlu TV'ler için test sürümüdür**. Whale OS üzerinde APK kurulum ve sistem kanal uygulamasına erişim ayrıca test gerektirir.
- EYMENkids bu depodan bağımsızdır ve değiştirilmez.

## Yayın içi reklam — v0.10

- Reklam **yayının alt kısmında tam genişlikte**, klasik TV alt bant reklamı gibi görünür; sağ altta bağımsız bir kutu değildir.
- Aile fotoğrafı altın renkli ışık yayıyla açılır, kırmızı/mavi neon prodüksiyon çizgileri hareket eder; ardından **www.atasoyyazılım.com**, **0531 221 89 65**, **MUHAMMET ATASOY** yazıları sıralı şekilde belirir, en son EYMEN TV logosu animasyonla gelir.
- **Yalnızca uygulama yayını açık ve kanal listesi kapalı iken**, son gösterimden **en az 60 dakika** sonra görünür ve **15 saniye** sürer.
- Zamanlamanın uygulama kapatılıp açıldığında da korunması için SharedPreferences kullanılır.
- İlk otomatik gösterim ilk çalıştırmadan 60 dakika sonradır; **Ayarlar → Reklamı şimdi göster (test)** ile hemen test edilebilir.
- **Yalnızca reklam görselliği değişmiştir; kanal menüsü, kumanda davranışı, kanal logoları ve kanal oynatma kodları aynı kalmıştır.**
- Fotoğraf `eymen-uydu-kontrol/assets/promo_family.jpg` dosyasındadır. Artık 490×205 boyutlu onaylanan aile fotoğrafı ve gerçek parlak EYMEN TV logosu (`assets/promo_logo.jpg`, 390×110) APK içinde bulunur. İlk düşük çözünürlüklü yer tutucu kaldırılmıştır.
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
