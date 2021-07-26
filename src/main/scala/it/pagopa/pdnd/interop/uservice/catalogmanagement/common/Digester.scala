package it.pagopa.pdnd.interop.uservice.catalogmanagement.common

import java.io.File
import java.nio.file.Files
import java.security.{DigestInputStream, MessageDigest}
import scala.annotation.tailrec

object Digester {
  def createHash(file: File): String = {
    val md  = MessageDigest.getInstance("MD5")
    val dis = new DigestInputStream(Files.newInputStream(file.toPath), md)

    loop(dis.available > 0, { val _ = dis.read }, { dis.close() })

    md.digest.map(b => String.format("%02x", Byte.box(b))).mkString

  }

  @tailrec
  private def loop(cond: => Boolean, block: => Unit, closing: => Unit): Unit =
    if (cond) {
      block
      loop(cond, block, closing)
    } else {
      closing
    }

}