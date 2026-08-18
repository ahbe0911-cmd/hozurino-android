package ir.hozurino.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.hardware.biometrics.BiometricPrompt;
import android.provider.MediaStore;
import android.webkit.*;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    WebView web;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff1e40af);
        web=new WebView(this);
        web.setBackgroundColor(0xfff0f4f8);
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(),"Android");
        web.setWebViewClient(new WebViewClient());
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
    class Bridge {
        @JavascriptInterface public void authenticate(String type){
            runOnUiThread(()->{
                try{
                    BiometricPrompt p=new BiometricPrompt.Builder(MainActivity.this)
                        .setTitle("تأیید حضورینو")
                        .setSubtitle("برای ثبت "+type+" انگشت خود را روی حسگر گوشی قرار دهید")
                        .setNegativeButton("انصراف",getMainExecutor(),(d,w)->result(type,false,"عملیات لغو شد"))
                        .build();
                    p.authenticate(new CancellationSignal(),getMainExecutor(),new BiometricPrompt.AuthenticationCallback(){
                        @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){result(type,true,"اثر انگشت تأیید شد");}
                        @Override public void onAuthenticationFailed(){result(type,false,"اثر انگشت شناخته نشد");}
                        @Override public void onAuthenticationError(int c,CharSequence m){if(c!=5&&c!=10)result(type,false,m.toString());}
                    });
                }catch(Exception e){result(type,false,"ابتدا اثر انگشت را در تنظیمات گوشی فعال کنید");}
            });
        }
        @JavascriptInterface public void saveCsv(String csv,String name){
            runOnUiThread(()->{
                try{
                    ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,name);v.put(MediaStore.Downloads.MIME_TYPE,"text/csv");v.put(MediaStore.Downloads.RELATIVE_PATH,"Download/Hozurino");
                    android.net.Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
                    try(OutputStream o=getContentResolver().openOutputStream(u)){o.write(("\ufeff"+csv).getBytes(StandardCharsets.UTF_8));}
                    Toast.makeText(MainActivity.this,"فایل در پوشه Download/Hozurino ذخیره شد",Toast.LENGTH_LONG).show();
                }catch(Exception e){Toast.makeText(MainActivity.this,"ذخیره فایل ناموفق بود",Toast.LENGTH_LONG).show();}
            });
        }
    }
    void result(String type,boolean ok,String message){
        String safe=message.replace("\\","\\\\").replace("'","\\'");
        web.evaluateJavascript("window.onBiometricResult('"+type+"',"+ok+",'"+safe+"')",null);
    }
}
