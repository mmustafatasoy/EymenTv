# EYMEN TV 0.4 — Uydu kanalları / özel logolar

Bu sürüm, kullanıcıya ait 0.3 kaynak sürümü üzerinden güncellenmiştir. **Android TV / Whale OS cihaz denemesi** içindir; gerçek DTVKit sesi ve dosya seçicisi henüz fiziksel TV'de doğrulanmamıştır.

## Kumanda davranışı

- **OK:** Kanallar görünmüyorsa kanal listesini açar. Listede seçili kanalı oynatır.
- **Sağ / Sol:** Kanal listesi açıkken **10'lu sayfalar** arasında dolaşır. Örnek: 1–10 → 11–20 → 21–30. Son sayfa eksik kalabilir; sınır aşılmaz.
- **Yukarı / Aşağı:** Görünen on kanal arasında tek tek dolaşır.
- **MENU:** Uygulama ayarları. MENU tuşu olmayan kumandada kanal listesinin altındaki **⚙ Uygulama ayarları** satırına gelip OK.
- **Ayarlar → Seçili kanalı düzenle / logo / sil:** Kanalı yeniden adlandır, kendine ait PNG/JPG logo yükle, özel logoyu kaldır, sırada taşı veya uygulama listesinden sil.
- **CH+ / CH−:** Yayın izlerken bir kanal ileri/geri.

Sistem kanal veritabanına yazma/silme işlemi yapılmaz. Kanallar yalnızca **EYMEN TV içindeki yerel listeden gizlenir**; uygulama ayarlarında “Silinen kanalları geri getir” bulunur.

## Logoyu TV'den seçme

1. PNG/JPG logoyu USB belleğe kopyala veya TV'de dosya seçicinin erişebildiği bir klasöre koy.
2. Kanal listesini aç, ilgili kanalı seç.
3. **⚙ Uygulama ayarları → Seçili kanalı düzenle / logo / sil → Kendi logomu yükle (PNG/JPG)**.
4. Dosya seçiciden resmi seç. Uygulama resmi (maks. yaklaşık 512 px) kendi özel depolama alanına kopyalar. İkinci kez dosyayı seçmek gerekmez; internet gerekmez.
5. **Özel logoyu kaldır** ile otomatik eşleşen paket/TV kanal logosuna geri dönebilirsin.

Whale TV'nin dosya seçici sağlamadığı cihazlarda dosya açma penceresi çalışmayabilir. Bu durumda `assets/logos/` klasöründe istediğin yayıncının PNG dosyasını değiştirip APK'yı yeniden derlemek mümkündür. Kanal adı/slug ile logo eşleştirme `PlayerActivity.logoName` içindedir.

Öncelik sırası: **kullanıcının seçtiği logo → APK içindeki eşleşen 52 logo → TV kanal logosu → yazılı kart**.

Kanal adı, sıra, favori, gizleme ve özel logo verileri APK sürümü güncellendiğinde aynı uygulama paketi ve imzayla kurulursa korunur. Uydu kanalları yeniden taranır ve TV sağlayıcısının kanal ID'leri değişirse eski özel logoların yeni kanallara tekrar atanması gerekebilir.

## Animasyon ve saat

Sağ üstte renkli EYMEN TV hareketli logosu ve altında 30 saniyede bir harf harf yazılan **atasoyyazılım.com** bulunur. Eski yazılım adı kullanıcıya gösterilmez. Saat/tarih ekranın üst köşesinde değil **kanal bilgi panelindedir**.

## Derleme / APK

Android SDK 33 (platforms;android-33), build-tools 33.0.2, JDK 17 gerektirir. `build.sh` kaynakları derler; ayrıca ZIP kökünde GitHub Actions iş akışı bulunur.

```bash
cd eymen-uydu-kontrol
export TV_ANDROID_JAR="$ANDROID_HOME/platforms/android-33/android.jar"
export TV_BUILD_TOOLS="$ANDROID_HOME/build-tools/33.0.2"
bash build.sh
```

Üretilen APK: `build/EYMEN-TV-0.4.apk`.

- `versionCode=5`, `versionName=0.4`, paket `com.eymen.uydukontrol`. 0.3 üzeri uygulama güncellemesi olarak tasarlandı.
- Önceki geliştirici test imzası `signing/development.jks` **yalnızca test** için korunmuştur; bu anahtarı üretim ortamında veya kamusal depoda kullanmayın. GitHub deposuna yüklemeden önce depo görünürlüğü ve imza stratejisini kontrol edin.
- Fiziksel TV'de DTVKit sesinin çalıştığı **henüz doğrulanmadı**. Var olan ses odağı/iz seçme tanılama kodu değişmeden korunmuştur. Mavi tuş → Ses tanılama menüsüyle test edilebilir.
- Bu ortamda Android SDK bulunmadığı için **0.4 APK üretimi burada doğrulanamadı**. Paket kaynak kodudur; “uygulama TV'de test edildi” iddiası değildir.

## Statik test

```bash
mkdir -p /tmp/eymen-tests
javac -d /tmp/eymen-tests src/com/eymen/uydukontrol/PageNav.java tests/PageNavTest.java
java -cp /tmp/eymen-tests PageNavTest
```
