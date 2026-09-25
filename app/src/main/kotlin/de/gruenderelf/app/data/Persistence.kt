package de.gruenderelf.app.data
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import de.gruenderelf.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.UUID

@Entity(tableName="savegames",indices=[Index(value=["slot"],unique=true)])
data class Savegame(@PrimaryKey val id: Int,val slot: Int,val clubName: String,val season: Int,val matchday: Int,val leagueName: String,val difficulty: String,val updatedAt: Long,val worldJson: String,@ColumnInfo(defaultValue="2") val saveVersion: Int=2)
data class SaveSummary(val id: Int,val slot: Int,val clubName: String,val season: Int,val matchday: Int,val leagueName: String,val difficulty: String,val updatedAt: Long)
data class SaveHeader(val id: Int,val slot: Int,val clubName: String,val season: Int,val matchday: Int,val leagueName: String,val difficulty: String,val updatedAt: Long,val saveVersion: Int)
@Dao interface SaveDao {
 @Query("SELECT id,slot,clubName,season,matchday,leagueName,difficulty,updatedAt FROM savegames WHERE slot BETWEEN 1 AND 5 ORDER BY slot") fun summaries(): Flow<List<SaveSummary>>
 @Query("SELECT id,slot,clubName,season,matchday,leagueName,difficulty,updatedAt,saveVersion FROM savegames WHERE slot=:slot") suspend fun header(slot: Int): SaveHeader?
 @Query("SELECT length(CAST(worldJson AS BLOB)) FROM savegames WHERE slot=:slot") suspend fun jsonSize(slot: Int): Int?
 @Query("SELECT substr(CAST(worldJson AS BLOB),:offset,:count) FROM savegames WHERE slot=:slot") suspend fun jsonChunk(slot: Int,offset: Int,count: Int): ByteArray?
 /** Chunked reads avoid Android 8 CursorWindow's per-row size limit. */
 @Transaction suspend fun get(slot: Int): Savegame? {
  val h=header(slot)?:return null;val size=jsonSize(slot)?:error("Spielstanddaten fehlen.");require(size in 1..(128*1024*1024)){"Spielstandgröße ungültig."}
  val out=java.io.ByteArrayOutputStream(size);var offset=1
  while(out.size()<size){val bytes=jsonChunk(slot,offset,262144)?:error("Spielstand unvollständig.");check(bytes.isNotEmpty());out.write(bytes);offset+=bytes.size}
  return Savegame(h.id,h.slot,h.clubName,h.season,h.matchday,h.leagueName,h.difficulty,h.updatedAt,out.toString("UTF-8"),h.saveVersion)
 }
 @Upsert suspend fun put(save: Savegame)
 @Query("DELETE FROM savegames WHERE slot=:slot") suspend fun delete(slot: Int)
 @Query("DELETE FROM savegames WHERE slot=:slot OR slot=:backupSlot") suspend fun deleteWithBackup(slot:Int,backupSlot:Int)
}
@Database(entities=[Savegame::class],version=2,exportSchema=true)
abstract class SaveDatabase: RoomDatabase(){
 abstract fun saves(): SaveDao
 companion object {
  val MIGRATION_1_2=object: Migration(1,2){override fun migrate(db: SupportSQLiteDatabase){db.execSQL("ALTER TABLE savegames ADD COLUMN saveVersion INTEGER NOT NULL DEFAULT 2")}}
  fun open(context: Context,name: String="gruenderelf.db")=Room.databaseBuilder(context,SaveDatabase::class.java,name).setJournalMode(JournalMode.WRITE_AHEAD_LOGGING).addMigrations(MIGRATION_1_2).addCallback(object: Callback(){override fun onOpen(db: SupportSQLiteDatabase){super.onOpen(db);db.execSQL("PRAGMA synchronous=NORMAL")}}).build()
 }
}
private val Context.settings by preferencesDataStore(name="einstellungen")
class GameRepository(context: Context,private val db: SaveDatabase=SaveDatabase.open(context)){
 private val saveMutex=Mutex()
 private val settings=context.applicationContext.settings
 private val lastKey=intPreferencesKey("letzter_slot");private val fastKey=booleanPreferencesKey("schnelles_spiel");private val speedKey=stringPreferencesKey("spieltempo_v044");private val soundKey=booleanPreferencesKey("match_sounds_v0464");private val changelogKey=stringPreferencesKey("changelog_seen_version")
 private val developerHashKey=stringPreferencesKey("developer_password_hash_v1");private val developerSaltKey=stringPreferencesKey("developer_password_salt_v1")
 val saves=db.saves().summaries()
 val lastSlot=settings.data.catch{emit(emptyPreferences())}.map{it[lastKey]?:1}
 val matchSpeed=settings.data.catch{emit(emptyPreferences())}.map{prefs->prefs[speedKey]?.let{runCatching{MatchSpeed.valueOf(it)}.getOrNull()}?:if(prefs[fastKey]==true)MatchSpeed.FAST else MatchSpeed.NORMAL}
 val soundsEnabled=settings.data.catch{emit(emptyPreferences())}.map{prefs->prefs[soundKey]?:true}
 val changelogSeenVersion=settings.data.catch{emit(emptyPreferences())}.map{prefs->prefs[changelogKey]?:""}
 val developerPasswordConfigured=settings.data.catch{emit(emptyPreferences())}.map{prefs->!prefs[developerHashKey].isNullOrBlank()&&!prefs[developerSaltKey].isNullOrBlank()}
 private fun passwordHash(value:String,salt:String)=MessageDigest.getInstance("SHA-256").digest((salt+"|"+value).toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
 suspend fun setDeveloperPassword(value:String){require(value.length>=6){"Der Developer-Code braucht mindestens 6 Zeichen."};val salt=UUID.randomUUID().toString();settings.edit{it[developerSaltKey]=salt;it[developerHashKey]=passwordHash(value,salt)}}
 suspend fun verifyDeveloperPassword(value:String):Boolean{val prefs=settings.data.first();val salt=prefs[developerSaltKey]?:return false;val hash=prefs[developerHashKey]?:return false;return MessageDigest.isEqual(hash.toByteArray(),passwordHash(value,salt).toByteArray())}
 suspend fun developerSaveExists()=db.saves().header(BMW_DEVELOPER_SAVE_SLOT)!=null
 suspend fun setMatchSpeed(value: MatchSpeed){settings.edit{it[speedKey]=value.name;it[fastKey]=value==MatchSpeed.FAST}}
 suspend fun setSoundsEnabled(enabled: Boolean){settings.edit{it[soundKey]=enabled}}
 suspend fun markChangelogSeen(version:String){settings.edit{it[changelogKey]=version}}
 private fun backupSlot(slot:Int)=slot+100
 private suspend fun decodeOrNull(row:Savegame?):World? {
  if(row==null)return null
  // OutOfMemoryError darf NICHT wie ein kaputter Save behandelt werden. Sonst würde load()
  // zusätzlich noch das Backup laden und den Speicherdruck weiter erhöhen.
  return try{SaveCodec.decode(row.worldJson)}catch(e:Exception){null}
 }
 private fun utf8Size(text:String):Long {
  var bytes=0L;var i=0
  while(i<text.length){
   val c=text[i].code
   bytes+=when{c<=0x7F->1;c<=0x7FF->2;c in 0xD800..0xDBFF&&i+1<text.length&&text[i+1].code in 0xDC00..0xDFFF->{i++;4};else->3}
   i++
  }
  return bytes
 }
 suspend fun load(slot: Int): World=withContext(Dispatchers.IO){
  require(slot in 1..5||slot==BMW_DEVELOPER_SAVE_SLOT);val dao=db.saves();var primary=dao.get(slot);val hadPrimary=primary!=null;val primaryVersion=primary?.saveVersion
  var w=decodeOrNull(primary)
  // Die große JSON-Zeichenkette sofort freigeben, bevor ggf. ein Backup oder Re-Save folgt.
  primary=null
  if(w==null){
   var backup=dao.get(backupSlot(slot));w=decodeOrNull(backup)?:if(!hadPrimary)error("Dieser Speicherplatz ist leer.") else error("Spielstand und Sicherheitskopie sind beschädigt.")
   backup=null;dao.delete(slot);checkpoint(slot,w)
  }else if(primaryVersion!=SAVE_VERSION){save(slot,w)}
  // Kein encode(w)-Vergleich mehr beim Laden. Der erzeugte früher eine zweite Vollkopie des Saves.
  settings.edit{it[lastKey]=slot};w
 }
 private fun saveRow(slot:Int,w:World,payload:String)=Savegame(slot,slot,w.club().name,w.calendar.season,w.calendar.matchday,WorldFactory.leagueName(w,w.club().tier),w.user.difficulty.label,System.currentTimeMillis(),payload,SAVE_VERSION)
 suspend fun save(slot: Int,w: World)=saveMutex.withLock{withContext(Dispatchers.IO+NonCancellable){
  require(slot in 1..5||slot==BMW_DEVELOPER_SAVE_SLOT){"Ungültiger Speicherplatz."};SaveCodec.requireRuntimeIntegrity(w);val payload=SaveCodec.encode(w)
  require(utf8Size(payload)<=128L*1024*1024){"Der Spielstand überschreitet 128 MB."}
  // World ist bereits ein gültiger Engine-Zustand. Ein komplettes encode->decode nur zur Kontrolle
  // verdoppelte den Peak-RAM. Backup und Primärstand werden nun atomar aus demselben Payload geschrieben.
  val row=saveRow(slot,w,payload);val dao=db.saves();db.withTransaction{
   dao.put(row.copy(id=backupSlot(slot),slot=backupSlot(slot)))
   dao.put(row)
  }
  settings.edit{it[lastKey]=slot}
 }}
 /**
  * Schneller, atomarer Live-Checkpoint. Während einer Partie wird absichtlich nicht bei jedem
  * Checkpoint der komplette Primärstand erneut gelesen, dekodiert und als Backup kopiert.
  * Room/WAL schreibt die neue Zeile atomar; die letzte Vollsicherung bleibt als Backup erhalten.
  */
 suspend fun checkpoint(slot:Int,w:World)=saveMutex.withLock{withContext(Dispatchers.IO){
  require(slot in 1..5||slot==BMW_DEVELOPER_SAVE_SLOT);SaveCodec.requireRuntimeIntegrity(w);val payload=SaveCodec.encode(w)
  require(utf8Size(payload)<=128L*1024*1024){"Der Spielstand überschreitet 128 MB."}
  db.saves().put(saveRow(slot,w,payload));settings.edit{it[lastKey]=slot}
 }}
 suspend fun delete(slot: Int)=withContext(Dispatchers.IO){db.withTransaction{db.saves().deleteWithBackup(slot,backupSlot(slot))}}
}
