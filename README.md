# EYMEN TV — Uydu Kanal Listesi

EYMEN TV, Android TV uyumlu cihazlar için geliştirilen uydu/TV kanal listesi uygulamasıdır. Eski launcher projesi iptal edildi; bu depo yalnızca TV kanal uygulamasını içerir.

## 📥 Android TV APK — v0.11

**[⬇ EYMEN TV v0.11 APK](https://github.com/mmustafatasoy/EymenTv/releases/tag/uydu-tv-v0.11)**

Bu depo **Private** olduğundan sürümü görüntülemek için yetkili GitHub hesabıyla oturum açmak gerekir. v0.10 ve eski sürümler Releases bölümünde korunur.

### V0.11 — Büyük reklam kaldırıldı

- Yayının altında görünen fotoğraflı, telefon numaralı, isimli **tam genişlikte reklam bandı tamamen kaldırıldı**. Bu bandın fotoğraf ve reklam logosu da APK paketinden çıkarıldı.
- Sağ üst köşedeki şeffaf **EYMEN TV** logosu ve **www.atasoyyazilim.com** web adresi korundu.
- Sağ üst logo **sürekli görünmez**: Uygulama içinde bir kanalın yayını açıkken, menü kapalıyken, **her 60 dakikada bir 15 saniye** boyunca belirir; ardından kaybolur.
- Son otomatik gösterim zamanı yerel olarak saklanır; TV/uygulama yeniden başlatıldığında süre sıfırlanmaz. İlk kurulumdan sonra ilk otomatik gösterim için 60 dakika beklenir.
- Elle deneme: **MENU → Uygulama ayarları → Görünüm, ses ve diğer ayarlar → Sağ üst logo ve web adresini göster (test)**.
- Reklam, fotoğraf, telefon, isim veya aşağıdan kayan bir tanıtım bandı artık yayın üzerinde gösterilmez.
- 10 kanallık sayfalar, sağ/sol kumandayla sayfa geçişi, kanal ekleme/düzenleme, çoklu kanal silme ve geri getirme, favoriler, kanal logoları, bilgi katmanı ve ses yönetimi korunur.

## TV cihaz desteği ve sınırlamalar

- Whale/DTVKit üzerinden erişilebilen kanallar öncelikli kullanılır.
- DTVKit bulunmayan Android TV modellerinde (ör. bazı Vestel cihazlarında) `TvContract.Channels` üzerinden **yalnızca cihazın erişime açtığı** kanallar listelenebilir.
- Üretici TV Input Framework izinlerini kapatmışsa uygulama sistem tunerine veya ses/video akışına erişemeyebilir. Cihazda gerçek oynatma testi gerekir.
- **TV'nin gerçek kanal veritabanını silmez.** Kanal gizleme ve geri getirme yalnızca EYMEN TV'nin yerel listesinde geçerlidir.
- Uygulama Android TV test derlemesidir. Kurulu APK ile yeni APK farklı imzalıysa güncelleme kurulamaması mümkündür.

## Derleme ve test

- Kod: `eymen-uydu-kontrol/`
- `AndroidManifest.xml`: versionCode 12, versionName 0.11
- Java 17, Android API 33 derleme araçları
- `build.sh` → `build/EYMEN-TV-0.11.apk`
- Testler: `tests/PageNavTest.java`, `tests/cross_tv_smoke_test.py`, `tests/brand_bulk_smoke_test.py`, `tests/promo_smoke_test.py`
- GitHub Actions: **Build EYMEN Uydu TV — Kanal Listesi**. Test ve APK derlemesi başarılı olduğunda `uydu-tv-v0.11` etiketiyle GitHub Releases'e yükler.
- 3D köşe logo kaynağı `assets/brand_3d.svg` ve bundan oluşturulan `assets/brand_3d.png` dosyasıdır; 8K PNG logo ayrı Release varlığı olarak yayımlanır.

## Sürüm geçmişi

- **v0.11:** Tam ekran reklam kaldırıldı, sağ üst logo ve adres saatte bir 15 saniye görünür.
- **v0.10:** 3D logo, toplu kanal silme, TV marka uyumluluğu ve alt bant promosyonu.
- **v0.5 / v0.4:** İlk kanal listesi ve kumanda denemeleri. Releases üzerinde mevcut olan önceki sürümler silinmez.

**Geliştirici:** Muhammet Atasoy — EYMEN TV
