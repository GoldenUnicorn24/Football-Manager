package de.gruenderelf.app
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.gruenderelf.app.data.*
import de.gruenderelf.app.ui.CHANGELOG_VERSION
import de.gruenderelf.engine.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class RoomPersistenceTest {
 @Test fun committedWeekSurvivesCloseAndReopenInFiveSlots(){runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();val name="test-saves.db";context.deleteDatabase(name)
  val first=SaveDatabase.open(context,name);val repo=GameRepository(context,first);val w=WorldFactory.createWorld(5656);w.club().budget=45000;ConstructionEngine.start(w,Facility.FLOODLIGHTS)
  while(w.calendar.matchday==1){val fixture=w.nextFixture()?:error("Kein Pflichtspiel für Spieltag 1");w.live=MatchEngine.simulateFullMatch(w,fixture);SeasonEngine.advanceWeek(w)}
  assertEquals(2,w.calendar.matchday)
  for(slot in 1..5)repo.save(slot,w);first.close()
  val second=SaveDatabase.open(context,name);val reloaded=GameRepository(context,second)
  for(slot in 1..5){val value=reloaded.load(slot);assertEquals(w.club().budget,value.club().budget);assertEquals(w.club().tactics.xi,value.club().tactics.xi);assertEquals(w.calendar.matchday,value.calendar.matchday);assertEquals(w.construction,value.construction)}
  reloaded.delete(3);assertNull(second.saves().get(3));assertNotNull(second.saves().get(2));second.close();context.deleteDatabase(name)
 }}
 @Test fun invalidSaveDoesNotReplaceExistingSlot(){runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();val db=SaveDatabase.open(context,"test-invalid.db");val repo=GameRepository(context,db);val w=WorldFactory.createWorld(10);repo.save(1,w)
  try{val bad=SaveCodec.decode("{}");repo.save(1,bad);fail("Invalid JSON must fail")}catch(_:Exception){}
  assertEquals(w.club().budget,repo.load(1).club().budget);db.close();context.deleteDatabase("test-invalid.db")
 }}
 @Test fun largeWorldCrossesCursorWindowLimitWithoutLosingUnicode(){runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();val db=SaveDatabase.open(context,"test-large.db");val repo=GameRepository(context,db);val w=WorldFactory.createWorld(101)
  val text="Gründerelf – Straße am Sportplatz. ".repeat(100000);w.news("Großer Spielstand",text);assertTrue(SaveCodec.encode(w).toByteArray().size>3*1024*1024);repo.save(1,w);assertEquals(text,repo.load(1).news.first().text);db.close();context.deleteDatabase("test-large.db")
 }}
 @Test fun globalSoundPreferenceDefaultsOnAndPersistsAsOneMasterSwitch(){runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();val db=SaveDatabase.open(context,"test-sound-pref.db");val repo=GameRepository(context,db)
  assertTrue(repo.soundsEnabled.first());repo.setSoundsEnabled(false);assertFalse(repo.soundsEnabled.first());repo.setSoundsEnabled(true);assertTrue(repo.soundsEnabled.first())
  db.close();context.deleteDatabase("test-sound-pref.db")
 }}
 @Test fun changelogVersionDefaultsUnseenAndPersistsAfterDismiss(){runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();val db=SaveDatabase.open(context,"test-changelog-pref.db");val repo=GameRepository(context,db)
  assertNotEquals(CHANGELOG_VERSION,repo.changelogSeenVersion.first());repo.markChangelogSeen(CHANGELOG_VERSION);assertEquals(CHANGELOG_VERSION,repo.changelogSeenVersion.first())
  val reopened=GameRepository(context,db);assertEquals(CHANGELOG_VERSION,reopened.changelogSeenVersion.first())
  db.close();context.deleteDatabase("test-changelog-pref.db")
 }}

 @Test fun corruptedPrimaryFallsBackToHiddenValidatedBackup(){runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();val name="test-backup.db";context.deleteDatabase(name);val db=SaveDatabase.open(context,name);val repo=GameRepository(context,db);val w=WorldFactory.createWorld(50512L)
  w.club().budget=111_111L;repo.save(1,w);w.club().budget=222_222L;repo.save(1,w)
  val primary=db.saves().get(1)!!;db.saves().put(primary.copy(worldJson="{broken-json"))
  val restored=repo.load(1);assertEquals(222_222L,restored.club().budget);assertEquals(1,repo.saves.first().size);assertEquals(1,repo.saves.first().single().slot)
  assertEquals(222_222L,SaveCodec.decode(db.saves().get(1)!!.worldJson).club().budget);db.close();context.deleteDatabase(name)
 }}

 @Test fun corruptPrimaryIsNeverPromotedToBackupOnNextSave(){runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();val name="test-backup-validation.db";context.deleteDatabase(name);val db=SaveDatabase.open(context,name);val repo=GameRepository(context,db);val w=WorldFactory.createWorld(50513L)
  w.club().budget=100_000L;repo.save(1,w);w.club().budget=200_000L;repo.save(1,w)
  val primary=db.saves().get(1)!!;db.saves().put(primary.copy(worldJson="{broken-json"))
  w.club().budget=300_000L;repo.save(1,w)
  assertEquals(300_000L,SaveCodec.decode(db.saves().get(101)!!.worldJson).club().budget)
  assertEquals(300_000L,repo.load(1).club().budget);db.close();context.deleteDatabase(name)
 }}

 @Test fun fastCheckpointKeepsLastFullBackupForRecovery(){runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();val name="test-checkpoint-backup.db";context.deleteDatabase(name);val db=SaveDatabase.open(context,name);val repo=GameRepository(context,db);val w=WorldFactory.createWorld(50514L)
  w.club().budget=111_111L;repo.save(1,w);w.club().budget=222_222L;repo.checkpoint(1,w)
  val primary=db.saves().get(1)!!;db.saves().put(primary.copy(worldJson="{broken-json"))
  val restored=repo.load(1);assertEquals(111_111L,restored.club().budget);db.close();context.deleteDatabase(name)
 }}

}
