package ir.hozurino.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val Bg=Color(0xFF06151C);private val Panel=Color(0xFF102A35);private val Teal=Color(0xFF20D5B0);private val Orange=Color(0xFFFFA24B);private val Muted=Color(0xFF92A8B1)

class MainActivity:FragmentActivity(){
 override fun onCreate(s:Bundle?){super.onCreate(s);setContent{App(::fingerprint)}}
 private fun fingerprint(done:(Boolean,String)->Unit){
  val strong=BiometricManager.Authenticators.BIOMETRIC_STRONG
  if(BiometricManager.from(this).canAuthenticate(strong)!=BiometricManager.BIOMETRIC_SUCCESS){done(false,"ابتدا اثر انگشت را در تنظیمات گوشی فعال کنید");return}
  val p=BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback(){
   override fun onAuthenticationSucceeded(r:BiometricPrompt.AuthenticationResult)=done(true,"تأیید شد")
   override fun onAuthenticationError(c:Int,t:CharSequence)=done(false,t.toString())
   override fun onAuthenticationFailed()=done(false,"اثر انگشت شناخته نشد")
  })
  p.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("تأیید حضورینو").setSubtitle("انگشت خود را روی حسگر گوشی قرار دهید").setNegativeButtonText("انصراف").setAllowedAuthenticators(strong).build())
 }
}

@Composable fun App(auth:((Boolean,String)->Unit)->Unit){
 val db=remember{OfflineStore(LocalContext.current)};var user by remember{mutableStateOf(db.profile())};var page by remember{mutableStateOf(if(user==null)"setup" else "home")};var records by remember{mutableStateOf(db.records())};var counts by remember{mutableStateOf(db.todayCount())};var msg by remember{mutableStateOf("")};var action by remember{mutableStateOf("ورود")}
 fun save(type:String,method:String){val r=db.add(user!!,type);msg=if(r==null)"ثبت تکراری؛ یک دقیقه صبر کنید" else type+" با موفقیت ثبت شد";records=db.records();counts=db.todayCount()}
 MaterialTheme(colorScheme=darkColorScheme(primary=Teal,background=Bg,surface=Panel)){Surface(Modifier.fillMaxSize(),color=Bg){when(page){
  "setup"->Setup{n,c,p->db.saveProfile(n,c,p);user=db.profile();page="home"}
  "logs"->Logs(records){page="home"}
  "settings"->Settings(user!!,{page="home"}){db.reset();user=null;page="setup"}
  "pin"->Pin(action,{page="home"}){if(db.verifyPin(user!!,it)){save(action,"رمز شخصی");page="home"}else msg="رمز شخصی نادرست است"}
  else->Home(user!!,counts,msg,{page="logs"},{page="settings"},{type->action=type;auth{ok,text->if(ok)save(type,"اثر انگشت گوشی") else msg=text}},{type->action=type;page="pin"})
 }}}}

@Composable fun Setup(done:(String,String,String)->Unit){var n by remember{mutableStateOf("")};var c by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Brand();Spacer(Modifier.height(28.dp));Text("فعال‌سازی گوشی شخصی",fontSize=23.sp,fontWeight=FontWeight.Black);Text("کاملاً آفلاین؛ اطلاعات فقط روی همین گوشی",color=Muted,fontSize=12.sp,modifier=Modifier.padding(8.dp));Input(n,{n=it},"نام و نام خانوادگی");Input(c,{c=it.filter(Char::isDigit).take(12)},"کد پرسنلی",true);OutlinedTextField(p,{p=it.filter(Char::isDigit).take(8)},label={Text("رمز شخصی ۴ تا ۸ رقم")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true,modifier=Modifier.fillMaxWidth().padding(vertical=7.dp));Button({done(n,c,p)},enabled=n.length>2&&c.length>=3&&p.length>=4,modifier=Modifier.fillMaxWidth().height(56.dp)){Text("فعال‌سازی حضورینو",fontWeight=FontWeight.Black)}}}

@Composable fun Home(u:Employee,c:Pair<Int,Int>,msg:String,logs:()->Unit,settings:()->Unit,bio:(String)->Unit,pin:(String)->Unit){var now by remember{mutableStateOf(Date())};LaunchedEffect(Unit){while(true){now=Date();delay(1000)}};Column(Modifier.fillMaxSize().padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Brand();Spacer(Modifier.weight(1f));TextButton(settings){Text("تنظیمات",color=Muted)}};Card(Modifier.fillMaxWidth().padding(vertical=14.dp),shape=RoundedCornerShape(28.dp)){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(SimpleDateFormat("HH:mm",Locale("fa")).format(now),fontSize=54.sp,fontWeight=FontWeight.Black);Text(SimpleDateFormat("EEEE، yyyy/MM/dd",Locale("fa")).format(now),color=Muted);Text("● کاملاً آفلاین",color=Teal,fontSize=11.sp,modifier=Modifier.padding(top=10.dp))}};Text("سلام، "+u.name,fontSize=20.sp,fontWeight=FontWeight.Black);Text("کد پرسنلی: "+u.code,color=Muted,fontSize=12.sp);Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){Action("ثبت ورود",Teal,Modifier.weight(1f)){bio("ورود")};Action("ثبت خروج",Orange,Modifier.weight(1f)){bio("خروج")}};Row{TextButton({pin("ورود")},Modifier.weight(1f)){Text("ورود با رمز")};TextButton({pin("خروج")},Modifier.weight(1f)){Text("خروج با رمز")}};if(msg.isNotBlank())Text(msg,color=if(msg.contains("موفقیت"))Teal else Orange,textAlign=TextAlign.Center,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(15.dp)).padding(11.dp));Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){Stat("ورود امروز",c.first,Modifier.weight(1f));Stat("خروج امروز",c.second,Modifier.weight(1f))};Spacer(Modifier.weight(1f));OutlinedButton(logs,Modifier.fillMaxWidth()){Text("گزارش ترددها")};Text("اثر انگشت هرگز در برنامه ذخیره نمی‌شود",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(10.dp))}

}
@Composable fun Action(t:String,color:Color,m:Modifier,go:()->Unit){Button(go,m.height(130.dp),shape=RoundedCornerShape(25.dp),colors=ButtonDefaults.buttonColors(containerColor=color,contentColor=Bg)){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("◎",fontSize=42.sp,fontWeight=FontWeight.Black);Text(t,fontSize=19.sp,fontWeight=FontWeight.Black);Text("اثر انگشت",fontSize=10.sp)}}}
@Composable fun Stat(t:String,n:Int,m:Modifier){Card(m,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Text(t,color=Muted,fontSize=11.sp);Text(n.toString(),fontSize=25.sp,fontWeight=FontWeight.Black)}}}
@Composable fun Brand(){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(Teal,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Text("✓",color=Bg,fontSize=25.sp,fontWeight=FontWeight.Black)};Spacer(Modifier.width(9.dp));Column{Text("حضورینو",fontSize=22.sp,fontWeight=FontWeight.Black);Text("حضور و غیاب هوشمند",fontSize=9.sp,color=Muted)}}}
@Composable fun Input(v:String,set:(String)->Unit,label:String,num:Boolean=false){OutlinedTextField(v,set,label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(num)KeyboardType.Number else KeyboardType.Text),modifier=Modifier.fillMaxWidth().padding(vertical=7.dp))}
@Composable fun Pin(type:String,back:()->Unit,done:(String)->Unit){var p by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Brand();Spacer(Modifier.height(25.dp));Text("ثبت "+type+" با رمز",fontSize=22.sp,fontWeight=FontWeight.Black);OutlinedTextField(p,{p=it.filter(Char::isDigit).take(8)},label={Text("رمز شخصی")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),modifier=Modifier.fillMaxWidth().padding(vertical=15.dp));Button({done(p)},enabled=p.length>=4,modifier=Modifier.fillMaxWidth()){Text("تأیید و ثبت "+type)};TextButton(back){Text("انصراف")}}
}
@Composable fun Settings(u:Employee,back:()->Unit,reset:()->Unit){Column(Modifier.fillMaxSize().padding(20.dp)){Row{Brand();Spacer(Modifier.weight(1f));TextButton(back){Text("بازگشت")}};Card(Modifier.fillMaxWidth().padding(vertical=20.dp)){Column(Modifier.padding(18.dp)){Text(u.name,fontSize=20.sp,fontWeight=FontWeight.Black);Text("کد پرسنلی: "+u.code,color=Muted)}};Text("احراز با اثر انگشت گوشی یا رمز شخصی",color=Muted);Spacer(Modifier.weight(1f));OutlinedButton(reset,Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("حذف پروفایل و گزارش‌ها")}}
}
@Composable fun Logs(rows:List<AttendanceRecord>,back:()->Unit){Column(Modifier.fillMaxSize().padding(18.dp)){Row{Text("گزارش ترددها",fontSize=22.sp,fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));TextButton(back){Text("بازگشت")}};if(rows.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("هنوز ترددی ثبت نشده",color=Muted)}else LazyColumn{items(rows){r->Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Row(Modifier.padding(15.dp)){Column(Modifier.weight(1f)){Text(r.type,fontWeight=FontWeight.Black,color=if(r.type=="ورود")Teal else Orange);Text(r.method,color=Muted,fontSize=11.sp)};Text(SimpleDateFormat("yyyy/MM/dd HH:mm",Locale("fa")).format(Date(r.time)),color=Muted)}}}}}}
