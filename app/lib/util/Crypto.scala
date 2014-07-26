package lib.util

/**
 * Created by trupin on 7/26/14.
 */
object Crypto {
  def generateToken(): String = {
    val key = java.util.UUID.randomUUID.toString
    new sun.misc.BASE64Encoder().encode(key.getBytes)
  }
}