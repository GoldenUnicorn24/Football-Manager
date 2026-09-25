package de.gruenderelf.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrandingPersistenceTest {
 @Test fun uploadedBrandingSurvivesSaveRoundtrip(){
  val world=WorldFactory.createWorld(52301L)
  world.club().logo.customImage="crest-base64"
  world.club().kits.home.customImage="home-base64"
  world.club().kits.away.customImage="away-base64"

  val copy=SaveCodec.decode(SaveCodec.encode(world))

  assertEquals("crest-base64",copy.club().logo.customImage)
  assertEquals("home-base64",copy.club().kits.home.customImage)
  assertEquals("away-base64",copy.club().kits.away.customImage)
  assertNull(copy.club().kits.keeper.customImage)
 }
}
