package ir.hozurino.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int BG=Color.rgb(6,21,28), PANEL=Color.rgb(16,42,53), TEAL=Color.rgb(32,213,176), ORANGE=Color.rgb(255,162,75), WHITE=Color.WHITE, MUTED=Color.rgb(146,168,177);
    SharedPreferences pref; Store store; LinearLayout root; TextView clock, notice, enterCount, exitCount; Handler timer=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        pref=getSharedPreferences("profile",MODE_PRIVATE);store=new Store(this);
        if(!pref.contains("code")) setup(); else home();
    }

    TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(dp(8),dp(7),dp(8),dp(7));return t;
    }
    GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    LinearLayout page(){
        ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(16),dp(18),dp(18));root.setBackgroundColor(BG);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(root);setContentView(scroll);return root;
    }
    void addSpace(int h){Space s=new Space(this);root.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    EditText input(String hint,boolean password){
        EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(WHITE);e.setTextSize(16);e.setSingleLine(true);e.setGravity(Gravity.RIGHT);e.setTextDirection(View.TEXT_DIRECTION_RTL);e.setPadding(dp(16),0,dp(16),0);e.setBackground(bg(PANEL,18));
        if(password)e.setInputType(2|16);root.addView(e,new LinearLayout.LayoutParams(-1,dp(58)));addSpace(10);return e;
    }
    Button button(String label,int color){
        Button b=new Button(this);b.setText(label);b.setTextSize(16);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(BG);b.setAllCaps(false);b.setBackground(bg(color,20));root.addView(b,new LinearLayout.LayoutParams(-1,dp(60)));return b;
    }
    void brand(){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView logo=text("✓",26,BG,true);logo.setGravity(Gravity.CENTER);logo.setBackground(bg(TEAL,14));row.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.addView(text("حضورینو",24,WHITE,true));TextView sub=text("حضور و غیاب هوشمند",10,MUTED,false);sub.setPadding(dp(8),0,dp(8),0);titles.addView(sub);row.addView(titles,new LinearLayout.LayoutParams(0,-2,1));root.addView(row);
    }

    void setup(){
        page();addSpace(35);brand();addSpace(35);TextView h=text("فعال‌سازی روی گوشی شخصی",23,WHITE,true);h.setGravity(Gravity.CENTER);root.addView(h);
        TextView d=text("اطلاعات کاملاً آفلاین و فقط روی همین گوشی ذخیره می‌شود",12,MUTED,false);d.setGravity(Gravity.CENTER);root.addView(d);addSpace(25);
        EditText name=input("نام و نام خانوادگی",false),code=input("کد پرسنلی",false),pin=input("رمز شخصی ۴ تا ۸ رقم",true);
        code.setInputType(2);Button save=button("فعال‌سازی حضورینو",TEAL);
        save.setOnClickListener(v->{String n=name.getText().toString().trim(),c=code.getText().toString().trim(),p=pin.getText().toString();if(n.length()<3||c.length()<3||p.length()<4){toast("همه اطلاعات را کامل وارد کنید");return;}pref.edit().putString("name",n).putString("code",c).putString("pin",hash(c,p)).apply();home();});
    }

    void home(){
        page();brand();addSpace(14);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(12),dp(18),dp(12),dp(18));hero.setBackground(bg(PANEL,28));
        clock=text("",52,WHITE,true);clock.setGravity(Gravity.CENTER);hero.addView(clock);TextView date=text("",13,MUTED,false);date.setGravity(Gravity.CENTER);hero.addView(date);TextView offline=text("● کاملاً آفلاین",11,TEAL,true);offline.setGravity(Gravity.CENTER);hero.addView(offline);root.addView(hero);
        Runnable tick=new Runnable(){public void run(){Date n=new Date();clock.setText(new SimpleDateFormat("HH:mm",new Locale("fa")).format(n));date.setText(new SimpleDateFormat("EEEE، yyyy/MM/dd",new Locale("fa")).format(n));timer.postDelayed(this,1000);}};timer.removeCallbacksAndMessages(null);timer.post(tick);
        addSpace(14);root.addView(text("سلام، "+pref.getString("name",""),20,WHITE,true));root.addView(text("کد پرسنلی: "+pref.getString("code",""),12,MUTED,false));addSpace(12);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button enter=action("◎\nثبت ورود\nاثر انگشت",TEAL),exit=action("◎\nثبت خروج\nاثر انگشت",ORANGE);actions.addView(enter,new LinearLayout.LayoutParams(0,dp(145),1));Space gap=new Space(this);actions.addView(gap,new LinearLayout.LayoutParams(dp(10),1));actions.addView(exit,new LinearLayout.LayoutParams(0,dp(145),1));root.addView(actions);
        enter.setOnClickListener(v->biometric("ورود"));exit.setOnClickListener(v->biometric("خروج"));
        LinearLayout pins=new LinearLayout(this);pins.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);Button pe=small("ورود با رمز"),px=small("خروج با رمز");pins.addView(pe,new LinearLayout.LayoutParams(0,dp(48),1));pins.addView(px,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(pins);pe.setOnClickListener(v->pinDialog("ورود"));px.setOnClickListener(v->pinDialog("خروج"));
        notice=text("",14,TEAL,true);notice.setGravity(Gravity.CENTER);notice.setBackground(bg(PANEL,16));root.addView(notice,new LinearLayout.LayoutParams(-1,dp(50)));addSpace(12);
        LinearLayout stats=new LinearLayout(this);stats.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);enterCount=stat("ورود امروز");exitCount=stat("خروج امروز");stats.addView(enterCount,new LinearLayout.LayoutParams(0,dp(85),1));Space gs=new Space(this);stats.addView(gs,new LinearLayout.LayoutParams(dp(10),1));stats.addView(exitCount,new LinearLayout.LayoutParams(0,dp(85),1));root.addView(stats);updateStats();addSpace(14);
        Button logs=button("گزارش ترددها",TEAL);logs.setOnClickListener(v->history());addSpace(8);Button reset=small("تنظیمات و حذف پروفایل");root.addView(reset,new LinearLayout.LayoutParams(-1,dp(50)));reset.setOnClickListener(v->confirmReset());
    }
    Button action(String s,int c){Button b=new Button(this);b.setText(s);b.setTextSize(18);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(BG);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setBackground(bg(c,25));return b;}
    Button small(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEAL);b.setTextSize(13);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    TextView stat(String label){TextView t=text(label+"\n۰",15,WHITE,true);t.setGravity(Gravity.CENTER);t.setBackground(bg(PANEL,18));return t;}
    void updateStats(){int[] c=store.today();enterCount.setText("ورود امروز\n"+c[0]);exitCount.setText("خروج امروز\n"+c[1]);}

    void biometric(String type){
        try{
            BiometricPrompt prompt=new BiometricPrompt.Builder(this).setTitle("تأیید حضورینو").setSubtitle("انگشت خود را روی حسگر گوشی قرار دهید").setNegativeButton("انصراف",getMainExecutor(),(d,w)->{}).build();
            prompt.authenticate(new CancellationSignal(),getMainExecutor(),new BiometricPrompt.AuthenticationCallback(){
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){register(type,"اثر انگشت گوشی");}
                @Override public void onAuthenticationFailed(){toast("اثر انگشت شناخته نشد");}
                @Override public void onAuthenticationError(int c,CharSequence m){if(c!=5&&c!=10)toast(m.toString());}
            });
        }catch(Exception e){toast("اثر انگشت در تنظیمات گوشی فعال نیست");}
    }
    void pinDialog(String type){
        final EditText p=new EditText(this);p.setInputType(2|16);p.setHint("رمز شخصی");p.setGravity(Gravity.RIGHT);
        new AlertDialog.Builder(this).setTitle("ثبت "+type+" با رمز").setView(p).setNegativeButton("انصراف",null).setPositiveButton("تأیید",(d,w)->{String c=pref.getString("code","");if(hash(c,p.getText().toString()).equals(pref.getString("pin","")))register(type,"رمز شخصی");else toast("رمز نادرست است");}).show();
    }
    void register(String type,String method){if(store.add(type,method,pref.getString("name",""))){notice.setText(type+" با موفقیت ثبت شد");notice.setTextColor(TEAL);updateStats();}else{notice.setText("ثبت تکراری؛ یک دقیقه صبر کنید");notice.setTextColor(ORANGE);}}
    void history(){
        page();brand();addSpace(15);root.addView(text("گزارش ترددها",22,WHITE,true));addSpace(10);Cursor c=store.logs();if(c.getCount()==0)root.addView(text("هنوز ترددی ثبت نشده",15,MUTED,false));
        while(c.moveToNext()){LinearLayout card=new LinearLayout(this);card.setPadding(dp(14),dp(10),dp(14),dp(10));card.setBackground(bg(PANEL,16));TextView info=text(c.getString(1)+" · "+c.getString(2),15,c.getString(1).equals("ورود")?TEAL:ORANGE,true);card.addView(info,new LinearLayout.LayoutParams(0,-2,1));card.addView(text(new SimpleDateFormat("yyyy/MM/dd  HH:mm",new Locale("fa")).format(new Date(c.getLong(3))),12,MUTED,false));root.addView(card);addSpace(7);}c.close();addSpace(10);Button back=button("بازگشت",TEAL);back.setOnClickListener(v->home());
    }
    void confirmReset(){new AlertDialog.Builder(this).setTitle("حذف اطلاعات").setMessage("پروفایل و تمام گزارش‌های این گوشی حذف شوند؟").setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->{store.clear();pref.edit().clear().apply();setup();}).show();}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    String hash(String code,String pin){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(("hozurino:"+code+":"+pin).getBytes());StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}catch(Exception e){return "";}}
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}

    static class Store extends SQLiteOpenHelper{
        Store(Context c){super(c,"hozurino.db",null,1);}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE logs(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT,method TEXT,name TEXT,time INTEGER)");}
        public void onUpgrade(SQLiteDatabase d,int o,int n){}
        boolean add(String type,String method,String name){long now=System.currentTimeMillis();Cursor c=getReadableDatabase().rawQuery("SELECT time FROM logs ORDER BY id DESC LIMIT 1",null);long last=c.moveToFirst()?c.getLong(0):0;c.close();if(now-last<60000)return false;ContentValues v=new ContentValues();v.put("type",type);v.put("method",method);v.put("name",name);v.put("time",now);return getWritableDatabase().insert("logs",null,v)>0;}
        Cursor logs(){return getReadableDatabase().rawQuery("SELECT id,type,method,time FROM logs ORDER BY id DESC LIMIT 500",null);}
        int[] today(){Calendar x=Calendar.getInstance();x.set(Calendar.HOUR_OF_DAY,0);x.set(Calendar.MINUTE,0);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);int[] out={0,0};Cursor c=getReadableDatabase().rawQuery("SELECT type,COUNT(*) FROM logs WHERE time>=? GROUP BY type",new String[]{String.valueOf(x.getTimeInMillis())});while(c.moveToNext()){if("ورود".equals(c.getString(0)))out[0]=c.getInt(1);else out[1]=c.getInt(1);}c.close();return out;}
        void clear(){getWritableDatabase().delete("logs",null,null);}
    }
}
