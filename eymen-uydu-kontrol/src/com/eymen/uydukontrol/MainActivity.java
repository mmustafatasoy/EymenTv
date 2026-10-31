package com.eymen.uydukontrol;

import android.app.Activity;
import android.os.*;
import android.content.pm.*;
import android.database.Cursor;
import android.media.tv.*;
import android.net.Uri;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private TextView body, heading, footer;
 private Button previous, next, scan;
 private final ArrayList<String> pages = new ArrayList<>();
 private final ArrayList<String> titles = new ArrayList<>();
 private final ExecutorService worker = Executors.newSingleThreadExecutor();
 private final Handler handler = new Handler(Looper.getMainLooper());
 private volatile boolean closed=false;
 private int page=0;
 private String key="Henüz tuşa basılmadı";
 private int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density); }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
  LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(38),dp(18),dp(38),dp(14)); root.setBackgroundColor(Color.rgb(12,20,35));
  heading=new TextView(this); heading.setTextSize(25); heading.setTextColor(Color.rgb(75,218,214)); root.addView(heading);
  TextView note=new TextView(this); note.setText("EYMEN • Salt okunur uydu tanılama / v1.1"); note.setTextSize(14); root.addView(note); Button play=new Button(this);play.setText("CANLI UYDU TESTİNİ AÇ");root.addView(play);play.setOnClickListener(v->startActivity(new android.content.Intent(this,PlaybackActivity.class)));
  ScrollView scroll=new ScrollView(this); body=new TextView(this); body.setTextSize(18); body.setTextColor(Color.WHITE); body.setPadding(0,dp(12),0,dp(12)); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout controls=new LinearLayout(this);
  previous=button("◀ Önceki",controls); scan=button("Yeniden kontrol et",controls); next=button("Sonraki ▶",controls);
  root.addView(controls); footer=new TextView(this); footer.setTextSize(13); root.addView(footer); setContentView(root);
  previous.setOnClickListener(v->{if(page>0){page--;show();scroll.scrollTo(0,0);}});
  next.setOnClickListener(v->{if(page+1<pages.size()){page++;show();scroll.scrollTo(0,0);}});
  scan.setOnClickListener(v->startScan()); startScan();
 }
 private Button button(String label,LinearLayout parent){ Button b=new Button(this);b.setText(label);b.setTextSize(15); parent.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));return b; }
 private void show(){heading.setText(titles.get(page));body.setText(pages.get(page));previous.setEnabled(page>0);next.setEnabled(page+1<pages.size());footer.setText("Sayfa "+(page+1)+" / "+pages.size()+"   •   Kumanda: "+key);}
 private void add(ArrayList<String> ts,ArrayList<String> ps,String title,String text){
  String[] lines=text.split("\n");StringBuilder part=new StringBuilder(); int n=0;
  for(String line:lines){
   if(n==8){ts.add(title);ps.add(part.toString());part.setLength(0);n=0;}
   part.append(line).append('\n');n++;
  }
  if(part.length()>0){ts.add(title);ps.add(part.toString());}
 }
 private String error(Exception e){String m=e.getMessage(); return e.getClass().getSimpleName()+": "+(m==null?"Açıklama yok":m.substring(0,Math.min(m.length(),240)));}
 private void startScan(){
  scan.setEnabled(false);previous.setEnabled(false);next.setEnabled(false);heading.setText("Uydu erişimi kontrol ediliyor…");body.setText("Kanal ve program verileri yalnızca okunur.\nTV ayarları ve kayıtlı kanallar değiştirilmez.\nKontrol genellikle birkaç saniye sürer.");
  worker.execute(()->{
   ArrayList<String> ts=new ArrayList<>(),ps=new ArrayList<>();
   StringBuilder dev=new StringBuilder();
   dev.append("Marka: ").append(Build.MANUFACTURER).append(" / ").append(Build.BRAND).append("\nModel: ").append(Build.MODEL).append("\nCihaz: ").append(Build.DEVICE).append("\nAndroid: ").append(Build.VERSION.RELEASE).append(" • API ").append(Build.VERSION.SDK_INT).append("\nYazılım: ").append(Build.DISPLAY).append("\nTV listesi izni: ").append(checkSelfPermission("android.permission.READ_TV_LISTINGS")==PackageManager.PERMISSION_GRANTED?"VERİLMİŞ":"VERİLMEMİŞ");
   add(ts,ps,"1 • Cihaz ve izinler",dev.toString());
   StringBuilder inputs=new StringBuilder();
   try{TvInputManager manager=(TvInputManager)getSystemService(TV_INPUT_SERVICE);
    if(manager==null)inputs.append("TV giriş hizmeti bulunamadı.");
    else{List<TvInputInfo> list=manager.getTvInputList(); inputs.append("Görünen TV girişi: ").append(list.size()).append('\n');
     for(TvInputInfo info:list){inputs.append(info.loadLabel(this)).append(" • tür: ").append(info.getType()==TvInputInfo.TYPE_TUNER?"TUNER":"HDMI / diğer ("+info.getType()+")").append('\n').append(info.getId()).append('\n');}
     if(list.isEmpty())inputs.append("Giriş görünmemesi, TV'de uydu alıcısı yok demek değildir.");}
   }catch(Exception e){inputs.append(error(e));}
   add(ts,ps,"2 • TV girişleri",inputs.toString());
   add(ts,ps,"3 • Kanal okuma",query(TvContract.Channels.CONTENT_URI,new String[]{"_id","display_number","display_name","input_id"},false));
   add(ts,ps,"4 • Yayın rehberi (EPG)",query(TvContract.Programs.CONTENT_URI,new String[]{"title","channel_id","start_time_utc_millis"},true));
   StringBuilder apps=new StringBuilder();
   try{PackageManager pm=getPackageManager();List<ApplicationInfo> all=pm.getInstalledApplications(0);int count=0;
    for(ApplicationInfo a:all){String label=String.valueOf(pm.getApplicationLabel(a));String s=(label+" "+a.packageName).toLowerCase(Locale.ROOT);
     if(s.contains("uydu")||s.contains("satellite")||s.contains("livetv")||s.contains("live tv")||s.contains("dtv")||s.contains("dvb")||s.contains("tuner")||s.contains("mediatek")||s.contains("mstar")||s.contains("tvinput")||s.contains("amlogic")||label.equalsIgnoreCase("TV")){
      apps.append(label).append('\n').append(a.packageName).append('\n');count++;
     }
    } if(count==0)apps.append("Adıyla eşleşen uygulama bulunamadı. Üretici farklı ad kullanabilir.");
   }catch(Exception e){apps.append(error(e));}
   add(ts,ps,"5 • Uydu / TV uygulama adayları",apps.toString());
   add(ts,ps,"6 • Sonuç nasıl okunur?","Kanal ve EPG sonuçları yalnızca bu APK'nın erişimini gösterir.\nSıfır kayıt: görünür veri yok; uydu kanalları yok demek değil.\nSecurityException: erişim bu uygulama için reddedildi.\nTV girişi bulunması canlı görüntü izni verildiğini kanıtlamaz.\nBu sürüm canlı yayını açmaz; oynatma testi yapılmadı.\nUygulama adayları ad eşleşmeleridir, kesin tespit değildir.\nÖnce 1–4 bölümlerinin, sonra Uydu adayının fotoğrafını gönder.\nVeriler gönderilmez; internet ve depolama izni yoktur.");
   handler.post(()->{if(closed)return;titles.clear();titles.addAll(ts);pages.clear();pages.addAll(ps);page=0;scan.setEnabled(true);show();next.requestFocus();});
  });
 }
 private String query(Uri uri,String[] columns,boolean programs){
  CancellationSignal cancel=new CancellationSignal();Runnable timeout=()->cancel.cancel();handler.postDelayed(timeout,8000);
  try(Cursor c=getContentResolver().query(uri,columns,null,null,null,cancel)){
   if(c==null)return "Sağlayıcı boş yanıt verdi; sonuç doğrulanamadı.";
   StringBuilder b=new StringBuilder("Sorgu başarılı • görünür kayıt: "+c.getCount()+"\n");int n=0;
   while(c.moveToNext()&&n++<8){
    if(programs)b.append(c.getString(0)).append(" • kanal ").append(c.getLong(1)).append('\n');
    else b.append(c.getString(1)).append(" — ").append(c.getString(2)).append(" [id:").append(c.getLong(0)).append("]\n").append(c.getString(3)).append('\n');
   }
   if(c.getCount()>8)b.append("Yalnızca ilk 8 kayıt örnek olarak gösterildi.\n");
   if(c.getCount()==0)b.append("Veriler gizli olabilir veya üreticinin farklı veritabanında tutulabilir.");
   return b.toString();
  }catch(SecurityException e){return "ERİŞİM REDDEDİLDİ\n"+error(e);}
   catch(OperationCanceledException e){return "ZAMAN AŞIMI: 8 saniye içinde yanıt alınamadı.";}
   catch(Exception e){return "OKUNAMADI\n"+error(e);}
   finally{handler.removeCallbacks(timeout);}
 }
 @Override public boolean dispatchKeyEvent(KeyEvent event){if(event.getAction()==KeyEvent.ACTION_DOWN){key=KeyEvent.keyCodeToString(event.getKeyCode());if(!pages.isEmpty())footer.setText("Sayfa "+(page+1)+" / "+pages.size()+"   •   Kumanda: "+key);}return super.dispatchKeyEvent(event);}
 @Override protected void onDestroy(){closed=true;worker.shutdownNow();super.onDestroy();}
}
