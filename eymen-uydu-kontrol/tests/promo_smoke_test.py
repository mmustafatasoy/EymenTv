from pathlib import Path
p = Path(__file__).resolve().parents[1]
s = (p / 'src/com/eymen/uydukontrol/PlayerActivity.java').read_text(encoding='utf-8')
manifest = (p / 'AndroidManifest.xml').read_text(encoding='utf-8')
checks = {
    'one-hour interval': 'BRAND_INTERVAL_MS=3600000L',
    '15-second top display': 'BRAND_DURATION_MS=15000L',
    'brand last shown stored': 'hourly_brand_last_shown_epoch',
    'branding only while channel is open': 'if(current==null||menu)return;',
    'time persists': 'prefs.edit().putLong(BRAND_LAST_KEY',
    'upper branding': 'private void drawHourlyBrand(Canvas c)',
    'top not lower': 'c.drawRoundRect(348,22,987,119,21,21,p)',
    'website address top only': 'String url="www.atasoyyazilim.com";',
    'right logo decoded': 'getAssets().open("brand_right.png")',
    'right logo continuously drawn': 'c.drawBitmap(brandLogo,null,new RectF(-113,-58,113,58),p)',
    'menu preview': 'Saatlik logo/web göster (test)',
    'preview does not reset timer': 'if(!preview)prefs.edit().putLong(BRAND_LAST_KEY',
    'hourly top overlay drawing': 'if(brandStartedUptime>0)drawHourlyBrand(c)',
    'version': 'android:versionName="0.11"'
}
for name, text in checks.items():
    assert text in (manifest if name == 'version' else s), name
for forbidden in (
    'promo_family.jpg','promo_logo.jpg','promoFamily','promoLogo',
    'drawPromo(', 'promoBar(', 'promoLine(','PROMO_LAST_KEY',
    'Reklamı şimdi göster (test)','0531 221 89 65',
    'MUHAMMET ATASOY','promo_last_shown_epoch'
):
    assert forbidden not in s, 'obsolete advertisement found: ' + forbidden
for file in ('promo_family.jpg', 'promo_logo.jpg'):
    assert not (p / 'assets' / file).exists(), 'old advertisement asset still bundled: ' + file
assert 'getAssets().open("brand_right.png")' in s
print('PASS: no TV lower-third advertisement, right logo remains, top web/logo hourly for 15 seconds')
