package de.gruenderelf.engine

data class DomesticCupSpec(
 val id:String,
 val name:String,
 val country:String,
 val targetSize:Int,
 val roundDays:List<Int>,
 val roundNames:List<String>,
 val winnerPrize:Long,
 val secondary:Boolean=false
) {
 init {
  require(targetSize>1 && targetSize and (targetSize-1)==0){"Cup-Größe muss Zweierpotenz sein: $name"}
  require(roundDays.size==roundNames.size){"Runden/Termine passen nicht: $name"}
 }
}

object DomesticCompetitionData {
 const val FANTASY_ID="FANTASY_CUP"
 const val DFB_ID="DFB_POKAL"

 val cups: List<DomesticCupSpec> = listOf(
  DomesticCupSpec(DFB_ID,"DFB-Pokal","Deutschland",64,listOf(1,4,10,16,25,33),listOf("1. Runde","2. Runde","Achtelfinale","Viertelfinale","Halbfinale","Finale"),6_000_000L),
  DomesticCupSpec("FA_CUP","FA Cup","England",64,listOf(3,8,14,21,29,37),listOf("3. Runde","4. Runde","Achtelfinale","Viertelfinale","Halbfinale","Finale"),7_000_000L),
  DomesticCupSpec("EFL_CUP","EFL Cup","England",64,listOf(1,6,11,17,24,32),listOf("1. Runde","2. Runde","Achtelfinale","Viertelfinale","Halbfinale","Finale"),4_000_000L,secondary=true),
  DomesticCupSpec("COPA_DEL_REY","Copa del Rey","Spanien",64,listOf(2,6,12,18,26,34),listOf("1. Runde","2. Runde","Achtelfinale","Viertelfinale","Halbfinale","Finale"),6_500_000L),
  DomesticCupSpec("COPPA_ITALIA","Coppa Italia","Italien",64,listOf(2,7,13,19,27,35),listOf("1. Runde","2. Runde","Achtelfinale","Viertelfinale","Halbfinale","Finale"),6_000_000L),
  DomesticCupSpec("COUPE_DE_FRANCE","Coupe de France","Frankreich",32,listOf(2,8,15,23,31),listOf("1. Runde","Achtelfinale","Viertelfinale","Halbfinale","Finale"),5_500_000L),
  DomesticCupSpec("KNVB_BEKER","KNVB Beker","Niederlande",16,listOf(4,12,22,32),listOf("Achtelfinale","Viertelfinale","Halbfinale","Finale"),3_000_000L),
  DomesticCupSpec("CROKY_CUP","Croky Cup","Belgien",16,listOf(4,12,22,32),listOf("Achtelfinale","Viertelfinale","Halbfinale","Finale"),3_000_000L),
  DomesticCupSpec("OEFB_CUP","ÖFB-Cup","Österreich",8,listOf(6,16,28),listOf("Viertelfinale","Halbfinale","Finale"),2_500_000L),
  DomesticCupSpec("SWISS_CUP","Schweizer Cup","Schweiz",8,listOf(6,16,28),listOf("Viertelfinale","Halbfinale","Finale"),2_500_000L),
  DomesticCupSpec("TURKIYE_KUPASI","Türkiye Kupası","Türkei",16,listOf(4,12,22,32),listOf("Achtelfinale","Viertelfinale","Halbfinale","Finale"),3_500_000L),
  DomesticCupSpec("TACA_DE_PORTUGAL","Taça de Portugal","Portugal",16,listOf(4,12,22,32),listOf("Achtelfinale","Viertelfinale","Halbfinale","Finale"),3_500_000L)
 )

 fun byId(id:String): DomesticCupSpec? = cups.firstOrNull{it.id==id}
 fun forCountry(country:String): List<DomesticCupSpec> = cups.filter{it.country==country}
 fun primaryForCountry(country:String): DomesticCupSpec? = forCountry(country).firstOrNull{!it.secondary} ?: forCountry(country).firstOrNull()
}
