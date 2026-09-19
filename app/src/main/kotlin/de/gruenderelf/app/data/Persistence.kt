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

@Entity(tableName="savegames",indices=[Index(value=["slot"],unique=true)])
data class Savegame(@PrimaryKey val id: Int,val slot: Int,val clubName: String,val season: Int,val matchday: Int,val leagueName: String,val difficulty: String,val updatedAt: Long,val worldJson: String,@ColumnInfo(defaultValue="2") val saveVersion: Int=2)
data class SaveSummary(val id: Int,val slot: Int,val clubName: String,val season: Int,val matchday: Int,val leagueName: String,val difficulty: String,val updatedAt: Long)
data class SaveHeader(val id: Int,val slot: Int,val clubName: String,val season: Int,val matchday: Int,val leagueName: String,val difficulty: String,val updatedAt: Long,val saveVersion: Int)
@Dao interface SaveDao {
 @Query("SELECT id,slot,clubName,season,matchday,leagueName,difficulty,updatedAt FROM savegames ORDER BY slot") fun summaries(): Flow<List<SaveSummary>>
 @Query("SELECT id,slot,clubName,season,matchday,leagueName,difficulty,updatedAt,saveVersion FROM savegames WHERE slot=:slot") suspend fun header(slot: Int): SaveHeader?
 @Query("SELECT length(CAST(worldJson AS BLOB)) FROM savegames WHERE slot=:slot") suspend fun jsonSize(slot: Int): Int?
 @Query("SELECT substr(CAST(worldJson AS BLOB),:offset,:count) FROM savegames WHERE slot=:slot") suspend fun jsonChunk(slot: Int,offset: Int,count: Int): ByteArray?
 /** Chunked reads avoid Android 8 CursorWindow's per-row size limit. */
 @Transaction suspend fun get(slot: Int): Savegame? {
  val h=header(slot)?:return null;val size=jsonSize(slot)?:error("Spielstanddaten fehlen.");require(size in 1..(32*1024*1024)){"Spielstandgröße ungültig."}
  val out=java.io.ByteArrayOutputStream(size);var offset=1
  while(out.size()<size){val bytes=jsonChunk(slot,offset,262144)?:error("Spielstand unvollständig.");check(bytes.isNotEmpty());out.write(bytes);offset+=bytes.size}
  return Savegame(h.id,h.slot,h.clubName,h.season,h.matchday,h.leagueName,h.difficulty,h.updatedAt,out.toString("UTF-8"),h.saveVersion)
 }
 @Upsert suspend fun put(save: Savegame)
 @Query("DELETE FROM savegames WHERE slot=:slot") suspend fun delete(slot: Int)
}
@Database(entities=[Savegame::class],version=2,exportSchema=true)
abstract class SaveDatabase: RoomDatabase(){
 abstract fun saves(): SaveDao
 companion object {
  val MIGRATION_1_2=object: Migration(1,2){override fun migrate(db: SupportSQLiteDatabase){db.execSQL("ALTER TABLE savegames ADD COLUMN saveVersion INTEGER NOT NULL DEFAULT 2")}}
  fun open(context: Context,name: String="gruenderelf.db")=Room.databaseBuilder(context,SaveDatabase::class.java,name).setJournalMode(JournalMode.WRITE_AHEAD_LOGGING).addMigrations(MIGRATION_1_2).addCallback(object: Callback(){override fun onOpen(db: SupportSQLiteDatabase){super.onOpen(db);db.execSQL("PRAGMA synchronous=FULL")}}).build()
 }
}
private val Context.settings by preferencesDataStore(name="einstellungen")
class GameRepository(context: Context,private val db: SaveDatabase=SaveDatabase.open(context)){
 private val settings=context.applicationContext.settings
 private val lastKey=intPreferencesKey("letzter_slot");private val fastKey=booleanPreferencesKey("schnelles_spiel");private val speedKey=stringPreferencesKey("spieltempo_v044");private val soundKey=booleanPreferencesKey("match_sounds_v0464")
 val saves=db.saves().summaries()
 val lastSlot=settings.data.catch{emit(emptyPreferences())}.map{it[lastKey]?:1}
 val matchSpeed=settings.data.catch{emit(emptyPreferences())}.map{prefs->prefs[speedKey]?.let{runCatching{MatchSpeed.valueOf(it)}.getOrNull()}?:if(prefs[fastKey]==true)MatchSpeed.FAST else MatchSpeed.NORMAL}
 val soundsEnabled=settings.data.catch{emit(emptyPreferences())}.map{prefs->prefs[soundKey]?:true}
 suspend fun setMatchSpeed(value: MatchSpeed){settings.edit{it[speedKey]=value.name;it[fastKey]=value==MatchSpeed.FAST}}
 suspend fun setSoundsEnabled(enabled: Boolean){settings.edit{it[soundKey]=enabled}}
 suspend fun load(slot: Int): World=withContext(Dispatchers.IO){require(slot in 1..5);val row=db.saves().get(slot)?:error("Dieser Speicherplatz ist leer.");val w=SaveCodec.decode(row.worldJson);if(row.saveVersion!=SAVE_VERSION||row.worldJson!=SaveCodec.encode(w))save(slot,w);settings.edit{it[lastKey]=slot};w}
 suspend fun save(slot: Int,w: World)=withContext(Dispatchers.IO+NonCancellable){
  require(slot in 1..5){"Es gibt fünf Speicherplätze."};val payload=SaveCodec.encode(w)
  require(payload.toByteArray().size<=32*1024*1024){"Der Spielstand überschreitet 32 MB."}
  val row=Savegame(slot,slot,w.club().name,w.calendar.season,w.calendar.matchday,WorldFactory.leagueName(w,w.club().tier),w.user.difficulty.label,System.currentTimeMillis(),payload,SAVE_VERSION)
  db.withTransaction{db.saves().put(row)};settings.edit{it[lastKey]=slot}
 }
 suspend fun delete(slot: Int)=withContext(Dispatchers.IO){db.withTransaction{db.saves().delete(slot)}}
}
