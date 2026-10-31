from pathlib import Path

p=Path(__file__).resolve().parents[1]
s=(p/'src/com/eymen/uydukontrol/PlayerActivity.java').read_text(encoding='utf-8')
manifest=(p/'AndroidManifest.xml').read_text(encoding='utf-8')
tests={
 'one-hour schedule':'PROMO_INTERVAL_MS=3600000L',
 '15-second display':'PROMO_DURATION_MS=15000L',
 'shown after picture':'promoTyping(c,"www.atasoyyazılım.com"',
 'phone':'0531 221 89 65',
 'name':'MUHAMMET ATASOY',
 'logo last':'entrance(elapsed,8200,1300)',
 'visible only while viewing':'if(current==null||menu)return;',
 'persist last shown':'prefs.edit().putLong(PROMO_LAST_KEY',
 'preview menu':'Reklamı şimdi göster (test)',
 'photo bundled':'promo_family.jpg',
 'display at end of overlay':'if(promoStartedUptime>0)drawPromo(c)',
 'version 0.5':'android:versionName="0.5"'
}
for name,token in tests.items():
 assert token in (manifest if name=='version 0.5' else s),name
assert (p/'assets/promo_family.jpg').exists(), 'promo photo missing'
print('Hourly lower-third promo smoke test passed:',len(tests),'checks')
