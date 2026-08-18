package ir.hozurino.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.nio.FloatBuffer
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

data class FaceReading(val found:Boolean=false,val descriptor:String="",val live:Boolean=false,val hint:String="صورت را داخل کادر قرار دهید",val score:Float=0f)

@Composable fun CameraBox(modifier:Modifier=Modifier.fillMaxWidth().height(360.dp),onFace:(FaceReading)->Unit){
 val ctx=LocalContext.current;var granted by remember{mutableStateOf(ContextCompat.checkSelfPermission(ctx,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it};LaunchedEffect(Unit){if(!granted)launcher.launch(Manifest.permission.CAMERA)}
 Box(modifier.background(ComposeColor(0xFF102A36),RoundedCornerShape(28.dp)),contentAlignment=Alignment.Center){
  if(granted)AndroidView(factory={context->PreviewView(context).apply{scaleType=PreviewView.ScaleType.FILL_CENTER;startCamera(this,onFace)}},modifier=Modifier.fillMaxSize()) else Text("اجازه دوربین لازم است",color=ComposeColor.White)
 }
}

private fun startCamera(view:PreviewView,onFace:(FaceReading)->Unit){
 val future=ProcessCameraProvider.getInstance(view.context);val executor=Executors.newSingleThreadExecutor();val recognizer=FaceRecognizer(view.context);val busy=AtomicBoolean(false)
 future.addListener({
  val provider=future.get();val preview=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)}
  val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
  analysis.setAnalyzer(executor){proxy->
   if(!busy.compareAndSet(false,true)){proxy.close();return@setAnalyzer}
   try{recognizer.process(proxy){reading->onFace(reading);busy.set(false)}}catch(e:Throwable){onFace(FaceReading(hint="خطای موتور چهره"));busy.set(false);proxy.close()}
  }
  provider.unbindAll();provider.bindToLifecycle(view.context as FragmentActivity,CameraSelector.DEFAULT_FRONT_CAMERA,preview,analysis)
 },ContextCompat.getMainExecutor(view.context))
}

private class FaceRecognizer(context:Context){
 private val detector=FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).setMinFaceSize(.16f).build())
 private val env=OrtEnvironment.getEnvironment();private val session:OrtSession=env.createSession(context.assets.open("mobilefacenet_fp32.onnx").readBytes())
 fun process(proxy:ImageProxy,done:(FaceReading)->Unit){
  val source=proxy.toBitmap();val rotated=rotate(source,proxy.imageInfo.rotationDegrees.toFloat(),true);proxy.close();if(rotated!==source)source.recycle()
  detector.process(InputImage.fromBitmap(rotated,0)).addOnSuccessListener{faces->
   val face=faces.maxByOrNull{it.boundingBox.width()*it.boundingBox.height()}
   if(face==null){rotated.recycle();done(FaceReading());return@addOnSuccessListener}
   Thread{try{
    val aligned=alignFace(rotated,face);val embedding=embedding(aligned);val live=(face.smilingProbability?:0f)>.55f&&(face.leftEyeOpenProbability?:0f)>.35f&&(face.rightEyeOpenProbability?:0f)>.35f
    aligned.recycle();rotated.recycle();done(FaceReading(true,embedding.joinToString(","){"%.6f".format(Locale.US,it)},live,if(live)"چهره زنده آماده شناسایی است" else "مستقیم نگاه کنید و لبخند بزنید",face.smilingProbability?:0f))
   }catch(e:Throwable){rotated.recycle();done(FaceReading(hint="مدل تشخیص اجرا نشد"))}}.start()
  }.addOnFailureListener{rotated.recycle();done(FaceReading(hint="صورت خوانده نشد"))}
 }
 private fun rotate(src:Bitmap,degrees:Float,mirror:Boolean):Bitmap{if(degrees==0f&&!mirror)return src;val m=Matrix().apply{postRotate(degrees);if(mirror)postScale(-1f,1f)};return Bitmap.createBitmap(src,0,0,src.width,src.height,m,true)}
 private fun alignFace(src:Bitmap,face:Face):Bitmap{
  val out=Bitmap.createBitmap(112,112,Bitmap.Config.ARGB_8888);val canvas=Canvas(out);canvas.drawColor(Color.BLACK);val paint=Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
  val le=face.getLandmark(FaceLandmark.LEFT_EYE)?.position;val re=face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
  if(le!=null&&re!=null){val m=Matrix();if(m.setPolyToPoly(floatArrayOf(le.x,le.y,re.x,re.y),0,floatArrayOf(38.2946f,51.6963f,73.5318f,51.5014f),0,2)){canvas.drawBitmap(src,m,paint);return out}}
  val b=face.boundingBox;val ex=(b.width()*.25f).toInt();val ey=(b.height()*.25f).toInt();val l=maxOf(0,b.left-ex);val t=maxOf(0,b.top-ey);val r=minOf(src.width,b.right+ex);val bo=minOf(src.height,b.bottom+ey)
  if(r>l&&bo>t){val crop=Bitmap.createBitmap(src,l,t,r-l,bo-t);val scaled=Bitmap.createScaledBitmap(crop,112,112,true);canvas.drawBitmap(scaled,0f,0f,paint);crop.recycle();if(scaled!==crop)scaled.recycle()};return out
 }
 private fun embedding(aligned:Bitmap):FloatArray{
  val px=IntArray(112*112);aligned.getPixels(px,0,112,0,0,112,112);val buf=FloatBuffer.allocate(3*112*112)
  for(c in 0..2)for(p in px){val v=when(c){0->(p shr 16)and 255;1->(p shr 8)and 255;else->p and 255};buf.put(v/127.5f-1f)};buf.rewind()
  OnnxTensor.createTensor(env,buf,longArrayOf(1,3,112,112)).use{tensor->session.run(mapOf("input" to tensor)).use{result->val raw=result[0].value;val e=when(raw){is Array<*>->raw[0] as FloatArray;is FloatArray->raw;else->error("bad model output")};var n=0f;e.forEach{n+=it*it};n=sqrt(n);return FloatArray(e.size){i->if(n>0)e[i]/n else e[i]}}}
 }
}

fun faceDistance(a:String,b:String):Double{val x=a.split(',').mapNotNull{it.toDoubleOrNull()};val y=b.split(',').mapNotNull{it.toDoubleOrNull()};if(x.size!=y.size||x.size<64)return 2.0;var dot=0.0;var nx=0.0;var ny=0.0;for(i in x.indices){dot+=x[i]*y[i];nx+=x[i]*x[i];ny+=y[i]*y[i]};return 1.0-dot/(sqrt(nx)*sqrt(ny)).coerceAtLeast(1e-9)}
fun averageDescriptors(items:List<String>):String{val rows=items.map{it.split(',').map(String::toDouble)};val avg=rows.first().indices.map{i->rows.map{it[i]}.average()};val n=sqrt(avg.sumOf{it*it});return avg.joinToString(","){"%.6f".format(Locale.US,it/n)}}
