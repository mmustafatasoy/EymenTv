from pathlib import Path
p=Path(__file__).resolve().parents[1]
s=(p/'src/com/eymen/uydukontrol/PlayerActivity.java').read_text(encoding='utf-8')
assert 'android:versionName="0.10"' in (p/'AndroidManifest.xml').read_text(encoding='utf-8')
must=['setMultiChoiceItems(labels,checked','if(checked[i])ids.add(String.valueOf(candidates.get(i).id))',
      'hidden.addAll(ids);','setTitle(ids.size()+" kanal silinsin mi?")',
      'Toplu kanal sil (işaretleyerek)','Silinen kanalları geri getir',
      'getAssets().open("brand_3d.png")','spinMs*Math.PI*2/14000.0',
      'c.drawBitmap(brandLogo','PROMO_INTERVAL_MS=3600000L',
      'for(int k=0;k<10&&start+k<visible.size();k++)','PageNav.jump(selected,visible.size()']
for item in must:assert item in s,item
f=p/'assets/brand_3d.png'
assert f.exists() and f.stat().st_size>2500,'logo not bundled'
assert f.read_bytes()[:4]==b'\x89PNG','invalid logo'
print('PASS: checked multi channel selection, double confirmation, rotating brand, old menu and advert preserved')
