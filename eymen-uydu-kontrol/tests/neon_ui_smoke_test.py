"""Regression checks for the real Android TV neon UI, not marketing images."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=(root/'src/com/eymen/uydukontrol/PlayerActivity.java').read_text(encoding='utf-8')
manifest=(root/'AndroidManifest.xml').read_text(encoding='utf-8')
build=(root/'build.sh').read_text(encoding='utf-8')
must=[
 'class Overlay extends View',
 'drawHeader(Canvas c)',
 'drawMenu(Canvas c)',
 'drawProgram(Canvas c)',
 'drawPermanentWatermark(Canvas c)',
 'drawHourlyLogo(Canvas c)',
 'new LinearGradient(',
 '0xffff3c9b',
 '0xff19bfff',
 '▣  KANAL',
 'KANAL LİSTESİ',
 'for(int k=0;k<10&&start+k<visible.size();k++)',
 'PageNav.jump(selected,visible.size()',
 'new LinearLayout(this)',
 'private void neonMenu(',
 'EYMEN TV • KANAL YÖNETİMİ',
 'EYMEN TV • GENEL AYARLAR',
 'setOnItemClickListener(',
 'state_focused',
 'Toplu kanal sil (işaretleyerek)',
 'Silinen kanalları geri getir',
 'channelLogos',
 'showEpg',
 'showWatermark',
 'hourlyBrand',
 'BRAND_INTERVAL_MS=3600000L',
 'BRAND_DURATION_MS=15000L',
 'brandStartedUptime>0&&current!=null&&!menu',
 'getAssets().open("brand_3d.png")',
 'c.drawBitmap(brandLogo',
 'TvContract.Channels.CONTENT_URI',
 'tv.tune(c.input,TvContract.buildChannelUri(c.id))',
 'soundTracks.get(0)',
 'drawPermanentWatermark(c)',
 'drawProgram(c)'
]
for term in must:
    assert term in src, "Missing real UI or functional behavior: "+term
for forbidden in ['promoFamily','promoLogo','drawPromo(','promo_family.jpg','promo_logo.jpg']:
    assert forbidden not in src,"Obsolete ad still present: "+forbidden
assert 'android:versionName="0.12"' in manifest
assert 'EYMEN-TV-0.12.apk' in build
print("PASS: true neon Android canvas layout, functional settings and preserved TV controls")
