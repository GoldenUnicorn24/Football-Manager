package de.gruenderelf.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Imports user-selected club branding into a portable, save-friendly Base64 payload.
 * Images are downscaled before they enter the save so exported careers remain reasonably small.
 */
fun importBrandImage(
 context: Context,
 uri: Uri,
 maxDimension: Int,
 preserveAlpha: Boolean
): String? = runCatching {
 val resolver=context.contentResolver
 val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
 resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
 if(bounds.outWidth<=0||bounds.outHeight<=0)return@runCatching null

 var sample=1
 while(max(bounds.outWidth,bounds.outHeight)/sample>maxDimension*2)sample*=2
 val opts=BitmapFactory.Options().apply{
  inSampleSize=sample
  inPreferredConfig=Bitmap.Config.ARGB_8888
 }
 val decoded=resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,opts)}?:return@runCatching null
 val largest=max(decoded.width,decoded.height)
 val bitmap=if(largest>maxDimension){
  val scale=maxDimension.toFloat()/largest
  Bitmap.createScaledBitmap(
   decoded,
   (decoded.width*scale).roundToInt().coerceAtLeast(1),
   (decoded.height*scale).roundToInt().coerceAtLeast(1),
   true
  ).also{if(it!==decoded)decoded.recycle()}
 }else decoded

 val out=ByteArrayOutputStream()
 val format=if(preserveAlpha)Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
 bitmap.compress(format,if(preserveAlpha)100 else 90,out)
 bitmap.recycle()
 val bytes=out.toByteArray()
 if(bytes.isEmpty()||bytes.size>2_500_000)return@runCatching null
 Base64.encodeToString(bytes,Base64.NO_WRAP)
}.getOrNull()

fun decodeBrandImage(encoded: String?): Bitmap? {
 if(encoded.isNullOrBlank())return null
 return runCatching {
  val bytes=Base64.decode(encoded,Base64.DEFAULT)
  BitmapFactory.decodeByteArray(bytes,0,bytes.size)
 }.getOrNull()
}
