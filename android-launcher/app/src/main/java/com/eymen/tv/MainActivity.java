package com.eymen.tv;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

/** TV launcher candidate; manufacturer controls HOME permissions. */
public class MainActivity extends Activity {
  private WebView web;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().getDecorView().setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    web=new WebView(this);
    web.setFocusable(true);
    web.setFocusableInTouchMode(true);
    setContentView(web);
    WebSettings settings=web.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(true);
    settings.setAllowFileAccessFromFileURLs(false);
    settings.setAllowUniversalAccessFromFileURLs(false);
    settings.setMediaPlaybackRequiresUserGesture(true);
    web.setWebChromeClient(new WebChromeClient());
    web.addJavascriptInterface(new NativeBridge(),"EymenNative");
    web.setWebViewClient(new WebViewClient(){
      private boolean handle(Uri uri) {
        if("file".equals(uri.getScheme()) && uri.toString().startsWith("file:///android_asset/"))return false;
        if("https".equals(uri.getScheme())) {
          try { startActivity(new Intent(Intent.ACTION_VIEW,uri)); } catch(Exception ignored) { }
        }
        return true;
      }
      @Override public boolean shouldOverrideUrlLoading(WebView view,String url) { return handle(Uri.parse(url)); }
      @Override public boolean shouldOverrideUrlLoading(WebView view,android.webkit.WebResourceRequest req) { return handle(req.getUrl()); }
    });
    web.loadUrl("file:///android_asset/index.html");
  }
  private void notifyUser(String message){runOnUiThread(()->Toast.makeText(this,message,Toast.LENGTH_SHORT).show());}
  private class NativeBridge {
    @JavascriptInterface public String listApps(){
      try{
        PackageManager pm=getPackageManager();
        Intent tv=new Intent(Intent.ACTION_MAIN);tv.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Intent phone=new Intent(Intent.ACTION_MAIN);phone.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps=new ArrayList<>();
        apps.addAll(pm.queryIntentActivities(tv,0));
        apps.addAll(pm.queryIntentActivities(phone,0));
        Collections.sort(apps,new Comparator<ResolveInfo>(){
          @Override public int compare(ResolveInfo a,ResolveInfo b){return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());}
        });
        JSONArray arr=new JSONArray(); HashSet<String> seen=new HashSet<>();
        for(ResolveInfo info:apps) {
          if(info.activityInfo==null)continue;
          String pkg=info.activityInfo.packageName;
          if(pkg.equals(getPackageName())||!seen.add(pkg))continue;
          JSONObject item=new JSONObject();
          item.put("packageName",pkg);
          item.put("label",info.loadLabel(pm).toString());
          arr.put(item);
        }
        return arr.toString();
      }catch(Exception e){return "[]";}
    }
    @JavascriptInterface public void launchPackage(String name) {
      if(name==null||!name.matches("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+"))return;
      runOnUiThread(()->{
        try{
          PackageManager pm=getPackageManager();
          Intent i=pm.getLeanbackLaunchIntentForPackage(name);
          if(i==null)i=pm.getLaunchIntentForPackage(name);
          if(i==null){notifyUser("Uygulama bulunamadı");return;}
          startActivity(i);
        }catch(Exception e){notifyUser("Uygulama açılamadı");}
      });
    }
    @JavascriptInterface public void homeSettings(){runOnUiThread(()->{
      try {startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}
      catch(Exception e){notifyUser("TV ana ekran seçimine izin vermiyor");}
    });}
    @JavascriptInterface public void systemSettings(){runOnUiThread(()->{
      try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }catch(Exception e){notifyUser("Ayarlar açılamadı");}
    });}
    @JavascriptInterface public void exitApp(){runOnUiThread(()->finish());}
  }
  @Override public void onBackPressed(){
    if(web==null){super.onBackPressed();return;}
    web.evaluateJavascript("window.EYMEN_TV && window.EYMEN_TV.back()",null);
  }
  @Override protected void onDestroy(){
    if(web!=null){web.removeJavascriptInterface("EymenNative");web.destroy();web=null;}
    super.onDestroy();
  }
}
