# EYMEN TV — Uydu Kanal Listesi (v0.11)

**EYMEN TV**, Android uyumlu TV'lerde kanal listesi, kumandayla 10 kanallık sayfalar, favoriler, kanal düzenleme ve DTVKit/TV Input Framework üzerinden cihazın izin verdiği yayınları açmayı hedefleyen uygulamadır. TV başlatıcısı (launcher) değildir.

## 📥 EYMEN TV v0.11 APK

**[EYMEN TV v0.11 — Reklamsız / saatlik üst logo ve web adresi](https://github.com/mmustafatasoy/EymenTv/releases/tag/uydu-tv-v0.11)**

[Önceki APK sürümleri](https://github.com/mmustafatasoy/EymenTv/releases)

APK, Android derleme ve testleri başarılı olduğunda yukarıdaki GitHub Releases sürümüne eklenir. Kod deposu özel olduğundan indirme için GitHub hesap erişimi gerekebilir.

## Yenilikler: Reklam kaldırıldı

- **Fotoğraflı alt bant reklamı tamamen kaldırıldı.** Yayını kaplayan fotoğraf, telefon numarası, geliştirici ismi ve neon reklam animasyonları artık gösterilmez.
- **Sağ üstteki EYMEN TV logosu sürekli kalır.** Şeffaf vektörden oluşturulur, dönen 3D hissi korunur. Web adresi sürekli gösterilen sağ logodan çıkarıldı.
- **Üstteki EYMEN TV logosu ve `www.atasoyyazilim.com` adresi**, TV uygulamasında kanal açık ve menü kapalıyken **60 dakikada bir, 15 saniye** görünür; ardından kaybolur.
- Son otomatik görünme zamanı uygulama yeniden açılsa da saklanır. Yeni kurulumda ilk otomatik gösterim ilk açılıştan yaklaşık bir saat sonradır.
- Test için **MENU → Seçenekler → Saatlik logo/web göster (test)** kullanılabilir. Bu test saatlik zamanlayıcıyı sıfırlamaz.
- Reklam fotoğrafı ve eski reklam logosunun dosyaları da APK'dan çıkarıldı.

## Değiştirilmeyenler

- Kanal listesi ve kumandayla sağ/sol 10'lu sayfa geçişi
- Favorilere ekleme ve çıkarma
- Kanal logosu seçme ve düzenleme
- İşaretleyerek toplu kanal silme ve geri getirme (TV'nin sistem veritabanını silmez)
- Kanal bilgisi ve EPG katmanı, ses izini seçme ve mevcut TV tuner yaklaşımı
- Vestel/DTVKit uyumluluk denemeleri ve mevcut erişim sınırları

## TV uyumluluğu

TV üreticisi kanal kayıtlarını veya tuner erişimini uygulamalara kapatmış olabilir. Uygulamanın kanalları listeleyebilmesi, görüntü ve sesi açabileceği anlamına gelmez. Fiziksel TV'de test gerektirir.

## Derleme

[GitHub Actions — EYMEN Uydu TV](https://github.com/mmustafatasoy/EymenTv/actions/workflows/build.yml)

Kaynak dosyaları `eymen-uydu-kontrol/` altındadır; otomatik derleme, Java kanal gezinme ve marka gösterim testlerinden sonra imzalı APK'yı Releases'e yayımlar.

### Kurulum uyarısı

Önceki sürüm başka anahtarla imzalandıysa Android güncellemeyi reddedebilir; uygulamayı kaldırmadan önce favori ve kanal düzenlerinizi yedeklemeyi değerlendirin. EYMENkids veya sistem TV uygulamasını kaldırmayın.
