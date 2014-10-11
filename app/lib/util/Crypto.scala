package lib.util

import java.security.{MessageDigest, SecureRandom}

/**
 * Created by trupin on 7/26/14.
 */
object Crypto {
  def generateToken(): String = {
    val key = java.util.UUID.randomUUID.toString
    new sun.misc.BASE64Encoder().encode(key.getBytes)
  }
}

object BearerTokenGenerator {
  val TOKEN_LENGTH = 32
  val TOKEN_CHARS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  val secureRandom = new SecureRandom()

  def generateToken:String =
    generateToken(TOKEN_LENGTH)

  def generateToken(tokenLength: Int): String =
    if(tokenLength == 0) "" else TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length())) +
      generateToken(tokenLength - 1)
}

object MD5 {
  def hex_digest(s: String): String = hex_digest(s.getBytes)
  def hex_digest(d: Array[Byte]): String = MessageDigest.getInstance("MD5").digest(d).map("%02X".format(_)).mkString
}