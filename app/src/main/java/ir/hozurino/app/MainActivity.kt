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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.text.SimpleDateFormat
import java.util.*

private val Ink=Color(0xFF102A36);private val Teal=Color(0xFF00A98F);private val Bg=Color(0xFFF1F5F4);private val Orange=Color(0xFFFFA34D)

class MainActivity:FragmentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{Hozurino(::biometric)}}
 private fun biometric(done:(Boolean,String)->Unit){
  val allowed=BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
  if(BiometricManager.from(this).canAuthenticate(allowed)!=BiometricManager.BIOMETRIC_SUCCESS){done(false,"اثر انگشت این دستگاه فعال نیست");return}
  val p=BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback(){override fun onAuthenticationSucceeded(r:BiometricPrompt.AuthenticationResult)=done(true,"هویت تأیید شد");override fun onAuthenticationError(c:Int,t:CharSequence)=done(false,t.toString())})
  p.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("تأیید حضورینو").setSubtitle("اثر انگشت یا قفل امن دستگاه").setAllowedAuthenticators(allowed).build())
 }
}

@Composable fun Hozurino(biometric:(((Boolean,String)->Unit)->Unit)){
 val context=LocalContext.current;val store=remember{OfflineStore(context)};var page by remember{mutableStateOf("kiosk")};var employees by remember{mutableStateOf(store.employees())};var logs by remember{mutableStateOf(store.records())};var stats by remember{mutableStateOf(store.todayCount())};var notice by remember{mutableStateOf("")};var selected by remember{mutableStateOf<Employee?>(null)}
 fun register(e:Employee,method:String){val r=store.add(e,method);notice=if(r==null)"ثبت تکراری؛ یک دقیقه صبر کنید" else "${r.type} ${e.name} با موفقیت ثبت شد";logs=store.records();stats=store.todayCount()}
 MaterialTheme(colorScheme=lightColorScheme(primary=Teal,background=Bg)){Surface(Modifier.fillMaxSize(),color=Bg){when(page){
  "adminLogin"->AdminLogin(store,{page="kiosk"},{page="admin"},biometric)
  "admin"->AdminScreen(employees,store,{page="kiosk"},{page="enroll"},{id->store.deleteEmployee(id);employees=store.employees()})
  "enroll"->EnrollmentScreen({page="admin"}){n,c,d->if(store.saveEmployee(Employee(name=n,code=c,descriptor=d))){employees=store.employees();page="admin";notice="کارمند اضافه شد"}else notice="کد پرسنلی تکراری است"}
  "history"->HistoryScreen(logs){page="kiosk"}
  else->KioskScreen(employees,stats,notice,{page="history"},{page="adminLogin"},{e,m->register(e,m)})
 }}}
}

@Composable fun KioskScreen(employees:List<Employee>,stats:Pair<Int,Int>,notice:String,history:()->Unit,admin:()->Unit,register:(Employee,String)->Unit){
 var reading by remember{mutableStateOf(FaceReading())};var code by remember{mutableStateOf("")};var locked by remember{mutableStateOf(false)}
 LaunchedEffect(reading.descriptor){if(reading.live&&!locked&&employees.isNotEmpty()){val best=employees.minByOrNull{faceDistance(it.descriptor,reading.descriptor)};if(best!=null&&faceDistance(best.descriptor,reading.descriptor)<.42){locked=true;register(best,"MobileFaceNet")}}}
 Column(Modifier.fillMaxSize()){
  Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Header("حضورینو");Spacer(Modifier.weight(1f));Text("● کاملاً آفلاین",color=Teal,fontSize=11.sp,fontWeight=FontWeight.Bold);TextButton(onClick=history){Text("سوابق")};TextButton(onClick=admin){Text("مدیر")}}
  Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){StatCard("ورود امروز",stats.first.toString(),Modifier.weight(1f));StatCard("خروج امروز",stats.second.toString(),Modifier.weight(1f));StatCard("کارکنان",employees.size.toString(),Modifier.weight(1f))}
  Box(Modifier.fillMaxWidth().weight(1f).padding(16.dp)){CameraBox(Modifier.fillMaxSize()){reading=it};Box(Modifier.matchParentSize().padding(38.dp).background(Color.Transparent,RoundedCornerShape(42.dp)));Text(if(employees.isEmpty())"ابتدا از پنل مدیر کارمند اضافه کنید" else reading.hint,Modifier.align(Alignment.TopCenter).padding(18.dp).background(Color(0xCC102A36),RoundedCornerShape(18.dp)).padding(horizontal=18.dp,vertical=10.dp),color=Color.White,fontWeight=FontWeight.Bold)}
  if(notice.isNotBlank())Text(notice,Modifier.align(Alignment.CenterHorizontally).padding(8.dp),color=Teal,fontWeight=FontWeight.Bold)
  Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(code,{code=it.filter(Char::isDigit).take(8)},label={Text("کد پرسنلی")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f));Button(onClick={employees.find{it.code==code}?.let{register(it,"کد");code=""}},enabled=employees.any{it.code==code},modifier=Modifier.height(56.dp)){Text("ثبت با کد")}}
  OutlinedButton(onClick={locked=false},modifier=Modifier.fillMaxWidth().padding(16.dp)){Text("اسکن دوباره")}
 }
}

@Composable fun EnrollmentScreen(back:()->Unit,done:(String,String,String)->Unit){var name by remember{mutableStateOf("")};var code by remember{mutableStateOf("")};var reading by remember{mutableStateOf(FaceReading())};val samples=remember{mutableStateListOf<String>()};Column(Modifier.fillMaxSize().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Header("ثبت کارمند جدید");Spacer(Modifier.weight(1f));TextButton(onClick=back){Text("بازگشت")}};OutlinedTextField(name,{name=it},label={Text("نام و نام خانوادگی")},modifier=Modifier.fillMaxWidth());OutlinedTextField(code,{code=it.filter(Char::isDigit).take(8)},label={Text("کد پرسنلی یا PIN")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(10.dp));CameraBox(Modifier.fillMaxWidth().weight(1f)){reading=it};Text("نمونه ${samples.size} از ۳ — ${reading.hint}",color=if(reading.live)Teal else Color.Gray,modifier=Modifier.padding(8.dp));LinearProgressIndicator(progress={samples.size/3f},modifier=Modifier.fillMaxWidth());OutlinedButton({samples+=reading.descriptor},enabled=reading.live&&samples.size<3,modifier=Modifier.fillMaxWidth()){Text("گرفتن نمونه چهره")};Button({done(name,code,averageDescriptors(samples))},enabled=name.length>2&&code.length>=4&&samples.size==3,modifier=Modifier.fillMaxWidth()){Text("ذخیره کارمند در دستگاه")}}
}

@Composable fun AdminLogin(store:OfflineStore,back:()->Unit,ok:()->Unit,biometric:(((Boolean,String)->Unit)->Unit)){var pin by remember{mutableStateOf("")};var bad by remember{mutableStateOf(false)};Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Header("ورود مدیر");Spacer(Modifier.height(24.dp));OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(8)},label={Text("رمز مدیر")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));if(bad)Text("رمز نادرست است",color=MaterialTheme.colorScheme.error);Button({if(pin==store.adminPin())ok() else bad=true},Modifier.padding(8.dp)){Text("ورود با رمز")};OutlinedButton({biometric{success,_->if(success)ok()}},Modifier.padding(8.dp)){Text("اثر انگشت مدیر A54")};TextButton(onClick=back){Text("بازگشت به دستگاه")};Text("رمز اولیه: ۱۳۵۷",color=Color.Gray,fontSize=12.sp)}}

@Composable fun AdminScreen(employees:List<Employee>,store:OfflineStore,back:()->Unit,add:()->Unit,delete:(Long)->Unit){var newPin by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Header("مدیریت دستگاه");Spacer(Modifier.weight(1f));TextButton(onClick=back){Text("خروج")}};Button(add,Modifier.fillMaxWidth().padding(vertical=12.dp)){Text("+ افزودن کارمند و ثبت چهره")};Text("کارکنان (${employees.size})",fontWeight=FontWeight.Black,fontSize=18.sp);LazyColumn(Modifier.weight(1f)){items(employees){e->Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(e.name,fontWeight=FontWeight.Bold);Text("کد: ${e.code}",color=Color.Gray)};TextButton(onClick={delete(e.id)}){Text("حذف",color=MaterialTheme.colorScheme.error)}}}}};Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(newPin,{newPin=it.filter(Char::isDigit).take(8)},label={Text("رمز جدید مدیر")},modifier=Modifier.weight(1f));Button({store.setAdminPin(newPin);newPin=""},enabled=newPin.length>=4,modifier=Modifier.padding(start=8.dp)){Text("ذخیره")}}}
}

@Composable fun HistoryScreen(logs:List<AttendanceRecord>,back:()->Unit){Column(Modifier.fillMaxSize().padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Header("گزارش ترددها");Spacer(Modifier.weight(1f));TextButton(onClick=back){Text("بازگشت")}};if(logs.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("هنوز ترددی ثبت نشده",color=Color.Gray)}else LazyColumn{items(logs){r->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(r.employee,fontWeight=FontWeight.Bold);Text("${r.type} · ${r.method}",color=if(r.type=="ورود")Teal else Orange)};Text(SimpleDateFormat("yyyy/MM/dd\nHH:mm",Locale("fa")).format(Date(r.time)),color=Color.Gray)}}}}}}
@Composable fun StatCard(label:String,value:String,modifier:Modifier){Card(modifier){Column(Modifier.padding(10.dp)){Text(label,color=Color.Gray,fontSize=11.sp);Text(value,fontWeight=FontWeight.Black,fontSize=22.sp)}}}
@Composable fun Header(title:String){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(Ink,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Text("✓",color=Color.White,fontWeight=FontWeight.Black)};Spacer(Modifier.width(9.dp));Text(title,fontWeight=FontWeight.Black,fontSize=20.sp,color=Ink)}}
