package de.knutwalker.ntparser

import java.io.{ Closeable, EOFException, InputStream, PushbackInputStream }
import java.nio.charset.Charset
import java.nio.file.{ FileSystems, Files, Path, StandardOpenOption }

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

object Loader {

  private var streams: List[Closeable] = Nil

  def apply(fileName: String): Option[InputStream] =
    openStream(fileName).map(deflateStream)

  def getLines(fileName: String, enc: Charset): Iterator[String] =
    apply(fileName).map(getLines(_, enc)).getOrElse(Iterator.empty)

  def getLines(is: InputStream, enc: Charset): Iterator[String] =
    scala.io.Source.fromInputStream(is)(enc).getLines()

  def shutdown() =
    streams.foreach(_.close())

  private def openStream(fileName: String): Option[InputStream] =
    Some(toPath(fileName)).
      filter(fileExists).
      map(loadFile).
      orElse(Option(loadResource(fileName))).
      orElse(Option(loadResource("/" + fileName))).
      map(addCloseable)

  private def toPath(fileName: String): Path =
    FileSystems.getDefault.getPath(fileName)

  private def fileExists(path: Path): Boolean =
    Files.exists(path)

  private def loadFile(path: Path): InputStream =
    Files.newInputStream(path, StandardOpenOption.READ)

  private def loadResource(fileName: String): InputStream =
    getClass.getResourceAsStream(fileName)

  private def addCloseable(is: InputStream): InputStream = {
    streams ::= is
    is
  }

  private def deflateStream(is: InputStream): InputStream =
    new PushbackInputStream(is, 3) match {
      case GzipStream(stream)  ⇒ stream
      case Bzip2Stream(stream) ⇒ stream
      case _                   ⇒ is
    }

  private trait CompressedStream {

    final def unapply(stream: PushbackInputStream): Option[InputStream] = {
      val buf = peekBytes(stream, peekSize)
      if (matches(buf)) {
        Some(newStream(stream))
      }
      else {
        None
      }
    }

    protected def peekSize: Int

    protected def matches(buf: Array[Byte]): Boolean

    protected def newStream(is: InputStream): InputStream

    private def peekBytes(stream: PushbackInputStream, n: Int): Array[Byte] = {
      val buf = new Array[Byte](n)
      val bytesRead = stream.read(buf)
      if (bytesRead == -1) {
        throw new EOFException
      }
      stream.unread(buf, 0, bytesRead)
      buf
    }
  }

  private object GzipStream extends CompressedStream {

    protected val peekSize = 2

    protected def matches(buf: Array[Byte]) =
      GzipCompressorInputStream.matches(buf, 2)

    protected def newStream(is: InputStream) =
      new GzipCompressorInputStream(is, true)
  }

  private object Bzip2Stream extends CompressedStream {

    protected val peekSize = 3

    protected def matches(buf: Array[Byte]) =
      BZip2CompressorInputStream.matches(buf, 3)

    protected def newStream(is: InputStream) =
      new BZip2CompressorInputStream(is, true)
  }

}
