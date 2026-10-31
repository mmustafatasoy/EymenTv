from pathlib import Path
base = Path(__file__).resolve().parent.parent
source = (base / 'src/com/eymen/uydukontrol/PlayerActivity.java').read_text()
manifest = (base / 'AndroidManifest.xml').read_text()
assert 'android:versionCode="5"' in manifest and 'android:versionName="0.4"' in manifest
for item in ('REQUEST_CHANNEL_LOGO','Intent.ACTION_OPEN_DOCUMENT','channel_logos','customLogoFile',
             'resetChannelLogo','chooseChannelLogo','PageNav.jump',
             'Kendi logomu yükle (PNG/JPG)','Uygulama ayarları',
             'atasoyyazılım.com','%30000','for(int k=0;k<10'):
    assert item in source, f'Feature not found: {item}'
assert 'gıppık yazılım' not in source.lower()
assert 'new String[]{"Adını değiştir","Kendi logomu yükle (PNG/JPG)"' in source
print('PASS: v0.4 features, file picker, 10-item menu, 30-second branding, manifest')
