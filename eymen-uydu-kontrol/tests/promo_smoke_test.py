from pathlib import Path
p=Path(__file__).resolve().parents[1]
s=(p/'src/com/eymen/uydukontrol/PlayerActivity.java').read_text(encoding='utf-8')
manifest=(p/'AndroidManifest.xml').read_text(encoding='utf-8')
checks={
 'one-hour corner brand interval':'BRAND_INTERVAL_MS=3600000L',
 '15-second appearance':'BRAND_DURATION_MS=15000L',
 'brand only when TV channel is playing':'if(current==null||menu)return;',
 'corner branding obeys visible program':'brandStartedUptime>0&&current!=null&&!menu',
 'right-side placement':'c.translate(1137f,80f)',
 '3D rotating icon':'spinMs*Math.PI*2/14000.0',
 'packaged logo':'getAssets().open("brand_3d.png")',
 'website text fallback':'www.atasoyyazilim.com',
 'last appearance persisted':'prefs.edit().putLong(BRAND_LAST_KEY',
 'old timer reused on update':'BRAND_LAST_KEY="promo_last_shown_epoch"',
 'first display only after one hour':'if(lastBrand<=0||lastBrand>System.currentTimeMillis())',
 'manual logo preview':'Sağ üst logo ve web adresini göster (test)',
 'new version':'android:versionName="0.11"'
}
for name,token in checks.items():
 assert token in (manifest if name=='new version' else s), name
for forbidden in [
 'drawPromo(', 'promoFamily', 'promoLogo', 'promo_family.jpg', 'promo_logo.jpg',
 'promoTyping(', 'promoBar(', 'promoLine(',
 'Reklamı şimdi göster (test)', 'İletişim: 0531 221 89 65', 'MUHAMMET ATASOY',
 'PROMO_INTERVAL_MS', 'if(promoStartedUptime>0)drawPromo(c)',
]:
 assert forbidden not in s, 'Removed advertisement still in player: '+forbidden
for binary in ['promo_family.jpg','promo_logo.jpg']:
 assert not (p/'assets'/binary).exists(), 'Unwanted ad asset still bundled: '+binary
print('PASS: large ad removed, right corner EYMEN logo+website appears hourly for 15 seconds:',len(checks),'checks')
