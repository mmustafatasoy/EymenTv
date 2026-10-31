# EYMEN TV 0.2 — ilk arayüzlü cihaz denemesi

Bu APK, Eymen Uydu Kontrol 1.1'in güncellemesidir. Aynı paket ve geliştirme imzası,
daha yüksek versionCode=3 kullanır; görünen isim EYMEN TV olur.

## Kullanım
- OK: kanal listesini aç; listede seçilen kanalı oynat.
- Yukarı/aşağı: listede gezin. Liste kapalıyken kanal değiştir.
- Sol/sağ: tüm kanallar ve favoriler arasında geçiş.
- Yeşil: favoriye ekle/çıkar. MENU üzerinden de yapılabilir.
- Mavi: ses izleri veya ses tanılama. MENU üzerinden de yapılabilir.
- INFO: bilgi çubuğu / açıkken program ayrıntısı.
- MENU: ses, favori, şeffaflık, bilgi süresi, izinler, liste yenileme, hakkında.
- Sayı tuşları: numarayla kanal; 1,5 saniye sonra veya OK ile aç.
- Geri: menüyü kapat; tekrar Geri çıkış onayı.

Sağ üstte EYMEN TV / gıppık yazılım sabit durur. Saat ve tarih yalnızca kanal
bilgi panelindedir. Bilgi çubuğu 3/5/8 saniyede gizlenebilir.
Program adı, saatler ve sıradaki yayın cihazdaki EPG'den alınır. Örnek içerik
ve Boğaz fotoğrafı uygulamaya gömülmemiştir; arka plan gerçek uydu yayınıdır.
Kanal logoları TV sağlayıcısından okunur. Sağlayıcı logosu olmayan kanalda
isim kartı gösterilir. Logolar arka planda ve sadece ihtiyaç oldukça okunur.

## Ses çalışması
AudioFocusRequest (medya/film), odak sonucuna bağlı TvView ses seviyesi,
Türkçe ses izini tercih eden seçim, elle iz değiştirme, odak kaybında ve
arka planda oturum bırakma eklendi. Sistem ses seviyesi otomatik yükseltilmez.
Bu değişiklikler önceki sessizlik sorununu gidermek için uygulanmıştır;
TV'de sonuç henüz doğrulanmamıştır. Ses yoksa Mavi/MENU ses ekranını gönderin.

## Görsel kapsam ve sınırlar
Referansın sol cam paneli, turkuaz odak, alt program kartı ve sağ üst marka
konumu native Canvas/View ile uygulanmıştır. Gerçek zamanlı arka plan blur
bu sürümde yoktur; yarı şeffaf panel kullanılır. Font, logolar ve gerçek yayın
referans görselle piksel düzeyinde aynı değildir. Bu ilk arayüzlü sürümdür;
çoklu favori grupları ve tam zaman çizelgeli EPG henüz eklenmemiştir.

## Derleme
API 33 android.jar ve Build Tools 33.0.2 gerekir. JDK javac veya ECJ 3.33:
TV_ANDROID_JAR=/path/android.jar TV_BUILD_TOOLS=/path/build-tools/33.0.2 \
TV_ECJ_JAR=/path/ecj.jar bash build.sh

signing/development.jks yalnızca bu cihaz denemelerinin geliştirme anahtarıdır;
parolası android. Üretim/mağaza sürümü için ayrı güvenli imza planlanmalıdır.

Doğrulanan: Java/API derlemesi, DEX üretimi, APK v2/v3 imzası, zipalign,
eski 1.1 APK ile imza eşleşmesi, launcher manifest girdisi.
Fiziksel TV veya emülatör çalıştırması yapılmadı. Kullanıcı testinde menü,
kanal değişimi, favorilerin kalıcılığı, EPG, görüntü ve ses kontrol edilmeli.
