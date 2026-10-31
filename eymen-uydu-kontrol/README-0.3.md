# EYMEN TV 0.3

0.2 üzerine aynı imzayla güncelleme; versionCode 4.

- Listede 10 kanal görünür. Sağ: sonraki 10; sol: önceki 10. İlk/son sayfada durur.
- Yukarı/aşağı satırlar arasında; ilk satırdan yukarı ile kategori odağı,
  OK ile Tüm Kanallar/Favoriler geçişi. Son satırdan aşağı ile Ayarlar düğmesi.
- Listenin altında Uygulama ayarları; kumandanın MENU tuşuyla da aynı ekran.
- Seçili kanalın adı değiştirilebilir, birer sıra taşınabilir, uygulama
  listesinden onayla silinebilir. Asıl uydu veritabanı değişmez. Silinenler
  Ayarlar'dan topluca geri getirilir. Adlar ve sıra uygulama yeniden açılınca korunur.
- Sağ üst EYMEN TV renkli geçiş ve hafif ölçek hareketi kullanır.
  Altında atasoyyazılım.com harf harf renkli yazılır; 30 saniyede tekrar başlar,
  yazım bittikten sonra bir sonraki döngüye kadar görünür kalır.
- Saat/tarih sadece kanal bilgisi panelinde kalır.
- 52 yayıncı logosu APK assets/logos altında paketlendi. Kanal adı normalleştirilerek
  eşleştirilir; eşleşmezse cihaz logosu, o da yoksa isim kartı kullanılır.
  Kaynaklar assets/LOGO-SOURCES.txt dosyasında. Yayıncılarla bağlantı yoktur.
- Önceki ses odağı ve ses izi seçim düzeltmeleri korunmuştur. Cihazda ses
  sorununun çözülmüş olduğu henüz doğrulanmadı; Mavi / ses menüsüyle test edilir.

Kontrol: Java/API ve DEX derleme, v2/v3 APK imzası, zipalign; önceki sürümle
imza eşleşmesi; APK içindeki 52 PNG'nin geçerliliği. PageNavTest boş/eksik
sayfalar ve 590 kayda kadar ilk/son sayfa sınırlarını doğrular.
Fiziksel TV/emülatör UI testi yapılmadı. Renkli yazı ve 10 satırın okunaklılığı
TV ekranında kontrol edilmelidir. Bu sürüm gerçek zamanlı blur içermez.
