package de.gruenderelf.engine
import kotlin.math.exp
/** Mulberry32. State is explicit and is included in every save. */
class SeededRandom(var state: Long){
 fun nextDouble(): Double {state=(state+0x6D2B79F5L) and 0xffffffffL;var t=state.toInt();t=(t xor(t ushr 15))*(t or 1);t=t xor(t+((t xor(t ushr 7))*(t or 61)));return ((t xor(t ushr 14)).toLong() and 0xffffffffL)/4294967296.0}
 fun int(from: Int,to: Int): Int{require(to>=from);return from+(nextDouble()*(to-from+1)).toInt()}
 fun chance(p: Double)=nextDouble()<p.coerceIn(0.0,1.0)
 fun <T> pick(items: List<T>): T{require(items.isNotEmpty());return items[int(0,items.lastIndex)]}
 fun poisson(lambda: Double): Int{val limit=exp(-lambda);var p=1.0;var n=0;do{n++;p*=nextDouble()}while(p>limit);return n-1}
}
internal inline fun <T> World.random(block: (SeededRandom)->T): T{val rng=SeededRandom(rngState);val result=block(rng);rngState=rng.state;return result}
