package com.eymen.uydukontrol;
import android.app.*;import android.os.*;import android.content.*;import android.content.pm.PackageManager;import android.database.Cursor;import android.graphics.*;import android.media.*;import android.media.tv.*;import android.view.*;import android.widget.*;import java.util.*;import java.text.SimpleDateFormat;import java.util.concurrent.*;import java.io.*;
public class PlayerActivity extends Activity {
 static class Channel {long id;String name,num,input,originalName;Bitmap logo;boolean logoRequested;}
 static class Program {String title,description;long start,end;}
 private TvView tv;private Overlay ui;private AudioManager audio;private AudioFocusRequest focus;private boolean hasFocus=false,active=false,dead=false,menu=true,info=true,favsOnly=false,loading=false;private int selected=0,tab=0,epoch=0;private String message="Kanallar yükleniyor…",audioState="Henüz ses izi bildirilmedi",digits="";private Channel current;private Program now,next;private final ArrayList<Channel> all=new ArrayList<>(),visible=new ArrayList<>();private final ArrayList<TvTrackInfo> soundTracks=new ArrayList<>();private final Handler h=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();private final ExecutorService logoWorker=Executors.newFixedThreadPool(2);private SharedPreferences prefs;private Set<String> favorites,hidden;private long animationStart=SystemClock.uptimeMillis();private final Locale tr=new Locale("tr","TR");private long lastEpg=0;private int opacity=185,hideSeconds=5;private boolean stoppedPlayback=false;private static final int REQUEST_CHANNEL_LOGO=504;private long pendingLogoChannelId=-1;private Bitmap promoFamily,promoLogo;private long promoStartedUptime=-1L;private static final long PROMO_INTERVAL_MS=3600000L,PROMO_DURATION_MS=15000L;private static final String PROMO_LAST_KEY="promo_last_shown_epoch";
 private final Runnable hide=()->{if(!menu){info=false;ui.invalidate();}};
 private final Runnable enterNumber=()->{String n=digits;digits="";for(Channel c:all)if(!hidden.contains(String.valueOf(c.id))&&n.equals(c.num)){play(c);return;}toast("Kanal numarası bulunamadı");};
 private final Runnable clock=new Runnable(){public void run(){if(dead)return;checkPromo();ui.invalidate();if(active&&current!=null&&System.currentTimeMillis()-lastEpg>60000)loadEpg(current);h.postDelayed(this,promoStartedUptime>0?40:((SystemClock.uptimeMillis()-animationStart)%30000<4000?80:1000));}};
 private void checkPromo(){if(!active)return;long uptime=SystemClock.uptimeMillis();if(promoStartedUptime>0){if(uptime-promoStartedUptime>=PROMO_DURATION_MS)promoStartedUptime=-1L;return;}if(current==null||menu)return;long now=System.currentTimeMillis();long last=prefs.getLong(PROMO_LAST_KEY,now);if(now-last>=PROMO_INTERVAL_MS)startPromo(false);}
 private void startPromo(boolean preview){promoStartedUptime=SystemClock.uptimeMillis();if(!preview)prefs.edit().putLong(PROMO_LAST_KEY,System.currentTimeMillis()).apply();ui.invalidate();}
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("eymen_tv",MODE_PRIVATE);favorites=new HashSet<>(prefs.getStringSet("favorites",new HashSet<String>()));hidden=new HashSet<>(prefs.getStringSet("hidden",new HashSet<String>()));opacity=prefs.getInt("opacity",185);hideSeconds=prefs.getInt("hideSeconds",5);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().getDecorView().setSystemUiVisibility(5894);setVolumeControlStream(AudioManager.STREAM_MUSIC);
 audio=(AudioManager)getSystemService(AUDIO_SERVICE);focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()).setOnAudioFocusChangeListener(change->{hasFocus=change==AudioManager.AUDIOFOCUS_GAIN;if(tv!=null)tv.setStreamVolume(hasFocus?1f:0f);if(change==AudioManager.AUDIOFOCUS_LOSS){tv.reset();stoppedPlayback=true;message="Ses odağı başka uygulamaya geçti; OK ile yeniden aç.";ui.invalidate();}},h).build();
 FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);tv=new TvView(this);tv.setFocusable(false);root.addView(tv,new FrameLayout.LayoutParams(-1,-1));ui=new Overlay();root.addView(ui,new FrameLayout.LayoutParams(-1,-1));setContentView(root);ui.setFocusable(true);ui.setFocusableInTouchMode(true);ui.requestFocus();try(InputStream picture=getAssets().open("promo_family.jpg")){promoFamily=BitmapFactory.decodeStream(picture);}catch(Exception e){android.util.Log.e("EYMEN-TV","Reklam aile fotoğrafı okunamadı",e);}
 try(InputStream picture=getAssets().open("promo_logo.jpg")){promoLogo=BitmapFactory.decodeStream(picture);}catch(Exception e){android.util.Log.e("EYMEN-TV","Reklam logosu okunamadı",e);}long lastPromo=prefs.getLong(PROMO_LAST_KEY,0L);if(lastPromo<=0||lastPromo>System.currentTimeMillis()){prefs.edit().putLong(PROMO_LAST_KEY,System.currentTimeMillis()).apply();}h.post(clock);
 if(checkSelfPermission("android.permission.READ_TV_LISTINGS")!=PackageManager.PERMISSION_GRANTED){message="Kanal listesini görmek için TV listesi izni gerekli.";requestPermissions(new String[]{"android.permission.READ_TV_LISTINGS"},42);}else loadChannels();}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] result){super.onRequestPermissionsResult(r,p,result);if(checkSelfPermission("android.permission.READ_TV_LISTINGS")==PackageManager.PERMISSION_GRANTED)loadChannels();else{message="İzin verilmedi. MENU → İzin ayarları.";ui.invalidate();}}
 private void loadChannels(){if(loading)return;loading=true;worker.execute(()->{ArrayList<Channel> result=new ArrayList<>();String err=null;try(Cursor c=getContentResolver().query(TvContract.Channels.CONTENT_URI,new String[]{"_id","display_number","display_name","input_id"},null,null,null)){if(c!=null)while(c.moveToNext()){String input=c.getString(3);if(input!=null&&input.startsWith("com.droidlogic.dtvkit.inputsource/")){Channel x=new Channel();x.id=c.getLong(0);x.num=c.getString(1);x.originalName=c.getString(2);x.name=prefs.getString("name_"+x.id,x.originalName);x.input=input;result.add(x);}}result.sort((a,b)->{try{return Integer.compare(Integer.parseInt(a.num),Integer.parseInt(b.num));}catch(Exception e){return String.valueOf(a.num).compareTo(String.valueOf(b.num));}});}catch(Exception e){err=e.toString();}final String error=err;h.post(()->{loading=false;if(dead)return;all.clear();all.addAll(result);restoreOrder();filter();message=error!=null?error:(all.isEmpty()?"DTVKit kanalı bulunamadı.":"Kanal seçip OK'ye bas.");ui.invalidate();if(!visible.isEmpty()&&active){long saved=prefs.getLong("lastChannel",-1);Channel first=visible.get(0);for(Channel x:visible)if(x.id==saved)first=x;play(first);menu=true;}});});}
 private void requestLogo(Channel c){if(c.logoRequested||dead)return;c.logoRequested=true;logoWorker.execute(()->{try(InputStream custom=new FileInputStream(customLogoFile(c.id))){Bitmap bmp=BitmapFactory.decodeStream(custom);if(bmp!=null){h.post(()->{if(!dead){c.logo=bmp;ui.invalidate();}});return;}}catch(Exception ignored){}String local=logoName(c.originalName);try(InputStream asset=getAssets().open("logos/"+local+".png")){Bitmap b=BitmapFactory.decodeStream(asset);if(b!=null){h.post(()->{if(!dead){c.logo=b;ui.invalidate();}});return;}}catch(Exception ignored){}try(InputStream in=getContentResolver().openInputStream(TvContract.buildChannelLogoUri(c.id))){BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeStream(in,null,options);int sample=1;while(options.outWidth/sample>256||options.outHeight/sample>128)sample*=2;try(InputStream data=getContentResolver().openInputStream(TvContract.buildChannelLogoUri(c.id))){options.inJustDecodeBounds=false;options.inSampleSize=sample;Bitmap bmp=BitmapFactory.decodeStream(data,null,options);h.post(()->{if(!dead){c.logo=bmp;ui.invalidate();}});}}catch(Exception ignored){}});}
 private File customLogoFile(long channelId){File dir=new File(getFilesDir(),"channel_logos");return new File(dir,"channel_"+channelId+".png");}
 private void chooseChannelLogo(Channel channel){
  if(channel==null){toast("Önce bir kanal seçin");return;}
  pendingLogoChannelId=channel.id;
  Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  pick.setType("image/*");pick.addCategory(Intent.CATEGORY_OPENABLE);
  pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(pick,REQUEST_CHANNEL_LOGO);}catch(android.content.ActivityNotFoundException e){toast("Bu TV'de dosya seçici bulunamadı. USB dosya yöneticisi gerekli.");}
 }
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
  super.onActivityResult(requestCode,resultCode,data);
  if(requestCode!=REQUEST_CHANNEL_LOGO)return;
  final long channelId=pendingLogoChannelId;pendingLogoChannelId=-1;
  if(resultCode!=RESULT_OK||data==null||data.getData()==null||channelId<0)return;
  final android.net.Uri source=data.getData();
  logoWorker.execute(()->{
   String error=null;Bitmap image=null;
   try{
    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
    try(InputStream input=getContentResolver().openInputStream(source)){BitmapFactory.decodeStream(input,null,o);}
    if(o.outWidth<=0||o.outHeight<=0)throw new IOException("Resim okunamadı");
    int sample=1;while(o.outWidth/sample>512||o.outHeight/sample>512)sample*=2;
    o.inJustDecodeBounds=false;o.inSampleSize=sample;
    try(InputStream input=getContentResolver().openInputStream(source)){image=BitmapFactory.decodeStream(input,null,o);}
    if(image==null)throw new IOException("Desteklenmeyen görsel");
    File destination=customLogoFile(channelId);File directory=destination.getParentFile();
    if(!directory.exists()&&!directory.mkdirs())throw new IOException("Logo klasörü oluşturulamadı");
    File temporary=new File(directory,"channel_"+channelId+".tmp");
    try(FileOutputStream output=new FileOutputStream(temporary)){
     if(!image.compress(Bitmap.CompressFormat.PNG,100,output))throw new IOException("Logo kaydedilemedi");
     output.getFD().sync();
    }
    if(!temporary.renameTo(destination)){
     try(InputStream in=new FileInputStream(temporary);FileOutputStream out=new FileOutputStream(destination)){
      byte[] chunk=new byte[8192];int length;while((length=in.read(chunk))!=-1)out.write(chunk,0,length);out.getFD().sync();
     }
     temporary.delete();
    }
   }catch(Exception e){error=e.getMessage()==null?"Bilinmeyen hata":e.getMessage();}
   final String failure=error;final Bitmap bitmap=image;
   h.post(()->{
    if(dead)return;
    if(failure!=null){if(bitmap!=null)bitmap.recycle();toast("Logo yüklenemedi: "+failure);return;}
    for(Channel channel:all)if(channel.id==channelId){channel.logo=bitmap;channel.logoRequested=true;break;}
    toast("Kanal logosu kaydedildi");ui.invalidate();
   });
  });
 }
 private void resetChannelLogo(Channel channel){
  if(channel==null){toast("Önce bir kanal seçin");return;}
  File file=customLogoFile(channel.id);
  if(file.exists()&&!file.delete()){toast("Özel logo silinemedi");return;}
  channel.logo=null;channel.logoRequested=false;ui.invalidate();toast("Varsayılan kanal logosuna dönüldü");
 }
 private String logoName(String name){String n=java.text.Normalizer.normalize(name==null?"":name,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace('ı','i').replaceAll("(?i)\\b(hd|sd|fhd|uhd)\\b","").replaceAll("[^a-z0-9]","");String[][] aliases={{"trt1","trt-1-hd"},{"showtv","show"},{"startv","star-tv"},{"star","star-tv"},{"tv85","tv85"},{"fox","now"},{"nowtv","now"},{"cnnturk","cnn-turk"},{"ahaber","a-haber"},{"aspor","a-spor"}};for(String[] a:aliases)if(a[0].equals(n))return a[1];try{for(String file:getAssets().list("logos")){String key=file.replace(".png","").replaceAll("[^a-z0-9]","");if(key.equals(n))return file.replace(".png","");}}catch(Exception ignored){}return n;}
 private Channel selectedChannel(){return menu&&!visible.isEmpty()?visible.get(Math.max(0,selected)):current;}
 private void restoreOrder(){String saved=prefs.getString("order","");final HashMap<Long,Integer> ranks=new HashMap<>();int n=0;for(String id:saved.split(","))try{ranks.put(Long.parseLong(id),n++);}catch(Exception ignored){}all.sort((a,b)->Integer.compare(ranks.containsKey(a.id)?ranks.get(a.id):Integer.MAX_VALUE,ranks.containsKey(b.id)?ranks.get(b.id):Integer.MAX_VALUE));}
 private void saveOrder(){StringBuilder out=new StringBuilder();for(Channel c:all)out.append(c.id).append(',');prefs.edit().putString("order",out.toString()).apply();}
 private void editChannel(){Channel ch=selectedChannel();if(ch==null){toast("Önce kanal seçin");return;}new AlertDialog.Builder(this).setTitle(ch.name+" • Kanal düzenleme").setItems(new String[]{"Adını değiştir","Kendi logomu yükle (PNG/JPG)","Özel logoyu kaldır","Bir sıra yukarı taşı","Bir sıra aşağı taşı","Uygulama listesinden sil","Kanal adını sıfırla"},(d,w)->{if(w==0){EditText field=new EditText(this);field.setSingleLine(true);field.setText(ch.name);new AlertDialog.Builder(this).setTitle("Kanal adı").setView(field).setPositiveButton("Kaydet",(a,b)->{String name=field.getText().toString().trim();if(name.isEmpty()){toast("Ad boş bırakılamaz");return;}ch.name=name;prefs.edit().putString("name_"+ch.id,name).apply();ui.invalidate();}).setNegativeButton("Vazgeç",null).show();}else if(w==1){chooseChannelLogo(ch);}else if(w==2){resetChannelLogo(ch);}else if(w==3||w==4){int i=all.indexOf(ch),j=i+(w==3?-1:1);if(j>=0&&j<all.size()){Collections.swap(all,i,j);saveOrder();filter();selected=Math.max(0,visible.indexOf(ch));ui.invalidate();}}else if(w==5){new AlertDialog.Builder(this).setTitle("Bu listeden silinsin mi?").setMessage(ch.name+" EYMEN TV listesinden kaldırılacak. TV'nin asıl uydu kaydı korunur; Ayarlar'dan geri getirebilirsiniz.").setPositiveButton("Sil",(a,b)->{hidden.add(String.valueOf(ch.id));prefs.edit().putStringSet("hidden",new HashSet<>(hidden)).apply();filter();ui.invalidate();}).setNegativeButton("Vazgeç",null).show();}else{ch.name=ch.originalName;prefs.edit().remove("name_"+ch.id).apply();ui.invalidate();}}).show();}
 private void settings(){new AlertDialog.Builder(this).setTitle("EYMEN TV • Uygulama ayarları").setItems(new String[]{"Seçili kanalı düzenle / logo / sil","Silinen kanalları geri getir","Tüm kanallar / Favoriler","Görünüm, ses ve diğer ayarlar"},(d,w)->{if(w==0)editChannel();else if(w==1){hidden.clear();prefs.edit().remove("hidden").apply();filter();ui.invalidate();toast("Silinen kanallar geri getirildi");}else if(w==2){favsOnly=!favsOnly;selected=0;tab=0;filter();ui.invalidate();}else options();}).show();}
 private String favoriteKey(Channel c){return c.input+"|"+c.num+"|"+c.originalName;}
 private void filter(){visible.clear();for(Channel c:all)if(!hidden.contains(String.valueOf(c.id))&&(!favsOnly||favorites.contains(favoriteKey(c))))visible.add(c);selected=Math.max(0,Math.min(selected,visible.size()-1));}
 private void toggleFavorite(){Channel c=menu&&!visible.isEmpty()?visible.get(selected):current;if(c==null)return;String k=favoriteKey(c);if(!favorites.remove(k))favorites.add(k);prefs.edit().putStringSet("favorites",new HashSet<>(favorites)).apply();filter();ui.invalidate();}
 private void play(Channel c){if(!active)return;current=c;now=null;next=null;final int token=++epoch;stoppedPlayback=false;soundTracks.clear();audioState="Ses izleri bekleniyor";message="Kanal açılıyor…";tv.reset();hasFocus=audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
 tv.setCallback(new TvView.TvInputCallback(){boolean audioChosen=false;
 public void onVideoAvailable(String id){if(token!=epoch)return;message="";tv.setStreamVolume(hasFocus?1:0);showInfo();}
 public void onVideoUnavailable(String id,int reason){if(token!=epoch)return;String[] reasons={"Görüntü henüz hazır değil","Kanal ayarlanıyor…","Sinyal zayıf","Yayın hazırlanıyor…","Yalnızca ses yayını"};message=reason>=0&&reason<reasons.length?reasons[reason]:"Görüntü durumu: "+reason;ui.invalidate();}
 public void onConnectionFailed(String id){if(token==epoch){message="Yayın servisine bağlanılamadı. OK ile tekrar deneyin.";ui.invalidate();}}
 public void onDisconnected(String id){if(token==epoch){message="Yayın bağlantısı kesildi.";ui.invalidate();}}
 public void onContentBlocked(String id,TvContentRating rating){if(token==epoch){message="TV ebeveyn denetimi bu yayını engelledi.";ui.invalidate();}}
 public void onTracksChanged(String id,List<TvTrackInfo> tracks){if(token!=epoch)return;soundTracks.clear();if(tracks!=null)for(TvTrackInfo t:tracks)if(t.getType()==TvTrackInfo.TYPE_AUDIO)soundTracks.add(t);audioState="Ses izi: "+soundTracks.size()+" • Odak: "+(hasFocus?"alındı":"alınamadı");if(!audioChosen&&!soundTracks.isEmpty()){audioChosen=true;TvTrackInfo chosen=soundTracks.get(0);for(TvTrackInfo t:soundTracks)if("tur".equals(t.getLanguage())||"tr".equals(t.getLanguage())){chosen=t;break;}try{tv.selectTrack(TvTrackInfo.TYPE_AUDIO,chosen.getId());tv.setStreamVolume(hasFocus?1:0);}catch(Exception e){audioState=e.toString();}}ui.invalidate();}
 public void onTrackSelected(String id,int type,String track){if(token==epoch&&type==TvTrackInfo.TYPE_AUDIO){audioState="Ses izi: "+soundTracks.size()+" • Seçili: "+track+" • Odak: "+hasFocus;}}
 });try{tv.tune(c.input,TvContract.buildChannelUri(c.id));tv.setStreamVolume(hasFocus?1:0);}catch(Exception e){message=e.getClass().getSimpleName()+": "+e.getMessage();}prefs.edit().putLong("lastChannel",c.id).apply();loadEpg(c);showInfo();}
 private void loadEpg(Channel ch){lastEpg=System.currentTimeMillis();long stamp=lastEpg;worker.execute(()->{Program present=null,upcoming=null;try(Cursor c=getContentResolver().query(TvContract.buildProgramsUriForChannel(ch.id,stamp,stamp+86400000L),new String[]{"title","short_description","start_time_utc_millis","end_time_utc_millis"},null,null,"start_time_utc_millis ASC")){if(c!=null)while(c.moveToNext()){Program p=new Program();p.title=c.getString(0);p.description=c.getString(1);p.start=c.getLong(2);p.end=c.getLong(3);if(p.start<=stamp&&p.end>stamp)present=p;else if(p.start>stamp&&upcoming==null)upcoming=p;if(present!=null&&upcoming!=null)break;}}catch(Exception ignored){}final Program a=present,b=upcoming;h.post(()->{if(!dead&&current==ch){now=a;next=b;ui.invalidate();}});});}
 private void showInfo(){info=true;h.removeCallbacks(hide);if(!menu)h.postDelayed(hide,hideSeconds*1000L);ui.invalidate();}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 private void soundDialog(){ArrayList<String> labels=new ArrayList<>();for(TvTrackInfo t:soundTracks)labels.add((t.getLanguage()==null?"Dil belirtilmemiş":t.getLanguage())+" • "+t.getAudioChannelCount()+" kanal • "+t.getId());if(labels.isEmpty()){new AlertDialog.Builder(this).setTitle("Ses tanılama").setMessage(audioState+"\nSistem medya sesi: "+audio.getStreamVolume(AudioManager.STREAM_MUSIC)+" / "+audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)+"\nSessiz: "+audio.isStreamMute(AudioManager.STREAM_MUSIC)+"\nOrijinal Uydu uygulamasında aynı kanalı karşılaştırın.").setPositiveButton("Tamam",null).show();return;}new AlertDialog.Builder(this).setTitle("Ses izi seç • "+(hasFocus?"Odak alındı":"Odak alınamadı")).setItems(labels.toArray(new String[0]),(d,w)->{try{hasFocus=audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;tv.selectTrack(TvTrackInfo.TYPE_AUDIO,soundTracks.get(w).getId());tv.setStreamVolume(hasFocus?1:0);toast("Ses izi seçildi; sesi kontrol edin.");}catch(Exception e){toast(e.toString());}}).setNegativeButton("Kapat",null).show();}
 private void options(){new AlertDialog.Builder(this).setTitle("EYMEN TV • Seçenekler").setItems(new String[]{"Favoriye ekle / çıkar","Ses izi seç ve tanılama","Program ayrıntıları","Menü şeffaflığı","Bilgi çubuğu süresi","Kanal listesini yenile","İzin ayarları","Hakkında","Reklamı şimdi göster (test)"},(d,w)->{switch(w){case 0:toggleFavorite();break;case 1:soundDialog();break;case 2:details();break;case 3:new AlertDialog.Builder(this).setTitle("Menü yoğunluğu").setSingleChoiceItems(new String[]{"Hafif","Dengeli","Koyu"},opacity<170?0:opacity<215?1:2,(a,n)->{opacity=new int[]{145,185,225}[n];prefs.edit().putInt("opacity",opacity).apply();ui.invalidate();a.dismiss();}).show();break;case 4:new AlertDialog.Builder(this).setTitle("Bilgi çubuğu süresi").setItems(new String[]{"3 saniye","5 saniye","8 saniye"},(a,n)->{hideSeconds=new int[]{3,5,8}[n];prefs.edit().putInt("hideSeconds",hideSeconds).apply();showInfo();}).show();break;case 5:loadChannels();break;case 6:startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));break;case 8:menu=false;info=false;startPromo(true);break;default:new AlertDialog.Builder(this).setTitle("EYMEN TV 0.4 • Cihaz denemesi").setMessage("Muhammet Atasoy\natasoyyazılım.com\nDahili DTVKit yayını • İnternetsiz\nÖzel kanal logoları: dosyadan yüklenebilir\n"+audioState+"\nSes düzeltmeleri cihazda doğrulanmalıdır.").setPositiveButton("Tamam",null).show();}}).show();}
 private void details(){new AlertDialog.Builder(this).setTitle(now==null?"Program bilgisi":now.title).setMessage(now==null?"Bu kanal için güncel yayın bilgisi bulunamadı.":time(now.start)+" – "+time(now.end)+"\n\n"+(now.description==null?"Açıklama bulunamadı.":now.description)).setPositiveButton("Kapat",null).show();}
 private void step(int delta){ArrayList<Channel> data=visible;if(data.isEmpty()){toast("Bu listede kanal yok");return;}int i=data.indexOf(current);if(i<0)i=delta>0?-1:0;i=(i+delta+data.size())%data.size();play(data.get(i));selected=Math.max(0,visible.indexOf(current));}
 @Override public boolean onKeyDown(int key,KeyEvent e){if(key>=KeyEvent.KEYCODE_0&&key<=KeyEvent.KEYCODE_9){if(digits.length()>=4)digits="";digits+=key-KeyEvent.KEYCODE_0;h.removeCallbacks(enterNumber);h.postDelayed(enterNumber,1500);ui.invalidate();return true;}switch(key){case KeyEvent.KEYCODE_DPAD_UP:if(menu){if(tab==2){tab=0;selected=Math.max(0,Math.min(visible.size()-1,(selected/10)*10+9));}else if(selected%10==0)tab=1;else selected--;}else step(-1);break;case KeyEvent.KEYCODE_DPAD_DOWN:if(menu){if(tab==1)tab=0;else if(tab==0&&(selected%10==9||selected==visible.size()-1))tab=2;else if(tab==0)selected=Math.min(visible.size()-1,selected+1);}else step(1);break;case KeyEvent.KEYCODE_DPAD_LEFT:case KeyEvent.KEYCODE_DPAD_RIGHT:if(menu){selected=PageNav.jump(selected,visible.size(),key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);tab=0;}else{menu=true;info=true;}break;case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:if(!digits.isEmpty()){h.removeCallbacks(enterNumber);enterNumber.run();}else if(menu){if(tab==2){settings();}else if(tab==1){favsOnly=!favsOnly;filter();}else if(!visible.isEmpty()){play(visible.get(selected));menu=false;showInfo();}}else{menu=true;info=true;selected=Math.max(0,visible.indexOf(current));}break;case KeyEvent.KEYCODE_CHANNEL_UP:step(1);break;case KeyEvent.KEYCODE_CHANNEL_DOWN:step(-1);break;case KeyEvent.KEYCODE_MENU:settings();break;case KeyEvent.KEYCODE_PROG_GREEN:case KeyEvent.KEYCODE_BOOKMARK:toggleFavorite();break;case KeyEvent.KEYCODE_PROG_BLUE:soundDialog();break;case KeyEvent.KEYCODE_INFO:case KeyEvent.KEYCODE_PROG_YELLOW:if(info&&!menu)details();else showInfo();break;case KeyEvent.KEYCODE_PROG_RED:menu=!menu;showInfo();break;case KeyEvent.KEYCODE_BACK:if(menu){menu=false;showInfo();}else new AlertDialog.Builder(this).setTitle("EYMEN TV kapatılsın mı?").setPositiveButton("Çık",(d,w)->finish()).setNegativeButton("İzlemeye devam et",null).show();break;default:return super.onKeyDown(key,e);}ui.invalidate();return true;}
 @Override protected void onStart(){super.onStart();active=true;if(stoppedPlayback&&current!=null)play(current);}
 @Override protected void onStop(){active=false;promoStartedUptime=-1L;epoch++;tv.reset();audio.abandonAudioFocusRequest(focus);hasFocus=false;stoppedPlayback=current!=null;h.removeCallbacks(hide);super.onStop();}
 @Override protected void onDestroy(){dead=true;h.removeCallbacksAndMessages(null);worker.shutdownNow();logoWorker.shutdownNow();super.onDestroy();}
 private String time(long t){return new SimpleDateFormat("HH:mm",tr).format(new Date(t));}
 class Overlay extends View {final Paint p=new Paint(3);Overlay(){super(PlayerActivity.this);}void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(size);p.setColor(color);c.drawText(s==null?"":s,x,y,p);}void fit(Canvas c,String s,float x,float y,float size,float width,int color,boolean bold){p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextSize(size);String safe=s==null?"":s;while(p.measureText(safe)>width&&safe.length()>1)safe=safe.substring(0,safe.length()-1);if(!safe.equals(s))safe=safe.length()>1?safe.substring(0,safe.length()-1)+"…":"…";text(c,safe,x,y,size,color,bold);}void panel(Canvas c,float l,float t,float r,float b,boolean focus){p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(focus?130:opacity,focus?0:10,focus?130:25,focus?150:45));c.drawRoundRect(l,t,r,b,18,18,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(focus?2:1);p.setColor(focus?0xff35e6ee:0xff6085a2);c.drawRoundRect(l,t,r,b,18,18,p);p.setStyle(Paint.Style.FILL);}void logo(Canvas c,Channel ch,float x,float y,float w,float ht){requestLogo(ch);if(ch.logo!=null){float scale=Math.min(w/ch.logo.getWidth(),ht/ch.logo.getHeight());float bw=ch.logo.getWidth()*scale,bh=ch.logo.getHeight()*scale;c.drawBitmap(ch.logo,null,new RectF(x+(w-bw)/2,y+(ht-bh)/2,x+(w+bw)/2,y+(ht+bh)/2),p);}else{p.setColor(0xff1b4b65);c.drawRoundRect(x,y,x+w,y+ht,8,8,p);fit(c,ch.name,x+8,y+ht*.62f,18,w-16,Color.WHITE,true);}}

 private float ease(float x){float v=Math.max(0f,Math.min(1f,x));return 1f-(1f-v)*(1f-v)*(1f-v);}
 private float entrance(long elapsed,long delay,long duration){return ease((elapsed-delay)/(float)duration);}
 private void promoTyping(Canvas c,String s,float x,float y,float size,int color,long elapsed,long from,long duration){
  float progress=Math.max(0f,Math.min(1f,(elapsed-from)/(float)duration));if(progress<=0)return;
  int count=Math.min(s.length(),Math.max(0,(int)Math.ceil(s.length()*progress)));
  text(c,s.substring(0,count),x,y,size,color,true);
 }
 private void drawPromo(Canvas c){
  long elapsed=SystemClock.uptimeMillis()-promoStartedUptime;
  if(elapsed<0||elapsed>=PROMO_DURATION_MS)return;
  float in=entrance(elapsed,0,750),out=Math.max(0f,Math.min(1f,(PROMO_DURATION_MS-elapsed)/850f));
  c.save();c.clipRect(0,530,1280,720);c.translate(0,(1f-in)*195f);
  c.saveLayerAlpha(0,525,1280,720,(int)(255*out));
  p.setStyle(Paint.Style.FILL);
  p.setShader(new LinearGradient(0,540,1280,720,new int[]{0xff00336c,0xff051a52,0xffad0c38},new float[]{0f,.62f,1f},Shader.TileMode.CLAMP));
  c.drawRect(0,542,1280,720,p);p.setShader(null);
  if(promoFamily!=null){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setAlpha(255);c.drawBitmap(promoFamily,null,new RectF(0,543,456,716),p);p.setAlpha(255);}
  p.setShader(new LinearGradient(320,570,535,570,0x00051a52,0xff051a52,Shader.TileMode.CLAMP));
  c.drawRect(318,543,535,720,p);p.setShader(null);
  p.setColor(0xffffca55);c.drawRect(0,540,1280,544,p);
  p.setColor(0xff45d6ff);c.drawRect(0,715,1280,719,p);
  // Fotoğraf görünür, metin blokları birbiri ardına açılır.
  if(elapsed>950){
   p.setColor(0xffb6092d);c.drawRoundRect(470,555,984,601,13,13,p);
   p.setColor(0xff063b91);c.drawRoundRect(470,606,984,650,13,13,p);
   p.setColor(0xffe4b744);c.drawRoundRect(470,655,984,704,13,13,p);
  }
  promoTyping(c,"www.atasoyyazılım.com",488,587,24,Color.WHITE,elapsed,1450,2800);
  promoTyping(c,"İletişim: 0531 221 89 65",488,638,22,Color.WHITE,elapsed,3850,2350);
  promoTyping(c,"MUHAMMET ATASOY",488,687,27,0xff072552,elapsed,6200,1950);
  float logo=entrance(elapsed,8200,1300);
  if(logo>0){
   c.save();c.scale(.88f+.12f*logo,.88f+.12f*logo,1136,622);
   c.saveLayerAlpha(985,554,1280,706,(int)(255*logo));
   p.setColor(0xff04245c);c.drawRoundRect(985,554,1280,704,17,17,p);
   if(promoLogo!=null){
    p.setAlpha(255);p.setStyle(Paint.Style.FILL);
    c.drawBitmap(promoLogo,null,new RectF(992,561,1275,655),p);
   }else{
    p.setColor(0xff125cba);c.drawRoundRect(993,569,1272,659,13,13,p);
    text(c,"EYMEN TV",1005,623,35,Color.WHITE,true);
   }
   p.setColor(0xffffd86d);c.drawRoundRect(994,665,1273,697,11,11,p);
   text(c,"Eymen TV ile dijital çözümler",1005,688,14,0xff061b54,true);
   c.restore();c.restore();
  }
  c.restore();c.restore();
 }
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/1280f,getHeight()/720f);int white=Color.WHITE,muted=0xffbdcbd6,cyan=0xff35e6ee;
 long phase=(SystemClock.uptimeMillis()-animationStart)%30000;float pulse=phase<4000?(float)Math.sin(phase/400f)*0.035f:0;c.save();c.scale(1+pulse,1+pulse,1130,50);p.setShader(new LinearGradient(1040,0,1210,0,new int[]{0xff35e6ee,0xff8893ff,0xffff7aca},null,Shader.TileMode.CLAMP));text(c,"EYMEN TV",1045,52,27,white,true);p.setShader(null);c.restore();String brand="atasoyyazılım.com";int letters=Math.min(brand.length(),(int)(phase/130));float bx=1045;for(int z=0;z<letters;z++){String one=brand.substring(z,z+1);text(c,one,bx,76,17,new int[]{0xff4be4ed,0xff83b7ff,0xffe0a3ff,0xffffba77}[z%4],true);bx+=p.measureText(one);}
 if(menu){panel(c,28,38,378,680,false);text(c,"EYMEN",48,87,33,white,true);text(c,"TV",176,87,33,cyan,true);if(!favsOnly)panel(c,46,111,216,151,tab==1);else panel(c,224,111,360,151,tab==1);text(c,"Tüm Kanallar",57,137,20,favsOnly?muted:white,true);text(c,"Favoriler",235,137,20,favsOnly?white:muted,true);
 int start=(selected/10)*10;for(int k=0;k<10&&start+k<visible.size();k++){int i=start+k;Channel ch=visible.get(i);float y=163+k*44;if(i==selected&&tab==0)panel(c,38,y,368,y+41,true);text(c,ch.num,50,y+27,16,white,true);logo(c,ch,82,y+5,67,30);fit(c,ch.name,160,y+27,17,172,white,true);text(c,favorites.contains(favoriteKey(ch))?"★":"☆",340,y+27,20,favorites.contains(favoriteKey(ch))?0xffffce54:muted,false);if(ch==current){p.setColor(cyan);c.drawCircle(363,y+20,3,p);}}
 if(visible.isEmpty())fit(c,favsOnly?"Henüz favori kanal yok":"Kanal bulunamadı",50,218,18,300,white,false);panel(c,46,615,360,647,tab==2);text(c,"⚙  Uygulama ayarları",66,637,18,white,true);text(c,"◀  Sayfa "+(visible.isEmpty()?0:selected/10+1)+" / "+((visible.size()+9)/10)+"  ▶   •   "+visible.size()+" kanal",50,670,14,muted,false);}
 if(info||menu){float left=menu?406:38;panel(c,left,510,1250,654,false);if(current!=null){logo(c,current,left+18,530,95,54);fit(c,current.name,left+18,611,16,116,muted,true);float x=left+135;fit(c,now==null?"Program bilgisi yok":now.title,x,552,28,menu?340:640,white,true);text(c,now==null?"Yayın rehberi bekleniyor":"Şimdi  •  "+time(now.start)+" – "+time(now.end),x,581,18,cyan,false);float barWidth=menu?330:625;p.setColor(0xff526474);c.drawRoundRect(x,600,x+barWidth,605,3,3,p);if(now!=null&&now.end>now.start){float progress=Math.max(0,Math.min(1,(System.currentTimeMillis()-now.start)/(float)(now.end-now.start)));p.setColor(cyan);c.drawRoundRect(x,600,x+barWidth*progress,605,3,3,p);}text(c,time(System.currentTimeMillis())+"  •  "+new SimpleDateFormat("d MMMM EEEE",tr).format(new Date()),x,635,16,white,false);text(c,"SIRADAKİ",1002,542,15,muted,true);fit(c,next==null?"Bilgi bulunamadı":next.title,1002,573,20,225,white,true);if(next!=null)text(c,time(next.start)+"’te başlayacak",1002,603,18,white,false);}
 fit(c,"OK  Kanal listesi    •    Yeşil  Favori    •    Mavi  Ses    •    MENU  Ayarlar",left+4,687,16,1240-left,white,false);}
 if(!message.isEmpty()){panel(c,410,418,1238,486,false);fit(c,message,430,460,18,785,white,false);}if(!digits.isEmpty()){panel(c,1100,100,1240,170,true);text(c,digits,1120,147,30,white,true);}if(promoStartedUptime>0)drawPromo(c);c.restore();}
 }
}
