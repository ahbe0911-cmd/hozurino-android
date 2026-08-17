package ir.hozurino.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

data class FaceReading(val found:Boolean=false,val descriptor:String="",val live:Boolean=false,val hint:String="صورت را داخل کادر قرار دهید")

@Composable fun CameraBox(modifier:Modifier=Modifier.fillMaxWidth().height(360.dp),onFace:(FaceReading)->Unit){
 val ctx=LocalContext.current;var granted by remember{mutableStateOf(ContextCompat.checkSelfPermission(ctx,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it};LaunchedEffect(Unit){if(!granted)launcher.launch(Manifest.permission.CAMERA)}
 Box(modifier.background(Color(0xFF102A36),RoundedCornerShape(28.dp)),contentAlignment=Alignment.Center){
  if(granted)AndroidView(factory={context->PreviewView(context).apply{scaleType=PreviewView.ScaleType.FILL_CENTER;startCamera(this,onFace)}},modifier=Modifier.fillMaxSize()) else Text("اجازه دوربین لازم است",color=Color.White)
 }
}

private fun startCamera(view:PreviewView,onFace:(FaceReading)->Unit){
 val future=ProcessCameraProvider.getInstance(view.context);val executor=Executors.newSingleThreadExecutor()
 future.addListener({
  val provider=future.get();val preview=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)}
  val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
  val options=FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build();val detector=FaceDetection.getClient(options)
  analysis.setAnalyzer(executor){proxy->val media=proxy.image;if(media==null){proxy.close();return@setAnalyzer}
   detector.process(InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)).addOnSuccessListener{faces->
    val face=faces.maxByOrNull{it.boundingBox.width()*it.boundingBox.height()}
    if(face==null)onFace(FaceReading()) else{val desc=descriptor(face);val live=(face.smilingProbability?:0f)>.65f;onFace(FaceReading(true,desc,live,if(live)"چهره زنده تأیید شد" else "برای تأیید لبخند بزنید"))}
   }.addOnCompleteListener{proxy.close()}
  }
  provider.unbindAll();provider.bindToLifecycle(view.context as FragmentActivity,CameraSelector.DEFAULT_FRONT_CAMERA,preview,analysis)
 },ContextCompat.getMainExecutor(view.context))
}

private fun descriptor(f:Face):String{
 val b=f.boundingBox;val w=b.width().toFloat().coerceAtLeast(1f);val h=b.height().toFloat().coerceAtLeast(1f)
 val ids=listOf(FaceLandmark.LEFT_EYE,FaceLandmark.RIGHT_EYE,FaceLandmark.NOSE_BASE,FaceLandmark.MOUTH_LEFT,FaceLandmark.MOUTH_RIGHT)
 return ids.flatMap{id->val p=f.getLandmark(id)?.position;listOf(((p?.x?:b.centerX().toFloat())-b.left)/w,((p?.y?:b.centerY().toFloat())-b.top)/h)}.joinToString(","){"%.4f".format(Locale.US,it)}
}
fun faceDistance(a:String,b:String):Double{val x=a.split(",").mapNotNull{it.toDoubleOrNull()};val y=b.split(",").mapNotNull{it.toDoubleOrNull()};if(x.size!=y.size||x.isEmpty())return 9.0;return x.zip(y).sumOf{abs(it.first-it.second)}/x.size}
fun averageDescriptors(items:List<String>):String{val rows=items.map{it.split(",").map(String::toDouble)};return rows.first().indices.joinToString(","){i->"%.4f".format(Locale.US,rows.map{it[i]}.average())}}
