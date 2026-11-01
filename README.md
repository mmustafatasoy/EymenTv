# EYMEN TV v0.12 — Neon Android TV Arayüzü

Bu sürüm, önceki tanıtım görsellerine yakın **gerçek uygulama arayüzü** sunmak için Android Canvas çizimlerini değiştirir. Tanıtım görselleri birebir ekran kaydı değildir; yayın görüntüsü yalnızca televizyonun izin verdiği gerçek tuner kaynağından gelir.

## Tasarım yenilikleri

- TV ekranında koyu lacivert, neon mavi ve pembe kanal listesi, seçili kanal çerçevesi, kanal logoları ve sayfa göstergesi
- Gerçek EPG bilgisi varsa mevcut program, yayın ilerleme çubuğu ve sıradaki program
- Sağ üstte kalıcı, küçük EYMEN TV metin/logosu; daha önceki dönen logo ve web adresi **saatte bir 15 saniyelik tanıtım** olarak çalışmaya devam eder
- Kumandayla gezilebilen neon koyu renkli **Kanal Yönetimi / Genel Ayarlar** menüsü
- Kanal logoları, EPG, köşe logosu ve saatlik tanıtım için çalışan aç/kapat seçenekleri; tercihler TV üzerinde saklanır
- Fotoğraflı alt reklam kaldırılmış olarak kalır

## Kanal özellikleri

10 kanallık sayfalar, sağ/sol kumanda geçişi, favoriler, kanal adını ve logosunu düzenleme, işaretleyerek toplu silme / geri getirme, mevcut ses izi seçimi ve TV Input Framework erişimi korunmuştur. İzinler TV üreticisinin kısıtlamalarına bağlıdır.

## APK sürümleri

- **v0.12**: GitHub Releases: https://github.com/mmustafatasoy/EymenTv/releases/tag/uydu-tv-v0.12
- **v0.11**: https://github.com/mmustafatasoy/EymenTv/releases/tag/uydu-tv-v0.11

Ana depo özel ise GitHub hesabıyla oturum açılması gerekir. Bu linkler yönetici içindir; halka açık uygulama kataloglarında özel proje deposu URL'si paylaşılmamalıdır.

## Test ve derleme

GitHub Actions `.github/workflows/build.yml` Android Java derleme, kanal gezinme ve yeni arayüz regresyon testlerini çalıştırır. Bunların başarılı olması fiziksel TV'de görüntü/ses/tuner davranışının doğrulandığı anlamına gelmez. Gerçek televizyon üzerinden ayrıca test gerekir.
