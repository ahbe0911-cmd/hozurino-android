package ir.hozurino.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar
import java.security.MessageDigest

data class Employee(val id:Long=1,val name:String,val code:String,val pinHash:String)
data class AttendanceRecord(val id:Long,val employeeId:Long,val employee:String,val method:String,val type:String,val time:Long)

class OfflineStore(context:Context):SQLiteOpenHelper(context,"hozurino_offline.db",null,4){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE profile(id INTEGER PRIMARY KEY CHECK(id=1),name TEXT NOT NULL,code TEXT NOT NULL,pin_hash TEXT NOT NULL)")
  db.execSQL("CREATE TABLE attendance(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,employee TEXT NOT NULL,method TEXT NOT NULL,type TEXT NOT NULL,time INTEGER NOT NULL)")
  db.execSQL("CREATE INDEX idx_attendance_time ON attendance(time DESC)")
 }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
  db.execSQL("DROP TABLE IF EXISTS employees");db.execSQL("DROP TABLE IF EXISTS profile");db.execSQL("DROP TABLE IF EXISTS attendance");db.execSQL("DROP TABLE IF EXISTS settings");onCreate(db)
 }
 private fun hash(code:String,pin:String)=MessageDigest.getInstance("SHA-256").digest("hozurino:$code:$pin".toByteArray()).joinToString(""){"%02x".format(it)}
 fun profile():Employee?=readableDatabase.rawQuery("SELECT name,code,pin_hash FROM profile WHERE id=1",null).use{c->if(c.moveToFirst())Employee(name=c.getString(0),code=c.getString(1),pinHash=c.getString(2)) else null}
 fun saveProfile(name:String,code:String,pin:String){writableDatabase.insertWithOnConflict("profile",null,ContentValues().apply{put("id",1);put("name",name.trim());put("code",code);put("pin_hash",hash(code,pin))},SQLiteDatabase.CONFLICT_REPLACE)}
 fun verifyPin(e:Employee,pin:String)=e.pinHash==hash(e.code,pin)
 fun reset(){writableDatabase.delete("attendance",null,null);writableDatabase.delete("profile",null,null)}
 fun records(limit:Int=500):List<AttendanceRecord>{val out=mutableListOf<AttendanceRecord>();readableDatabase.rawQuery("SELECT id,employee_id,employee,method,type,time FROM attendance ORDER BY time DESC LIMIT ?",arrayOf(limit.toString())).use{c->while(c.moveToNext())out+=AttendanceRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getLong(5))};return out}
 fun add(e:Employee,type:String):AttendanceRecord?{val now=System.currentTimeMillis();val last=readableDatabase.rawQuery("SELECT time FROM attendance ORDER BY time DESC LIMIT 1",null).use{if(it.moveToFirst())it.getLong(0) else 0L};if(now-last<60_000)return null;val id=writableDatabase.insert("attendance",null,ContentValues().apply{put("employee_id",1);put("employee",e.name);put("method","اثر انگشت گوشی");put("type",type);put("time",now)});return AttendanceRecord(id,1,e.name,"اثر انگشت گوشی",type,now)}
 fun todayCount():Pair<Int,Int>{val start=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis;var enter=0;var exit=0;readableDatabase.rawQuery("SELECT type,COUNT(*) FROM attendance WHERE time>=? GROUP BY type",arrayOf(start.toString())).use{c->while(c.moveToNext()){if(c.getString(0)=="ورود")enter=c.getInt(1) else exit=c.getInt(1)}};return enter to exit}
}
