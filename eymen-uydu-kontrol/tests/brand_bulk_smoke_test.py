from pathlib import Path
p=Path(__file__).resolve().parents[1]
s=(p/'src/com/eymen/uydukontrol/PlayerActivity.java').read_text(encoding='utf-8')
manifest=(p/'AndroidManifest.xml').read_text(encoding='utf-8')
assert 'android:versionCode="12" android:versionName="0.11"' in manifest
must=[
 'setMultiChoiceItems(labels,checked',
 'if(checked[i])ids.add(String.valueOf(candidates.get(i).id))',
 'hidden.addAll(ids);',
 'setTitle(ids.size()+" kanal silinsin mi?")',
 'Toplu kanal sil (işaretleyerek)',
 'Silinen kanalları geri getir',
 'getAssets().open("brand_right.png")',
 'spinMs*Math.PI*2/14000.0',
 'c.drawBitmap(brandLogo',
 'BRAND_INTERVAL_MS=3600000L',
 'for(int k=0;k<10&&start+k<visible.size();k++)',
 'PageNav.jump(selected,visible.size()'
]
for item in must: assert item in s,item
svg=p/'assets/brand_right.svg'
assert svg.is_file() and '<svg' in svg.read_text(encoding='utf-8')
assert 'www.atasoyyazilim.com' not in svg.read_text(encoding='utf-8'), 'right logo contains permanently visible URL'
print('PASS: bulk channel selection, double confirmation, right logo without URL, existing menu and hourly top brand')
