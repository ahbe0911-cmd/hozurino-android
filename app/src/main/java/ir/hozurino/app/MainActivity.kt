package ir.hozurino.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs

private val Ink=Color(0xFF102A36); private val Teal=Color(0xFF10AD91); private val Cream=Color(0xFFF4F7F5)
data class Employee(val name:String,val code:String,val descriptor:String)

class MainActivity:FragmentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{AttendanceApp(::biometric)}}
 private fun biometric(done:(Boolean,String)->Unit){
  val allowed=BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
  if(BiometricManager.from(this).canAuthenticate(allowed)!=BiometricManager.BIOMETRIC_SUCCESS){done(false,"اثر انگشت روی گوشی فعال نیست");return}
  val prompt=BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback(){
   override fun onAuthenticationSucceeded(r:BiometricPrompt.AuthenticationResult)=done(true,"اثر انگشت تأیید شد")
   override fun onAuthenticationError(c:Int,t:CharSequence)=done(false,t.toString())
  })
  prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("حضورینو").setSubtitle("برای ثبت تردد هویت را تأیید کنید").setAllowedAuthenticators(allowed).build())
 }
}

@Composable fun AttendanceApp(biometric:(((Boolean,String)->Unit)->Unit)){
 val ctx=LocalContext.current; val store=remember{OfflineStore(ctx)}
 var employee by remember{mutableStateOf(store.employee())}; var page by remember{mutableStateOf(if(employee==null)"enroll" else "kiosk")}
 var toast by remember{mutableStateOf("")}; var logs by remember{mutableStateOf(store.records())};var stats by remember{mutableStateOf(store.todayCount())}
 MaterialTheme(colorScheme=lightColorScheme(primary=Teal,background=Cream)){Surface(Modifier.fillMaxSize(),color=Cream){when(page){
  "enroll"->EnrollmentScreen{n,c,d->val e=Employee(n,c,d);store.saveEmployee(e);employee=e;page="kiosk"}
  "history"->HistoryScreen(logs){page="kiosk"}
  else->KioskScreen(employee!!,toast,stats,{page="history"},{msg->toast=msg},{
   val record=store.add(employee!!.name,it)
   if(record==null)toast="برای جلوگیری از ثبت تکراری، یک دقیقه صبر کنید" else{logs=store.records();stats=store.todayCount();toast=record.type+" "+employee!!.name+" با موفقیت ثبت شد"}
  },biometric)
 }}}}

@Composable fun EnrollmentScreen(done:(String,String,String)->Unit){
 var name by remember{mutableStateOf("")};var code by remember{mutableStateOf("")};var reading by remember{mutableStateOf(FaceReading())};val samples=remember{mutableStateListOf<String>()}
 Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Header("ثبت اولیه کارمند");Spacer(Modifier.height(16.dp))
  OutlinedTextField(name,{name=it},label={Text("نام و نام خانوادگی")},singleLine=true,modifier=Modifier.fillMaxWidth())
  Spacer(Modifier.height(8.dp));OutlinedTextField(code,{code=it.filter(Char::isDigit).take(8)},label={Text("کد پرسنلی")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
  Spacer(Modifier.height(14.dp));CameraBox{reading=it}
  Text(reading.hint,color=if(reading.live)Teal else Color.Gray,fontSize=12.sp,modifier=Modifier.padding(12.dp))
  LinearProgressIndicator(progress={samples.size/3f},modifier=Modifier.fillMaxWidth())
  Spacer(Modifier.height(8.dp));OutlinedButton(onClick={samples+=reading.descriptor},enabled=reading.live&&samples.size<3,modifier=Modifier.fillMaxWidth()){Text("ذخیره نمونه چهره "+(samples.size+1).coerceAtMost(3)+" از ۳")}
  Spacer(Modifier.height(8.dp));Button(onClick={done(name,code,averageDescriptors(samples))},enabled=name.length>2&&code.length>=4&&samples.size==3,modifier=Modifier.fillMaxWidth()){Text("فعال‌سازی آفلاین")}
  Text("برای هر نمونه صورت را کمی بچرخانید و لبخند بزنید.",fontSize=11.sp,color=Color.Gray,modifier=Modifier.padding(10.dp))
 }
}

@Composable fun KioskScreen(e:Employee,notice:String,stats:Pair<Int,Int>,history:()->Unit,setNotice:(String)->Unit,registered:(String)->Unit,biometric:(((Boolean,String)->Unit)->Unit)){
 var reading by remember{mutableStateOf(FaceReading())};var code by remember{mutableStateOf("")};var matched by remember{mutableStateOf(false)}
 LaunchedEffect(reading){if(reading.live&&faceDistance(e.descriptor,reading.descriptor)<0.17&&!matched){matched=true;registered("چهره")}}
 Column(Modifier.fillMaxSize()){
  Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Header("حضورینو");Spacer(Modifier.weight(1f));Text("● آفلاین",color=Teal,fontSize=11.sp,fontWeight=FontWeight.Bold);TextButton(onClick=history){Text("سوابق")}}
  Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   StatCard("ورود امروز",stats.first.toString(),Modifier.weight(1f));StatCard("خروج امروز",stats.second.toString(),Modifier.weight(1f))
  };Spacer(Modifier.height(10.dp))
  Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal=16.dp)){
   CameraBox(Modifier.fillMaxSize()){reading=it}
   Column(Modifier.align(Alignment.TopCenter).padding(top=18.dp).background(Color(0xCC102A36),RoundedCornerShape(18.dp)).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){
    Text(if(matched)"تشخیص موفق" else reading.hint,color=Color.White,fontWeight=FontWeight.Bold)
    if(matched)Text(e.name,color=Color(0xFF57E1C2),fontSize=20.sp,fontWeight=FontWeight.Black)
   }
  }
  if(notice.isNotBlank())Text(notice,color=Teal,fontWeight=FontWeight.Bold,modifier=Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
  Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   Button(onClick={biometric{ok,msg->setNotice(msg);if(ok)registered("اثر انگشت")}},modifier=Modifier.weight(1f)){Text("اثر انگشت")}
   OutlinedButton(onClick={matched=false;setNotice("")},modifier=Modifier.weight(1f)){Text("اسکن دوباره")}
  }
  OutlinedTextField(code,{code=it.filter(Char::isDigit).take(8)},label={Text("کد پرسنلی")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,trailingIcon={TextButton(onClick={if(code==e.code)registered("کد") else setNotice("کد نادرست است")}){Text("ثبت")}},modifier=Modifier.fillMaxWidth().padding(16.dp))
 }
}

@Composable fun HistoryScreen(logs:List<AttendanceRecord>,back:()->Unit){Column(Modifier.fillMaxSize().padding(20.dp)){Row(verticalAlignment=Alignment.CenterVertically){Header("سوابق آفلاین");Spacer(Modifier.weight(1f));TextButton(onClick=back){Text("بازگشت")}};Spacer(Modifier.height(20.dp));if(logs.isEmpty())Text("هنوز ترددی ثبت نشده",color=Color.Gray) else logs.forEach{r->Card(Modifier.fillMaxWidth().padding(vertical=5.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Row(Modifier.padding(16.dp)){Column(Modifier.weight(1f)){Text(r.employee,fontWeight=FontWeight.Bold);Text(r.type+" · "+r.method,color=if(r.type=="ورود")Teal else Color(0xFFE07A35),fontSize=12.sp)};Text(SimpleDateFormat("yyyy/MM/dd  HH:mm",Locale("fa")).format(Date(r.time)),fontSize=12.sp,color=Color.Gray)}}}}
@Composable fun StatCard(label:String,value:String,modifier:Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(12.dp)){Text(label,fontSize=11.sp,color=Color.Gray);Text(value,fontSize=24.sp,fontWeight=FontWeight.Black,color=Ink)}}}
@Composable fun Header(title:String){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(Ink,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Text("✓",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Black)};Spacer(Modifier.width(10.dp));Text(title,fontSize=22.sp,fontWeight=FontWeight.Black,color=Ink)}}
