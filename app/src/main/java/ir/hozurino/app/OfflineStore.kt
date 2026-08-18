package ir.hozurino.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

data class Employee(val id:Long=0,val name:String,val code:String,val descriptor:String,val active:Boolean=true)
data class AttendanceRecord(val id:Long,val employeeId:Long,val employee:String,val method:String,val type:String,val time:Long)

class OfflineStore(context:Context):SQLiteOpenHelper(context,"hozurino_offline.db",null,3){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,code TEXT NOT NULL UNIQUE,face TEXT NOT NULL,active INTEGER NOT NULL DEFAULT 1)")
  db.execSQL("CREATE TABLE attendance(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,employee TEXT NOT NULL,method TEXT NOT NULL,type TEXT NOT NULL,time INTEGER NOT NULL)")
  db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
  db.execSQL("INSERT INTO settings(key,value) VALUES('admin_pin','1357')")
  db.execSQL("CREATE INDEX idx_attendance_employee_time ON attendance(employee_id,time DESC)")
 }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
  if(oldVersion<2){
   db.execSQL("ALTER TABLE profile RENAME TO old_profile");db.execSQL("ALTER TABLE attendance RENAME TO old_attendance");onCreate(db)
   db.execSQL("INSERT INTO employees(name,code,face) SELECT name,code,face FROM old_profile")
   db.execSQL("INSERT INTO attendance(employee_id,employee,method,type,time) SELECT 1,employee,method,type,time FROM old_attendance")
   db.execSQL("DROP TABLE old_profile");db.execSQL("DROP TABLE old_attendance")
  }
  if(oldVersion<3){db.execSQL("DELETE FROM employees")}
 }
 fun employees():List<Employee>{val out=mutableListOf<Employee>();readableDatabase.rawQuery("SELECT id,name,code,face,active FROM employees ORDER BY name",null).use{c->while(c.moveToNext())out+=Employee(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)==1)};return out}
 fun saveEmployee(e:Employee):Boolean=try{writableDatabase.insertOrThrow("employees",null,ContentValues().apply{put("name",e.name.trim());put("code",e.code);put("face",e.descriptor);put("active",1)});true}catch(_:Exception){false}
 fun deleteEmployee(id:Long){writableDatabase.delete("employees","id=?",arrayOf(id.toString()))}
 fun adminPin():String=readableDatabase.rawQuery("SELECT value FROM settings WHERE key='admin_pin'",null).use{if(it.moveToFirst())it.getString(0) else "1357"}
 fun setAdminPin(pin:String){writableDatabase.insertWithOnConflict("settings",null,ContentValues().apply{put("key","admin_pin");put("value",pin)},SQLiteDatabase.CONFLICT_REPLACE)}
 fun records(limit:Int=500):List<AttendanceRecord>{val out=mutableListOf<AttendanceRecord>();readableDatabase.rawQuery("SELECT id,employee_id,employee,method,type,time FROM attendance ORDER BY time DESC LIMIT ?",arrayOf(limit.toString())).use{c->while(c.moveToNext())out+=AttendanceRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getLong(5))};return out}
 fun add(e:Employee,method:String):AttendanceRecord?{
  val now=System.currentTimeMillis();val previous=readableDatabase.rawQuery("SELECT id,employee_id,employee,method,type,time FROM attendance WHERE employee_id=? ORDER BY time DESC LIMIT 1",arrayOf(e.id.toString())).use{c->if(c.moveToFirst())AttendanceRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getLong(5)) else null}
  if(previous!=null&&now-previous.time<60_000)return null
  val type=if(previous?.type=="ورود")"خروج" else "ورود";val id=writableDatabase.insert("attendance",null,ContentValues().apply{put("employee_id",e.id);put("employee",e.name);put("method",method);put("type",type);put("time",now)})
  return AttendanceRecord(id,e.id,e.name,method,type,now)
 }
 fun todayCount():Pair<Int,Int>{val start=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis;var enter=0;var exit=0;readableDatabase.rawQuery("SELECT type,COUNT(*) FROM attendance WHERE time>=? GROUP BY type",arrayOf(start.toString())).use{c->while(c.moveToNext()){if(c.getString(0)=="ورود")enter=c.getInt(1) else exit=c.getInt(1)}};return enter to exit}
}
