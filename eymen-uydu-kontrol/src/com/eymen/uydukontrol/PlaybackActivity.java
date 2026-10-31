package com.eymen.uydukontrol;
import android.app.*;import android.os.*;import android.content.pm.PackageManager;import android.database.Cursor;import android.media.tv.*;import android.graphics.Color;import android.view.*;import android.widget.*;import java.util.*;
public class PlaybackActivity extends Activity {
 private TvView tv; private TextView status; private ListView list; private final ArrayList<Channel> channels=new ArrayList<>(); private final Handler h=new Handler(); private int generation=0; private boolean alive=true;
 static class Channel {long id;String number,name,input; public String toString(){return number+" • "+name;} }
 private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
 public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(24),dp(16),dp(24),dp(16));root.setBackgroundColor(Color.rgb(12,20,35));
 TextView title=new TextView(this);title.setText("EYMEN • Canlı uydu testi 1.1");title.setTextSize(24);title.setTextColor(Color.CYAN);root.addView(title);
 TextView hint=new TextView(this);hint.setText("Kanal seç + OK: yayını dene  |  Geri: testi kapat\nKanal listesi değiştirilmez. Görüntü ve sesi ayrıca kontrol et.");root.addView(hint);
 LinearLayout content=new LinearLayout(this);list=new ListView(this);content.addView(list,new LinearLayout.LayoutParams(dp(260),-1));tv=new TvView(this);tv.setFocusable(false);content.addView(tv,new LinearLayout.LayoutParams(0,-1,1));root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
 status=new TextView(this);status.setTextSize(17);status.setTextColor(Color.WHITE);status.setMinHeight(dp(70));root.addView(status);setContentView(root);
 list.setOnItemClickListener((p,v,pos,id)->tune(channels.get(pos)));
 if(checkSelfPermission("android.permission.READ_TV_LISTINGS")!=PackageManager.PERMISSION_GRANTED){status.setText("Kanal listesi izni gerekli.");requestPermissions(new String[]{"android.permission.READ_TV_LISTINGS"},42);}else load();
 }
 public void onRequestPermissionsResult(int r,String[] p,int[] results){super.onRequestPermissionsResult(r,p,results);if(r==42&&checkSelfPermission("android.permission.READ_TV_LISTINGS")==PackageManager.PERMISSION_GRANTED)load();else status.setText("İzin verilmedi. Ayarlar → Uygulamalar → EYMEN Uydu Kontrol → İzinler bölümünü kontrol et.");}
 private void load(){status.setText("DTVKit kanalları okunuyor…");new Thread(()->{ArrayList<Channel> found=new ArrayList<>();String error=null;
 try(Cursor c=getContentResolver().query(TvContract.Channels.CONTENT_URI,new String[]{"_id","display_number","display_name","input_id"},null,null,null)){
 if(c!=null)while(c.moveToNext()){String input=c.getString(3);if(input!=null&&input.startsWith("com.droidlogic.dtvkit.inputsource/")){Channel ch=new Channel();ch.id=c.getLong(0);ch.number=c.getString(1);ch.name=c.getString(2);ch.input=input;found.add(ch);}}
 found.sort((a,b)->{try{return Integer.compare(Integer.parseInt(a.number),Integer.parseInt(b.number));}catch(Exception e){return String.valueOf(a.number).compareTo(String.valueOf(b.number));}});
 }catch(Exception e){error=e.getClass().getSimpleName()+": "+e.getMessage();}
 final String err=error;h.post(()->{if(!alive)return;channels.clear();channels.addAll(found);list.setAdapter(new ArrayAdapter<Channel>(this,android.R.layout.simple_list_item_1,channels));status.setText(err!=null?err:"DTVKit kaydı: "+channels.size()+" • Önce TRT 1 HD'yi seçip OK'ye bas.");list.requestFocus();});}).start();}
 private void tune(Channel ch){final int token=++generation;tv.reset();status.setText(ch+" [id:"+ch.id+"]\nBağlantı deneniyor…");
 tv.setCallback(new TvView.TvInputCallback(){
  private void msg(String m){if(token==generation&&alive)status.setText(ch+" [id:"+ch.id+"]\n"+m);}
  public void onVideoAvailable(String id){h.removeCallbacksAndMessages(null);msg("GÖRÜNTÜ HAZIR bildirimi alındı. Ekranda görüntü ve ses var mı?");}
  public void onVideoUnavailable(String id,int reason){String[] names={"Bilinmeyen neden","Kanal ayarlanıyor","Sinyal zayıf","Tamponlanıyor","Yalnızca ses"};msg("Görüntü hazır değil: "+(reason>=0&&reason<names.length?names[reason]:"Neden kodu "+reason));}
  public void onConnectionFailed(String id){h.removeCallbacksAndMessages(null);msg("BAĞLANTI BAŞARISIZ • TV giriş servisi oturum açamadı.");}
  public void onDisconnected(String id){msg("TV giriş bağlantısı kesildi.");}
  public void onContentBlocked(String id,TvContentRating rating){h.removeCallbacksAndMessages(null);msg("İçerik TV'nin ebeveyn denetimi tarafından engellendi.");}
 });
 try{tv.tune(ch.input,TvContract.buildChannelUri(ch.id));tv.setStreamVolume(1f);h.postDelayed(()->{if(token==generation&&alive)status.append("\n20 saniye geçti; görüntü yoksa bu ekranın fotoğrafını gönder.");},20000);}catch(Exception e){status.setText(ch+"\n"+e.getClass().getSimpleName()+": "+e.getMessage());}
 }
 protected void onStop(){generation++;h.removeCallbacksAndMessages(null);if(tv!=null)tv.reset();status.setText("Test durduruldu. Devam etmek için kanalı tekrar seç.");super.onStop();}
 protected void onDestroy(){alive=false;h.removeCallbacksAndMessages(null);super.onDestroy();}
}
