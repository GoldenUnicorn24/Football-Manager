package de.gruenderelf.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_BRANDING_BYTES=1_200_000

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
 var bitmap=resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,opts)}?:return@runCatching null
 val largest=max(bitmap.width,bitmap.height)
 if(largest>maxDimension){
  val scale=maxDimension.toFloat()/largest
  val resized=Bitmap.createScaledBitmap(
   bitmap,
   (bitmap.width*scale).roundToInt().coerceAtLeast(1),
   (bitmap.height*scale).roundToInt().coerceAtLeast(1),
   true
  )
  if(resized!==bitmap)bitmap.recycle()
  bitmap=resized
 }

 fun encode(source:Bitmap):ByteArray{
  val out=ByteArrayOutputStream()
  val alpha=preserveAlpha&&source.hasAlpha()
  source.compress(if(alpha)Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,if(alpha)100 else 90,out)
  return out.toByteArray()
 }

 var bytes=encode(bitmap)
 while(bytes.size>MAX_BRANDING_BYTES&&max(bitmap.width,bitmap.height)>256){
  val resized=Bitmap.createScaledBitmap(
   bitmap,
   (bitmap.width*.82f).roundToInt().coerceAtLeast(1),
   (bitmap.height*.82f).roundToInt().coerceAtLeast(1),
   true
  )
  if(resized!==bitmap)bitmap.recycle()
  bitmap=resized
  bytes=encode(bitmap)
 }
 bitmap.recycle()
 if(bytes.isEmpty()||bytes.size>MAX_BRANDING_BYTES)return@runCatching null
 Base64.encodeToString(bytes,Base64.NO_WRAP)
}.getOrNull()

fun decodeBrandImage(encoded: String?): Bitmap? {
 if(encoded.isNullOrBlank())return null
 return runCatching {
  val bytes=Base64.decode(encoded,Base64.DEFAULT)
  BitmapFactory.decodeByteArray(bytes,0,bytes.size)
 }.getOrNull()
}
