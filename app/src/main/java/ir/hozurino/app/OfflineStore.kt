package ir.hozurino.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AttendanceRecord(val id:Long,val employee:String,val method:String,val type:String,val time:Long)

class OfflineStore(context:Context):SQLiteOpenHelper(context,"hozurino_offline.db",null,1){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE profile(id INTEGER PRIMARY KEY CHECK(id=1), name TEXT NOT NULL, code TEXT NOT NULL, face TEXT NOT NULL)")
  db.execSQL("CREATE TABLE attendance(id INTEGER PRIMARY KEY AUTOINCREMENT, employee TEXT NOT NULL, method TEXT NOT NULL, type TEXT NOT NULL, time INTEGER NOT NULL)")
  db.execSQL("CREATE INDEX idx_attendance_time ON attendance(time DESC)")
 }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){}
 fun employee():Employee?=readableDatabase.rawQuery("SELECT name,code,face FROM profile WHERE id=1",null).use{
  if(!it.moveToFirst())null else Employee(it.getString(0),it.getString(1),it.getString(2))
 }
 fun saveEmployee(e:Employee){writableDatabase.insertWithOnConflict("profile",null,ContentValues().apply{put("id",1);put("name",e.name);put("code",e.code);put("face",e.descriptor)},SQLiteDatabase.CONFLICT_REPLACE)}
 fun records(limit:Int=200):List<AttendanceRecord>{
  val out=mutableListOf<AttendanceRecord>()
  readableDatabase.rawQuery("SELECT id,employee,method,type,time FROM attendance ORDER BY time DESC LIMIT ?",arrayOf(limit.toString())).use{c->
   while(c.moveToNext())out+=AttendanceRecord(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4))
  };return out
 }
 fun last():AttendanceRecord?=records(1).firstOrNull()
 fun add(employee:String,method:String):AttendanceRecord?{
  val now=System.currentTimeMillis();val previous=last()
  if(previous!=null&&now-previous.time<60_000)return null
  val type=if(previous?.type=="ورود")"خروج" else "ورود"
  val id=writableDatabase.insert("attendance",null,ContentValues().apply{put("employee",employee);put("method",method);put("type",type);put("time",now)})
  return AttendanceRecord(id,employee,method,type,now)
 }
 fun todayCount():Pair<Int,Int>{
  val start=java.util.Calendar.getInstance().apply{set(java.util.Calendar.HOUR_OF_DAY,0);set(java.util.Calendar.MINUTE,0);set(java.util.Calendar.SECOND,0);set(java.util.Calendar.MILLISECOND,0)}.timeInMillis
  var enter=0;var exit=0
  readableDatabase.rawQuery("SELECT type,COUNT(*) FROM attendance WHERE time>=? GROUP BY type",arrayOf(start.toString())).use{c->while(c.moveToNext()){if(c.getString(0)=="ورود")enter=c.getInt(1) else exit=c.getInt(1)}}
  return enter to exit
 }
 fun clear(){writableDatabase.delete("attendance",null,null)}
}
