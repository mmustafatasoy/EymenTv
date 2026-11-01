package com.eymen.uydukontrol;
import android.app.*;import android.os.*;import android.content.*;import android.content.pm.PackageManager;import android.database.Cursor;import android.graphics.*;import android.media.*;import android.media.tv.*;import android.view.*;import android.widget.*;import java.util.*;import java.text.SimpleDateFormat;import java.util.concurrent.*;import java.io.*;
public class PlayerActivity extends Activity {
 static class Channel {long id;String name,num,input,originalName;Bitmap logo;boolean logoRequested;}
 static class Program {String title,description;long start,end;}
 private TvView tv;private Overlay ui;private AudioManager audio;private AudioFocusRequest focus;private boolean hasFocus=false,active=false,dead=false,menu=true,info=true,favsOnly=false,loading=false;private int selected=0,tab=0,epoch=0;private String message="Kanallar yükleniyor…",audioState="Henüz ses izi bildirilmedi",digits="";private Channel current;private Program now,next;private final ArrayList<Channel> all=new ArrayList<>(),visible=new ArrayList<>();private final ArrayList<TvTrackInfo> soundTracks=new ArrayList<>();private final Handler h=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();private final ExecutorService logoWorker=Executors.newFixedThreadPool(2);private SharedPreferences prefs;private Set<String> favorites,hidden;private long animationStart=SystemClock.uptimeMillis();private final Locale tr=new Locale("tr","TR");private long lastEpg=0;private int opacity=185,hideSeconds=5;private boolean showChannelLogos=true,showEpg=true,showWatermark=true,hourlyBrandEnabled=true;private boolean stoppedPlayback=false;private static final int REQUEST_CHANNEL_LOGO=504;private long pendingLogoChannelId=-1;private Bitmap brandLogo;private long brandStartedUptime=-1L;
 // Reuse existing timestamp during upgrade to avoid an unexpected immediate brand appearance.
 private static final long BRAND_INTERVAL_MS=3600000L,BRAND_DURATION_MS=15000L;
 private static final String BRAND_LAST_KEY="promo_last_shown_epoch";
 private final Runnable hide=()->{if(!menu){info=false;ui.invalidate();}};
 private final Runnable enterNumber=()->{String n=digits;digits="";for(Channel c:all)if(!hidden.contains(String.valueOf(c.id))&&n.equals(c.num)){play(c);return;}toast("Kanal numarası bulunamadı");};
 private final Runnable clock=new Runnable(){public void run(){
  if(dead)return;checkBrand();ui.invalidate();
  if(active&&current!=null&&System.currentTimeMillis()-lastEpg>60000)loadEpg(current);
  h.postDelayed(this,brandStartedUptime>0?40:110);
 }};
 private void checkBrand(){
  if(!active||!hourlyBrandEnabled)return;
  long uptime=SystemClock.uptimeMillis();
  if(brandStartedUptime>0){
   if(uptime-brandStartedUptime>=BRAND_DURATION_MS)brandStartedUptime=-1L;
   return;
  }
  if(current==null||menu)return;
  long now=System.currentTimeMillis();
  long last=prefs.getLong(BRAND_LAST_KEY,now);
  if(now-last>=BRAND_INTERVAL_MS)startBrand(false);
 }
 private void startBrand(boolean preview){
  brandStartedUptime=SystemClock.uptimeMillis();
  if(!preview)prefs.edit().putLong(BRAND_LAST_KEY,System.currentTimeMillis()).apply();
  ui.invalidate();
 }
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("eymen_tv",MODE_PRIVATE);favorites=new HashSet<>(prefs.getStringSet("favorites",new HashSet<String>()));hidden=new HashSet<>(prefs.getStringSet("hidden",new HashSet<String>()));opacity=prefs.getInt("opacity",185);hideSeconds=prefs.getInt("hideSeconds",5);showChannelLogos=prefs.getBoolean("channelLogos",true);showEpg=prefs.getBoolean("showEpg",true);showWatermark=prefs.getBoolean("showWatermark",true);hourlyBrandEnabled=prefs.getBoolean("hourlyBrand",true);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().getDecorView().setSystemUiVisibility(5894);setVolumeControlStream(AudioManager.STREAM_MUSIC);
 audio=(AudioManager)getSystemService(AUDIO_SERVICE);focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()).setOnAudioFocusChangeListener(change->{hasFocus=change==AudioManager.AUDIOFOCUS_GAIN;if(tv!=null)tv.setStreamVolume(hasFocus?1f:0f);if(change==AudioManager.AUDIOFOCUS_LOSS){tv.reset();stoppedPlayback=true;message="Ses odağı başka uygulamaya geçti; OK ile yeniden aç.";ui.invalidate();}},h).build();
 FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);tv=new TvView(this);tv.setFocusable(false);root.addView(tv,new FrameLayout.LayoutParams(-1,-1));ui=new Overlay();root.addView(ui,new FrameLayout.LayoutParams(-1,-1));setContentView(root);ui.setFocusable(true);ui.setFocusableInTouchMode(true);ui.requestFocus();try(InputStream logo=getAssets().open("brand_3d.png")){
  brandLogo=BitmapFactory.decodeStream(logo);
 }catch(Exception e){android.util.Log.e("EYMEN-TV","Köşe logosu okunamadı",e);}
 long lastBrand=prefs.getLong(BRAND_LAST_KEY,0L);
 if(lastBrand<=0||lastBrand>System.currentTimeMillis())prefs.edit().putLong(BRAND_LAST_KEY,System.currentTimeMillis()).apply();h.post(clock);
 if(checkSelfPermission("android.permission.READ_TV_LISTINGS")!=PackageManager.PERMISSION_GRANTED){message="Kanal listesini görmek için TV listesi izni gerekli.";requestPermissions(new String[]{"android.permission.READ_TV_LISTINGS"},42);}else loadChannels();}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] result){super.onRequestPermissionsResult(r,p,result);if(checkSelfPermission("android.permission.READ_TV_LISTINGS")==PackageManager.PERMISSION_GRANTED)loadChannels();else{message="İzin verilmedi. MENU → İzin ayarları.";ui.invalidate();}}
 private void loadChannels(){
  if(loading)return;loading=true;
  worker.execute(()->{
   ArrayList<Channel> raw=new ArrayList<>(),result=new ArrayList<>();
   String err=null;int scanned=0;int tvInputs=0;
   try{
    TvInputManager manager=(TvInputManager)getSystemService(TV_INPUT_SERVICE);
    if(manager!=null)tvInputs=manager.getTvInputList().size();
   }catch(Exception ignored){}
   try(Cursor c=getContentResolver().query(
     TvContract.Channels.CONTENT_URI,
     new String[]{"_id","display_number","display_name","input_id"},
     null,null,null)){
    if(c==null){err="TV kanal sağlayıcısı yanıt vermedi. Vestel uydu kayıtları üçüncü taraf uygulamalara kapalı olabilir.";}
    else{scanned=c.getCount();while(c.moveToNext()){
      String input=c.getString(3);
      if(input==null||input.trim().isEmpty())continue;
      Channel x=new Channel();x.id=c.getLong(0);
      x.num=c.getString(1);x.originalName=c.getString(2);
      x.name=prefs.getString("name_"+x.id,x.originalName);
      x.input=input;raw.add(x);
    }}
   }catch(SecurityException e){err="TV kanal erişim izni reddedildi: "+e.getMessage();}
    catch(Exception e){err="Kanal veritabanı okunamadı: "+e.toString();}
   // Existing Whale televisions retain the known-good DTVKit list. Other Android TVs
   // (including Vestel models) may expose a different TV Input Framework provider ID.
   // Never invent channels: use only rows that the device actually exposes.
   boolean dtvkitFound=false;
   for(Channel ch:raw)if(ch.input.startsWith("com.droidlogic.dtvkit.inputsource/")){dtvkitFound=true;break;}
   for(Channel ch:raw)if(!dtvkitFound||ch.input.startsWith("com.droidlogic.dtvkit.inputsource/"))result.add(ch);
   result.sort((a,b)->{
    try{return Integer.compare(Integer.parseInt(a.num),Integer.parseInt(b.num));}
    catch(Exception e){return String.valueOf(a.num).compareTo(String.valueOf(b.num));}
   });
   final String error=err;final int readable=scanned,availableInputs=tvInputs;
   h.post(()->{
    loading=false;if(dead)return;
    all.clear();all.addAll(result);restoreOrder();filter();
    message=error!=null?error:(all.isEmpty()?
      "Kanal erişimi yok: "+readable+" görünür kayıt, "+availableInputs+" TV girişi. TV sistem izinlerini kontrol edin.":
      "Kanal seçip OK'ye bas.");
    ui.invalidate();
    if(!visible.isEmpty()&&active){
     long saved=prefs.getLong("lastChannel",-1);Channel first=visible.get(0);
     for(Channel x:visible)if(x.id==saved)first=x;
     play(first);menu=true;
    }
   });
  });
 }
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
 private void bulkHideChannels(){
 ArrayList<Channel> candidates=new ArrayList<>();
 for(Channel ch:all)if(!hidden.contains(String.valueOf(ch.id)))candidates.add(ch);
 if(candidates.isEmpty()){toast("Silinecek kanal bulunamadı");return;}
 String[] labels=new String[candidates.size()];
 final boolean[] checked=new boolean[candidates.size()];
 for(int i=0;i<candidates.size();i++){
  Channel ch=candidates.get(i);labels[i]=(ch.num==null?"":ch.num+" • ")+ch.name;
 }
 new AlertDialog.Builder(this)
 .setTitle("Toplu kanal sil • Kanalları işaretle")
 .setMultiChoiceItems(labels,checked,(dialog,index,value)->checked[index]=value)
 .setPositiveButton("Seçilenleri sil",(dialog,which)->{
  final ArrayList<String> ids=new ArrayList<>();
  for(int i=0;i<candidates.size();i++)if(checked[i])ids.add(String.valueOf(candidates.get(i).id));
  if(ids.isEmpty()){toast("Önce kanalları işaretleyin");return;}
  new AlertDialog.Builder(this)
   .setTitle(ids.size()+" kanal silinsin mi?")
   .setMessage("Yalnızca EYMEN TV uygulamasındaki kanallar gizlenecek. Televizyonun gerçek uydu kanal listesi silinmez. Ayarlardan geri getirebilirsiniz.")
   .setPositiveButton("Evet, "+ids.size()+" kanalı sil",(c,w)->{
    hidden.addAll(ids);
    prefs.edit().putStringSet("hidden",new HashSet<>(hidden)).apply();
    filter();ui.invalidate();toast(ids.size()+" kanal listeden kaldırıldı");
   })
   .setNegativeButton("İptal",null).show();
 })
 .setNegativeButton("Vazgeç",null).show();
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private android.graphics.drawable.GradientDrawable menuBackground(int fill,int edge){
   android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();
   shape.setColor(fill);shape.setCornerRadius(dp(13));shape.setStroke(dp(1),edge);return shape;
 }
 private void neonMenu(String title,String[] rows,DialogInterface.OnClickListener action){
   LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);
   shell.setPadding(dp(16),dp(18),dp(16),dp(12));
   shell.setBackground(menuBackground(0xff08162e,0xff16b2e4));
   TextView heading=new TextView(this);heading.setText("▣  "+title);
   heading.setTextSize(22);heading.setTextColor(Color.WHITE);
   heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   heading.setPadding(dp(7),0,dp(7),dp(8));shell.addView(heading);
   TextView help=new TextView(this);help.setText("OK ile seç  •  ▲ ▼ ile gez  •  Geri ile kapat");
   help.setTextSize(12);help.setTextColor(0xff94d8f2);
   help.setPadding(dp(8),0,dp(8),dp(9));shell.addView(help);
   ListView list=new ListView(this);
   list.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
   list.setDividerHeight(dp(4));list.setCacheColorHint(Color.TRANSPARENT);
   list.setSelector(menuBackground(0xff641d64,0xffff3c9b));
   list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,rows){
     @Override public View getView(int position,View recycled,ViewGroup parent){
       TextView row=(TextView)super.getView(position,recycled,parent);
       row.setTextColor(Color.WHITE);row.setTextSize(16);
       row.setPadding(dp(17),dp(10),dp(12),dp(10));
       row.setMinHeight(dp(51));row.setGravity(Gravity.CENTER_VERTICAL);
       android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
       states.addState(new int[]{android.R.attr.state_selected},menuBackground(0xff49164d,0xffff3c9b));
       states.addState(new int[]{android.R.attr.state_focused},menuBackground(0xff49164d,0xffff3c9b));
       states.addState(new int[]{},menuBackground(0xff102749,0xff315987));
       row.setBackground(states);
       return row;
     }
   });
   shell.addView(list,new LinearLayout.LayoutParams(-1,dp(Math.min(470,rows.length*55))));
   AlertDialog dialog=new AlertDialog.Builder(this).setView(shell)
     .setNegativeButton("Kapat",(d,w)->{}).create();
   list.setOnItemClickListener((parent,view,pos,id)->{dialog.dismiss();action.onClick(dialog,pos);});
   dialog.setOnShowListener(d->{
     Window win=dialog.getWindow();
     if(win!=null){win.setBackgroundDrawableResource(android.R.color.transparent);
       win.setLayout(Math.min(dp(850),getResources().getDisplayMetrics().widthPixels-dp(36)),-2);}
     list.requestFocus();list.setSelection(0);
     Button close=dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
     if(close!=null){close.setTextColor(0xff36c8ef);close.setAllCaps(false);}
   });
   dialog.show();
 }
 private void settings(){
   neonMenu("EYMEN TV • KANAL YÖNETİMİ",new String[]{
     "✎  Kanal düzenle / kendi logosunu yükle",
     "☑  Toplu kanal sil (işaretleyerek)",
     "↶  Silinen kanalları geri getir",
     "★  Tüm kanallar / Favoriler",
     "⚙  Genel görünüm ve ses ayarları"
   },(d,w)->{
     if(w==0)editChannel();
     else if(w==1)bulkHideChannels();
     else if(w==2){hidden.clear();prefs.edit().remove("hidden").apply();filter();ui.invalidate();toast("Silinen kanallar geri getirildi");}
     else if(w==3){favsOnly=!favsOnly;selected=0;tab=0;filter();ui.invalidate();}
     else options();
   });
 }
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
 private void options(){
  neonMenu("EYMEN TV • GENEL AYARLAR",new String[]{
    "▣  Kanal logoları: "+(showChannelLogos?"Açık":"Kapalı"),
    "▤  EPG program bilgileri: "+(showEpg?"Açık":"Kapalı"),
    "◉  Sağ köşe EYMEN TV logosu: "+(showWatermark?"Açık":"Kapalı"),
    "◷  Saatlik web adresi ve logo: "+(hourlyBrandEnabled?"Açık":"Kapalı"),
    "★  Favoriye ekle / çıkar",
    "◖  Ses izi seç / tanılama",
    "≡  Program ayrıntıları",
    "▥  Menü şeffaflığı",
    "◴  Bilgi çubuğu süresi",
    "⟳  Kanal listesini yenile",
    "⚿  İzin ayarları",
    "ⓘ  Hakkında",
    "▶  Sağ üst logo ve web adresini göster (test)"
  },(d,w)->{
    switch(w){
      case 0:showChannelLogos=!showChannelLogos;prefs.edit().putBoolean("channelLogos",showChannelLogos).apply();ui.invalidate();break;
      case 1:showEpg=!showEpg;prefs.edit().putBoolean("showEpg",showEpg).apply();ui.invalidate();break;
      case 2:showWatermark=!showWatermark;prefs.edit().putBoolean("showWatermark",showWatermark).apply();ui.invalidate();break;
      case 3:hourlyBrandEnabled=!hourlyBrandEnabled;
        prefs.edit().putBoolean("hourlyBrand",hourlyBrandEnabled).apply();
        if(!hourlyBrandEnabled)brandStartedUptime=-1L;ui.invalidate();break;
      case 4:toggleFavorite();break;
      case 5:soundDialog();break;
      case 6:details();break;
      case 7:new AlertDialog.Builder(this).setTitle("Menü yoğunluğu")
        .setSingleChoiceItems(new String[]{"Hafif","Dengeli","Koyu"},opacity<170?0:opacity<215?1:2,
        (a,n)->{opacity=new int[]{145,185,225}[n];prefs.edit().putInt("opacity",opacity).apply();ui.invalidate();a.dismiss();}).show();break;
      case 8:new AlertDialog.Builder(this).setTitle("Bilgi çubuğu süresi")
        .setItems(new String[]{"3 saniye","5 saniye","8 saniye"},
        (a,n)->{hideSeconds=new int[]{3,5,8}[n];prefs.edit().putInt("hideSeconds",hideSeconds).apply();showInfo();}).show();break;
      case 9:loadChannels();break;
      case 10:startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.parse("package:"+getPackageName())));break;
      case 12:menu=false;info=false;startBrand(true);break;
      default:new AlertDialog.Builder(this).setTitle("EYMEN TV • Hakkında")
        .setMessage("Atasoy Yazılım\nwww.atasoyyazilim.com\n"+audioState+
          "\nTV tuner erişimi ve ses desteği cihaz üreticisine göre değişebilir.")
        .setPositiveButton("Tamam",null).show();
    }
  });
 }
 private void details(){new AlertDialog.Builder(this).setTitle(now==null?"Program bilgisi":now.title).setMessage(now==null?"Bu kanal için güncel yayın bilgisi bulunamadı.":time(now.start)+" – "+time(now.end)+"\n\n"+(now.description==null?"Açıklama bulunamadı.":now.description)).setPositiveButton("Kapat",null).show();}
 private void step(int delta){ArrayList<Channel> data=visible;if(data.isEmpty()){toast("Bu listede kanal yok");return;}int i=data.indexOf(current);if(i<0)i=delta>0?-1:0;i=(i+delta+data.size())%data.size();play(data.get(i));selected=Math.max(0,visible.indexOf(current));}
 @Override public boolean onKeyDown(int key,KeyEvent e){if(key>=KeyEvent.KEYCODE_0&&key<=KeyEvent.KEYCODE_9){if(digits.length()>=4)digits="";digits+=key-KeyEvent.KEYCODE_0;h.removeCallbacks(enterNumber);h.postDelayed(enterNumber,1500);ui.invalidate();return true;}switch(key){case KeyEvent.KEYCODE_DPAD_UP:if(menu){if(tab==2){tab=0;selected=Math.max(0,Math.min(visible.size()-1,(selected/10)*10+9));}else if(selected%10==0)tab=1;else selected--;}else step(-1);break;case KeyEvent.KEYCODE_DPAD_DOWN:if(menu){if(tab==1)tab=0;else if(tab==0&&(selected%10==9||selected==visible.size()-1))tab=2;else if(tab==0)selected=Math.min(visible.size()-1,selected+1);}else step(1);break;case KeyEvent.KEYCODE_DPAD_LEFT:case KeyEvent.KEYCODE_DPAD_RIGHT:if(menu){selected=PageNav.jump(selected,visible.size(),key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);tab=0;}else{menu=true;info=true;}break;case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:if(!digits.isEmpty()){h.removeCallbacks(enterNumber);enterNumber.run();}else if(menu){if(tab==2){settings();}else if(tab==1){favsOnly=!favsOnly;filter();}else if(!visible.isEmpty()){play(visible.get(selected));menu=false;showInfo();}}else{menu=true;info=true;selected=Math.max(0,visible.indexOf(current));}break;case KeyEvent.KEYCODE_CHANNEL_UP:step(1);break;case KeyEvent.KEYCODE_CHANNEL_DOWN:step(-1);break;case KeyEvent.KEYCODE_MENU:settings();break;case KeyEvent.KEYCODE_PROG_GREEN:case KeyEvent.KEYCODE_BOOKMARK:toggleFavorite();break;case KeyEvent.KEYCODE_PROG_BLUE:soundDialog();break;case KeyEvent.KEYCODE_INFO:case KeyEvent.KEYCODE_PROG_YELLOW:if(info&&!menu)details();else showInfo();break;case KeyEvent.KEYCODE_PROG_RED:menu=!menu;showInfo();break;case KeyEvent.KEYCODE_BACK:if(menu){menu=false;showInfo();}else new AlertDialog.Builder(this).setTitle("EYMEN TV kapatılsın mı?").setPositiveButton("Çık",(d,w)->finish()).setNegativeButton("İzlemeye devam et",null).show();break;default:return super.onKeyDown(key,e);}ui.invalidate();return true;}
 @Override protected void onStart(){super.onStart();active=true;if(stoppedPlayback&&current!=null)play(current);}
 @Override protected void onStop(){active=false;brandStartedUptime=-1L;epoch++;tv.reset();audio.abandonAudioFocusRequest(focus);hasFocus=false;stoppedPlayback=current!=null;h.removeCallbacks(hide);super.onStop();}
 @Override protected void onDestroy(){dead=true;h.removeCallbacksAndMessages(null);worker.shutdownNow();logoWorker.shutdownNow();super.onDestroy();}
 private String time(long t){return new SimpleDateFormat("HH:mm",tr).format(new Date(t));}
 class Overlay extends View {
  final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  private static final int PINK=0xffff3c9b, BLUE=0xff19bfff, PURPLE=0xffb866ff;
  private static final int WHITE=0xffffffff, MUTED=0xffaebed7, GOLD=0xffffd67c;
  private static final int PANEL=0xff07162c, PALE=0xffd6e9fb;
  Overlay(){super(PlayerActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
  void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){
    p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
    p.setTextSize(size);p.setColor(color);p.setAlpha(255);
    c.drawText(s==null?"":s,x,y,p);
  }
  void fit(Canvas c,String s,float x,float y,float size,float width,int color,boolean bold){
    p.setShader(null);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextSize(size);
    String safe=s==null?"":s;
    if(p.measureText(safe)>width){while(safe.length()>1&&p.measureText(safe+"…")>width)safe=safe.substring(0,safe.length()-1);safe+="…";}
    text(c,safe,x,y,size,color,bold);
  }
  void fill(Canvas c,float l,float t,float r,float b,int color,float radius){
    p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(color);p.setAlpha(255);
    c.drawRoundRect(l,t,r,b,radius,radius,p);
  }
  void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width){
    p.setStyle(Paint.Style.STROKE);p.setShader(null);p.setStrokeWidth(width);p.setColor(color);
    c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);
  }
  void glass(Canvas c,float l,float t,float r,float b,boolean selected,float radius){
    p.setStyle(Paint.Style.FILL);p.setAlpha(255);
    p.setShader(new LinearGradient(l,t,r,b,new int[]{
      selected?0xeb2b1849:Color.argb(Math.min(246,opacity+37),5,18,42),
      selected?0xe5201647:Color.argb(Math.min(242,opacity+27),11,29,54),
      selected?0xee411044:Color.argb(Math.min(232,opacity+25),7,14,37)
    },null,Shader.TileMode.CLAMP));
    c.drawRoundRect(l,t,r,b,radius,radius,p);p.setShader(null);
    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(selected?3:1.3f);
    p.setColor(selected?PINK:0xff376294);c.drawRoundRect(l,t,r,b,radius,radius,p);
    if(selected){
      p.setStrokeWidth(1.1f);p.setColor(0xffa7dbff);
      c.drawRoundRect(l+3,t+3,r-3,b-3,Math.max(1,radius-2),Math.max(1,radius-2),p);
    }
    p.setStyle(Paint.Style.FILL);
  }
  void panel(Canvas c,float l,float t,float r,float b,boolean focus){glass(c,l,t,r,b,focus,15);}
  void pill(Canvas c,String label,float l,float t,float r,float b,boolean active,int color){
    glass(c,l,t,r,b,active,11);
    p.setTextSize(15);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
    float textWidth=p.measureText(label);
    text(c,label,l+(r-l-textWidth)*.5f,t+(b-t)*.67f,15,active?WHITE:MUTED,true);
  }
  void logo(Canvas c,Channel ch,float x,float y,float w,float ht){
    if(!showChannelLogos)return;
    requestLogo(ch);
    p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setAlpha(255);
    if(showChannelLogos&&ch.logo!=null&&ch.logo.getWidth()>0&&ch.logo.getHeight()>0){
      float scale=Math.min(w/ch.logo.getWidth(),ht/ch.logo.getHeight());
      float bw=ch.logo.getWidth()*scale,bh=ch.logo.getHeight()*scale;
      c.drawBitmap(ch.logo,null,new RectF(x+(w-bw)/2,y+(ht-bh)/2,x+(w+bw)/2,y+(ht+bh)/2),p);
    }else{
      fill(c,x,y,x+w,y+ht,0xff18406c,9);
      fit(c,ch.name,x+6,y+ht*.64f,14,w-12,WHITE,true);
    }
  }
  void drawHeader(Canvas c){
    glass(c,16,12,1264,95,false,21);
    fill(c,34,27,92,79,0xfff52a75,14);
    text(c,"▶",49,65,28,WHITE,true);
    text(c,"EYMEN",103,63,32,WHITE,true);
    text(c,"TV",253,63,32,PINK,true);
    text(c,"CANLI YAYIN  •  KANALLAR  •  TEK UYGULAMA",337,61,17,MUTED,true);
    line(c,873,34,873,76,0xff356089,2);
    text(c,time(System.currentTimeMillis()),898,58,24,WHITE,true);
    fit(c,new SimpleDateFormat("d MMMM EEEE",tr).format(new Date()),995,55,17,248,MUTED,false);
    line(c,899,74,1238,74,PINK,2);
  }
  void drawMenu(Canvas c){
    glass(c,18,105,404,694,false,20);
    text(c,"KANAL LİSTESİ",40,146,26,WHITE,true);
    text(c,visible.isEmpty()?"0 / 0":(selected+1)+" / "+visible.size(),301,146,17,GOLD,true);
    pill(c,"Tüm Kanallar",38,157,208,196,!favsOnly,PINK);
    pill(c,"★ Favoriler",215,157,382,196,favsOnly,PINK);
    final int start=(selected/10)*10;
    for(int k=0;k<10&&start+k<visible.size();k++){
      int i=start+k;
      Channel ch=visible.get(i);
      float y=205+k*39f;
      glass(c,34,y,387,y+36,i==selected&&tab==0,9);
      text(c,ch.num,44,y+24,15,WHITE,true);
      logo(c,ch,99,y+4,55,28);
      fit(c,ch.name,161,y+24,17,169,WHITE,true);
      text(c,favorites.contains(favoriteKey(ch))?"★":"☆",352,y+24,19,
        favorites.contains(favoriteKey(ch))?GOLD:0xff8498b3,false);
      if(ch==current){fill(c,374,y+14,380,y+20,BLUE,3);}
    }
    if(visible.isEmpty())fit(c,favsOnly?"Favori kanal yok":"Kanal bulunamadı",53,285,20,308,WHITE,false);
    glass(c,34,599,387,642,tab==2,11);
    text(c,"⚙  Kanal yönetimi ve ayarlar",47,627,19,WHITE,true);
    text(c,"◀  Sayfa "+(visible.isEmpty()?0:selected/10+1)+" / "+((visible.size()+9)/10)+"  ▶",50,666,16,MUTED,false);
    text(c,visible.size()+" kanal",300,666,15,GOLD,true);
  }
  void drawProgram(Canvas c){
    float left=menu?421:34;
    float top=menu?519:537;
    glass(c,left,top,1258,682,false,18);
    if(current==null){
      text(c,"EYMEN TV",left+22,top+39,24,WHITE,true);
      fit(c,message.isEmpty()?"Kumandadan bir kanal seçin.":message,left+22,top+79,18,720,MUTED,false);
      text(c,"EPG • Kanal bilgisi",left+22,top+112,15,BLUE,true);
      return;
    }
    logo(c,current,left+18,top+19,menu?90:110,58);
    float x=left+(menu?128:151);
    fit(c,current.name,x,top+35,27,menu?360:620,WHITE,true);
    fit(c,now==null?"Program bilgisi henüz yok":now.title,x,top+73,21,menu?365:620,PALE,true);
    text(c,now==null?"EPG yükleniyor":time(now.start)+" - "+time(now.end)+"  •  Devam ediyor",
      x,top+104,16,BLUE,false);
    float progress=0f;
    if(now!=null&&now.end>now.start)
      progress=Math.max(0,Math.min(1,(System.currentTimeMillis()-now.start)/(float)(now.end-now.start)));
    float width=menu?365:660;
    fill(c,x,top+118,x+width,top+125,0xff314565,4);
    if(progress>0)fill(c,x,top+118,x+width*progress,top+125,PINK,4);
    if(menu){
      line(c,990,top+18,990,top+137,0xff335075,1);
      text(c,"SIRADAKİ",1011,top+34,14,BLUE,true);
      fit(c,next==null?"Program bilgisi yok":next.title,1011,top+67,17,219,WHITE,true);
      if(next!=null)text(c,time(next.start)+" - "+time(next.end),1011,top+92,15,MUTED,false);
    }else{
      text(c,"Şimdi",x+width+24,top+96,15,GOLD,true);
    }
  }
  void drawPermanentWatermark(Canvas c){
    if(current==null||!showWatermark)return;
    glass(c,1052,107,1256,152,false,10);
    text(c,"▣",1062,138,25,PINK,true);
    text(c,"EYMEN",1094,137,21,WHITE,true);
    text(c,"TV",1206,137,22,PINK,true);
  }
  void drawHourlyLogo(Canvas c){
    // Preserve existing schedule: rotating branding appears 15 seconds once per 60 minutes.
    if(brandStartedUptime>0&&current!=null&&!menu){
      long elapsed=SystemClock.uptimeMillis()-brandStartedUptime;
      if(elapsed>=0&&elapsed<BRAND_DURATION_MS){
        long spinMs=(SystemClock.uptimeMillis()-animationStart)%14000L;
        double angle=spinMs*Math.PI*2/14000.0;
        float fadeIn=Math.min(1f,elapsed/450f);
        float fadeOut=Math.min(1f,(BRAND_DURATION_MS-elapsed)/550f);
        int alpha=(int)(255*Math.max(0f,Math.min(fadeIn,fadeOut)));
        c.save();c.translate(1137f,226f);
        c.saveLayerAlpha(-120f,-64f,120f,64f,alpha);
        c.skew(0f,(float)Math.sin(angle)*.11f);
        float yaw=(float)Math.cos(angle);
        c.scale(yaw>=0f?Math.max(.06f,yaw):Math.min(-.06f,yaw),1f);
        if(brandLogo!=null){
          p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setAlpha(255);
          c.drawBitmap(brandLogo,null,new RectF(-113,-58,113,58),p);
        }else{
          text(c,"EYMEN TV",-105,0,29,WHITE,true);
          text(c,"www.atasoyyazilim.com",-106,35,15,BLUE,true);
        }
        c.restore();c.restore();
      }
    }
  }
  @Override protected void onDraw(Canvas c){
    super.onDraw(c);c.save();
    c.scale(getWidth()/1280f,getHeight()/720f);
    if(menu){drawHeader(c);drawMenu(c);}
    drawPermanentWatermark(c);
    if((info||menu)&&showEpg)drawProgram(c);
    if(menu){
      fit(c,"OK Kanalı aç  •  SAĞ/SOL 10'lu sayfa  •  YEŞİL Favori  •  MENU Ayarlar",
          435,706,15,800,WHITE,false);
    }else if(info){
      fit(c,"OK Kanal listesi  •  YEŞİL Favori  •  MAVİ Ses  •  MENU Ayarlar",
          44,710,15,1170,WHITE,false);
    }
    if(!message.isEmpty()&&current==null){
      glass(c,428,344,1222,414,false,14);
      fit(c,message,452,390,18,738,WHITE,false);
    }else if(!message.isEmpty()&&current!=null){
      glass(c,430,435,1222,501,false,14);
      fit(c,message,450,473,18,735,WHITE,false);
    }
    if(!digits.isEmpty()){
      glass(c,1080,165,1245,239,true,13);
      text(c,digits,1103,214,34,WHITE,true);
    }
    drawHourlyLogo(c);
    c.restore();
  }
 }
}
