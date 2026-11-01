# EYMEN TV — herkese açık APK yayınlama

EYMEN TV kaynak kodu **Private** kalır. Atasoy Yazılım sitesi yalnızca **Public** `mmustafatasoy/atasoyyazilim-downloads` deposundaki APK dosyalarını listeler.

### Bir kez yapılacak güvenli bağlantı

1. GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens.
2. **Only select repositories:** `atasoyyazilim-downloads`. **Contents: Read and write**. Süre sınırı belirleyin.
3. Özel `EymenTv` deposunda Settings → Secrets and variables → Actions → New repository secret yolunda adı **ATASOY_DOWNLOADS_TOKEN** olacak şekilde token'ı kaydedin.
4. `EymenTv` → Actions → **Mirror EYMEN TV APK files to public Atasoy Downloads** → Run workflow → main.

Token'ı sohbet, kaynak kod veya web sayfasında paylaşmayın.

### Nasıl çalışır?

- Özel depodaki **v0.4–v0.12** mevcut GitHub Releases dosyalarını kendi deposunun otomatik `GITHUB_TOKEN` yetkisiyle indirir.
- Sadece **.apk** dosyaları ve her APK için `SHA256SUMS.txt` dosyası dışarı aktarılır. Kaynak kod, ZIP proje paketi, özel depo URL'si veya kimlik bilgisi yayımlanmaz.
- Her sürüm, public depoda `eymen-tv-v0.12`, `eymen-tv-v0.11` vb. etiketlerle ayrı yayımlanır; daha önce yayımlanmış dosyalar korunur.
- Kullanıcılara `https://atasoyyazilim.com/indir/eymen-tv/` adresinden güncel ve eski sürüm düğmeleri otomatik sunulur.
- Kaynak APK'ların kalıcı üretim imzası doğrulanmadığı için yayınlar **prerelease / test** olarak işaretlenir.
- Çalışma tamamlandığında `https://github.com/mmustafatasoy/atasoyyazilim-downloads/releases` ekranında dosya adlarını ve gerçek boyutlarını kontrol edin.

Yeni sürümler yayımlandığında iş akışı yeni sürümleri içerecek şekilde güncellenmelidir; mevcut iş akışı yalnızca v0.4–v0.12 aralığını aktarır.
