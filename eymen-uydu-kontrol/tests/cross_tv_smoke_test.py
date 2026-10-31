from pathlib import Path
p=Path(__file__).resolve().parents[1]
s=(p/'src/com/eymen/uydukontrol/PlayerActivity.java').read_text(encoding='utf-8')
manifest=(p/'AndroidManifest.xml').read_text(encoding='utf-8')
must=[
 'boolean dtvkitFound=false;',
 'if(!dtvkitFound||ch.input.startsWith("com.droidlogic.dtvkit.inputsource/"))result.add(ch)',
 'String input=c.getString(3)',
 'raw.add(x);',
 'TvInputManager manager=(TvInputManager)getSystemService(TV_INPUT_SERVICE);',
 'Kanal erişimi yok:',
 'TV kanal erişim izni reddedildi',
 'TV kanal sağlayıcısı yanıt vermedi',
 'result.sort((a,b)->',
]
for token in must: assert token in s,token
assert 'if(input!=null&&input.startsWith("com.droidlogic.dtvkit.inputsource/"))' not in s
assert 'android:versionCode="11" android:versionName="0.10"' in manifest
for preserved in [
 'PageNav.jump(selected,visible.size()',
 'for(int k=0;k<10&&start+k<visible.size();k++)',
 'private void drawPromo(Canvas c)',
 'PROMO_INTERVAL_MS=3600000L',
 'Reklamı şimdi göster (test)',
]:
 assert preserved in s,"Menu/reklam unexpectedly changed "+preserved
# Model the exact fallback policy with independent sample vendor input IDs.
def scan(inputs):
 dtv=any(x.startswith('com.droidlogic.dtvkit.inputsource/') for x in inputs)
 return [x for x in inputs if not dtv or x.startswith('com.droidlogic.dtvkit.inputsource/')]
w='com.droidlogic.dtvkit.inputsource/.TvInputService'
v='com.vestel.liveinput/.TunerInputService'
assert scan([w,v])==[w]
assert scan([v])==[v]
assert scan([])==[]
print('PASS: DTVKit prioritized on Whale, fallback on Vestel, menu and promo preserved')
