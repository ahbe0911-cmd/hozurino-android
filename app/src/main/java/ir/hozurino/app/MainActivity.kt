package ir.hozurino.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.text.SimpleDateFormat
import java.util.*

private val Ink = Color(0xFF102A36)
private val Teal = Color(0xFF10AD91)
private val Cream = Color(0xFFF5F8F6)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HozurinoApp(onBiometric = ::authenticateBiometric) }
    }

    private fun authenticateBiometric(onResult: (Boolean, String) -> Unit) {
        val manager = BiometricManager.from(this)
        val allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (manager.canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult(false, "اثر انگشت یا قفل امن روی گوشی فعال نیست")
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(true, "هویت بیومتریک تأیید شد")
                }
                override fun onAuthenticationError(code: Int, text: CharSequence) {
                    onResult(false, text.toString())
                }
            })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("ثبت ورود در حضورینو")
                .setSubtitle("اثر انگشت یا قفل امن گوشی را تأیید کنید")
                .setAllowedAuthenticators(allowed)
                .build()
        )
    }
}

@Composable
fun HozurinoApp(onBiometric: ((Boolean, String) -> Unit) -> Unit) {
    var screen by remember { mutableStateOf("home") }
    var employeeCode by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, background = Cream)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).background(Ink, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Text("✓", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column { Text("حضورینو", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink); Text("حضور دقیق، مدیریت آسان", fontSize = 11.sp, color = Color.Gray) }
                }
                Spacer(Modifier.height(42.dp))
                Text("سلام 👋", color = Teal, fontWeight = FontWeight.Bold)
                Text("ورودت رو ثبت کن", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(SimpleDateFormat("HH:mm  •  yyyy/MM/dd", Locale("fa")).format(Date()), color = Color.Gray)
                Spacer(Modifier.height(30.dp))

                when(screen) {
                    "home" -> {
                        EntryCard("تشخیص چهره", "با دوربین جلوی گوشی", "◉") { screen = "face" }
                        Spacer(Modifier.height(12.dp))
                        EntryCard("اثر انگشت", "تأیید امن توسط اندروید", "◎") {
                            onBiometric { ok, msg -> notice = msg; if (ok) screen = "success" }
                        }
                        Spacer(Modifier.height(12.dp))
                        EntryCard("کد پرسنلی", "ورود با کد اختصاصی", "#") { screen = "code" }
                    }
                    "face" -> FaceEnrollment(
                        onBack = { screen = "home" },
                        onContinue = { notice = "چهره برای ارسال امن به سرویس تشخیص آماده شد"; screen = "success" }
                    )
                    "code" -> CodeEntry(employeeCode, { employeeCode = it.filter(Char::isDigit).take(8) }, {
                        notice = "کد پرسنلی $employeeCode تأیید شد"; screen = "success"
                    }, { screen = "home" })
                    "success" -> SuccessCard(notice) { screen = "home"; employeeCode = "" }
                }
                if (notice.isNotBlank() && screen == "home") {
                    Spacer(Modifier.height(12.dp)); Text(notice, color = Color(0xFFB26A00), fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("اطلاعات اثر انگشت از گوشی خارج نمی‌شود.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun EntryCard(title: String, subtitle: String, icon: String, action: () -> Unit) {
    Card(onClick = action, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(Color(0xFFDFF8F1), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Text(icon, color = Teal, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 17.sp); Text(subtitle, color = Color.Gray, fontSize = 12.sp) }
            Text("←", color = Teal, fontSize = 22.sp)
        }
    }
}

@Composable
private fun CodeEntry(code: String, change: (String)->Unit, submit: ()->Unit, back: ()->Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("کد پرسنلی", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(value = code, onValueChange = change, label = { Text("کد ۴ تا ۸ رقمی") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Spacer(Modifier.height(16.dp)); Button(onClick = submit, enabled = code.length >= 4, modifier = Modifier.fillMaxWidth()) { Text("ثبت ورود") }
            TextButton(onClick = back) { Text("بازگشت") }
        }
    }
}

@Composable
private fun FaceEnrollment(onBack: ()->Unit, onContinue: ()->Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(170.dp).background(Color(0xFFDFF8F1), RoundedCornerShape(50.dp)), contentAlignment = Alignment.Center) { Text("☺", fontSize = 86.sp, color = Ink) }
            Spacer(Modifier.height(18.dp)); Text("صورت را مقابل دوربین بگیرید", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text("در نسخه نهایی تصویر با رضایت شما پردازش می‌شود.", color = Color.Gray, fontSize = 11.sp)
            Spacer(Modifier.height(18.dp)); Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("ادامه") }
            TextButton(onClick = onBack) { Text("بازگشت") }
        }
    }
}

@Composable
private fun SuccessCard(message: String, done: ()->Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(88.dp).background(Color(0xFFDFF8EF), RoundedCornerShape(44.dp)), contentAlignment = Alignment.Center) { Text("✓", color = Teal, fontSize = 48.sp) }
            Spacer(Modifier.height(16.dp)); Text("ورود با موفقیت ثبت شد", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Ink)
            Text(message, color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp)); Button(onClick = done) { Text("بازگشت به خانه") }
        }
    }
}
