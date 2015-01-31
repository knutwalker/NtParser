/*
 * Copyright 2014 Paul Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.knutwalker.ntparser

import org.slf4j.LoggerFactory

import scala.Predef.{assert, classOf}
import scala.annotation.{switch, tailrec}
import scala.collection.GenIterable
import scala.collection.JavaConverters.{asJavaIteratorConverter, asScalaIteratorConverter}
import scala.collection.mutable.ListBuffer
import scala.io.Codec
import scala.util.Try
import java.io.InputStream
import java.lang.{Iterable ⇒ JIterable, StringBuilder}
import java.nio.charset.Charset
import java.util.{Iterator ⇒ JIterator}

/**
 * An NtParser always parses a single line in different modes, depending
 * on the method that was called.
 *
 * @define swallowsErrors
 *         This methods will swallow parse errors and not
 *         throw any [[ParseError]]s
 */
abstract class NtParser {
  import de.knutwalker.ntparser.NtParser._

  private[this] val logger = LoggerFactory.getLogger(classOf[NtParser])

  private[this] var input: Array[Char] = new Array[Char](8192)

  private[this] var pos = 0
  private[this] var max = 0
  private[this] var cursor: Char = 0
  private[this] var lineNo = -1

  private[this] val sb = new StringBuilder
  private[this] val nodes: ListBuffer[Node] = ListBuffer.empty
  private[this] var parsedSubject: Node = _
  private[this] var parsedPredicate: Resource = _
  private[this] var parsedObject: Node = _

  /**
   * Parse a single line into a [[Statement]].
   *
   * $swallowsErrors
   *
   * @param line A string that may contain one [[Statement]]
   * @return Some(statement) if a statement could be parsed, otherwise None
   */
  final def parseOpt(line: String): Option[Statement] = Option(parseOrNull(line))

  /**
   * Parse a single line into a [[Statement]] that is
   * part of a bigger file at some location.
   *
   * $swallowsErrors
   *
   * @param line A string that may contain one [[Statement]]
   * @param lineNumber The current line number
   * @return Some(statement) if a statement could be parsed, otherwise None
   */
  final def parseOpt(line: String, lineNumber: Int): Option[Statement] = Option(parseOrNull(line, lineNumber))

  /**
   * Parse a single line into a [[Statement]].
   *
   * $swallowsErrors
   *
   * @param line A string that may contain one [[Statement]]
   * @return Success(statement) if a statement could be parsed, otherwise Failure(parseError)
   */
  final def parseTry(line: String): Try[Option[Statement]] = Try(Option(parse(line)))

  /**
   * Parse a single line into a [[Statement]] that is
   * part of a bigger file at some location.
   *
   * @param line A string that may contain one [[Statement]]
   * @param lineNumber The current line number
   * @return Success(statement) if a statement could be parsed, otherwise Failure(parseError)
   */
  final def parseTry(line: String, lineNumber: Int): Try[Option[Statement]] = Try(Option(parse(line, lineNumber)))

  /**
   * Parse a single line into a [[Statement]].
   *
   * @param line A string that may contain one [[Statement]]
   * @return The [[Statement]] if it could be parsed, or null otherwise
   */
  final def parseOrNull(line: String): Statement = {
    try parse(line) catch {
      case pe: ParseError ⇒
        logger.warn(pe.getMessage, pe)
        null
    }
  }

  /**
   * Parse a single line into a [[Statement]] that is
   * part of a bigger file at some location.
   *
   * @param line A string that may contain one [[Statement]]
   * @param lineNumber The current line number
   * @return The [[Statement]] if it could be parsed, or null otherwise
   */
  final def parseOrNull(line: String, lineNumber: Int): Statement = {
    try parse(line, lineNo) catch {
      case pe: ParseError ⇒
        logger.warn(pe.getMessage, pe)
        null
    }
  }

  /**
   * Parse a single line into a [[Statement]].
   *
   * @param line A string that may contain one [[Statement]]
   * @throws ParseError if a line could not be parsed
   * @return The [[Statement]] if it could be parsed, otherwise throw a ParseException
   */
  @throws[ParseError]("ParseError if a line could not be parsed")
  final def parse(line: String): Statement = {
    lineNo = -1
    if (line.isEmpty) null
    else {
      reset(line)
      Line()
    }
  }

  /**
   * Parse a single line into a [[Statement]] that is
   * part of a bigger file at some location.
   *
   * @param line A string that may contain one [[Statement]]
   * @param lineNumber The current line number
   * @throws ParseError if a line could not be parsed
   * @return The [[Statement]] if it could be parsed, otherwise throw a ParseException
   */
  @throws[ParseError]("ParseError if a line could not be parsed")
  final def parse(line: String, lineNumber: Int): Statement = {
    lineNo = lineNumber
    if (line.isEmpty) null
    else {
      reset(line)
      Line()
    }
  }

  private[this] def Line(): Statement = {
    ws()
    (cursor: @switch) match {
      case '<'      ⇒ TripleLine()
      case '_'      ⇒ TripleLine()
      case '#'      ⇒ // comment line
      case '\u0000' ⇒ // empty line, inline char to allow switch statement
      case _        ⇒ error(LINE_BEGIN)
    }

    if ((parsedSubject ne null) && (parsedPredicate ne null) && (parsedObject ne null)) {
      Triple(parsedSubject, parsedPredicate, parsedObject)
    }
    else null
  }

  private[this] def TripleLine(): Boolean = {
    Subject()
    Predicate()
    Object()
    ws('.') || error('.')
  }

  private[this] def Subject(): Unit = {
    (cursor: @switch) match {
      case '<' ⇒ parsedSubject = IriRef()
      case '_' ⇒ parsedSubject = NamedNode()
      case _   ⇒ error(SUBJECT_BEGIN)
    }
  }

  private[this] def Predicate(): Unit = {
    parsedPredicate = IriRef()
  }

  private[this] def Object(): Unit = {
    (cursor: @switch) match {
      case '<' ⇒ parsedObject = IriRef()
      case '_' ⇒ parsedObject = NamedNode()
      case '"' ⇒ parsedObject = LiteralNode()
      case _   ⇒ error(OBJECT_BEGIN)
    }
  }

  private[this] def IriRef(): Resource = {
    mustAdvance('<')
    IriScheme()
    IriRefCharacters()
    mustAdvance('>')
    ws()
    Resource(clear())
  }

  private[this] def NamedNode(): BNode = {
    mustAdvance('_')
    mustAdvance(':')
    val start = cursor
    // TODO: proper IS_NAME_CHAR according to http://www.w3.org/TR/2014/REC-n-triples-20140225/#grammar-production-BLANK_NODE_LABEL
    advance(IS_NAME_CHAR) || error("name identifier")
    append(start)
    captureWhile(IS_NAME_CHAR)
    // TODO, maybe advance?
    ws() || error(WHITESPACE)
    ws()
    BNode(clear())
  }

  protected def LiteralNode(): Literal

  protected final def ShortLiteralNode(): Literal = {
    mustAdvance('"')
    ShortLiteralCharacters()
    mustAdvance('"')
    finishLiteral()
  }

  protected final def LongLiteralNode(): Literal = {
    mustAdvance("\"\"\"")
    LongLiteralCharacters()
    mustAdvance("\"\"\"")
    finishLiteral()
  }

  private[this] def finishLiteral(): Literal = {
    val value = clear()
    val lit = (cursor: @switch) match {
      case '@' ⇒ LangLiteral(value)
      case '^' ⇒ TypedLiteral(value)
      case _   ⇒ Literal.simple(value)
    }
    ws()
    lit
  }

  @tailrec private[this] def IriScheme(): Unit = {
    captureWhile(IS_SCHEMA_CHAR)
    (cursor: @switch) match {
      case ':' ⇒ // scheme finish
      case '\\' ⇒
        UnicodeEscapedCharacter()
        IriScheme()
      case '%' ⇒
        PercentEscapedCharacter()
        IriScheme()
      case _ ⇒ validationError(s"<${clear()}> is not absolute")
    }
  }

  @tailrec private[this] def IriRefCharacters(): Unit = {
    captureWhile(IS_IRIREF_CHAR)
    (cursor: @switch) match {
      case '>' ⇒ // iriref finish
      case '\\' ⇒
        UnicodeEscapedCharacter()
        IriRefCharacters()
      case '%' ⇒
        PercentEscapedCharacter()
        IriRefCharacters()
      case _ ⇒ error(IRIREF_CHARACTERS)
    }
  }

  @tailrec private[this] def ShortLiteralCharacters(): Unit = {
    captureWhile(IS_LITERAL_CHAR)
    (cursor: @switch) match {
      case '"' ⇒ //string finish
      case '\\' ⇒
        SlashEscapedCharacter()
        ShortLiteralCharacters()
      case _ ⇒ error(LITERAL_CHARACTERS)
    }
  }

  @tailrec private[this] def LongLiteralCharacters(): Unit = {
    captureWhile(IS_LONG_LITERAL_CHAR)
    (cursor: @switch) match {
      case '"' ⇒ // string might be finished
        if ((peek != '"') || (peekpeek != '"')) {
          append('"')
          advance()
          LongLiteralCharacters()
        }
      case '\\' ⇒
        SlashEscapedCharacter()
        LongLiteralCharacters()
      case _ ⇒ error(LITERAL_CHARACTERS)
    }
  }

  private[this] def TypedLiteral(value: String) = {
    advance("^^") || error('^')
    Literal.typed(value, IriRef())
  }

  private[this] def LangLiteral(value: String): Literal = {
    mustAdvance('@')
    captureWhile(IS_NAME_START)
    (cursor: @switch) match {
      case ' ' | '\t' ⇒ Literal.tagged(value, clear())
      case '-'        ⇒ ExtendedLangLiteral(value)
      case _ ⇒
        error("language tag identifier")
        null
    }
  }

  private[this] def ExtendedLangLiteral(value: String): Literal = {
    mustAdvance('-')
    append('-')
    captureWhile(IS_NAME_CHAR)
    (cursor: @switch) match {
      case ' ' | '\t' ⇒ Literal.tagged(value, clear())
      case _ ⇒
        error("language tag identifier")
        null
    }
  }

  private[this] def UnicodeEscapedCharacter(): Unit = {
    mustAdvance('\\')
    (cursor: @switch) match {
      case 'u' ⇒ Unicode()
      case 'U' ⇒ SuperUnicode()
      case _   ⇒ error(UNICODE_ESCAPE_CHARS)
    }
  }

  private[this] def SlashEscapedCharacter(): Unit = {
    mustAdvance('\\')
    (cursor: @switch) match {
      case '\\' ⇒
        append('\\')
        advance()
      case '"' ⇒
        append('"')
        advance()
      case '\'' ⇒
        append('\'')
        advance()
      case 'b' ⇒
        append('\b')
        advance()
      case 't' ⇒
        append('\t')
        advance()
      case 'n' ⇒
        append('\n')
        advance()
      case 'f' ⇒
        append('\f')
        advance()
      case 'r' ⇒
        append('\r')
        advance()
      case 'u' ⇒ Unicode()
      case 'U' ⇒ SuperUnicode()
      case _   ⇒ error(SLASH_ESCAPE_CHARS)
    }
  }

  private[this] def Unicode(): Unit = {
    mustAdvance('u')
    append(captureUnicodeDigits())
  }

  private[this] def captureUnicodeDigits(): Char = (
    captureHexDigit() * 4096 +
    captureHexDigit() * 256 +
    captureHexDigit() * 16 +
    captureHexDigit()).toChar

  private[this] def SuperUnicode(): Unit = {
    mustAdvance('U')
    append(captureSuperUnicodeDigits())
  }

  private[this] def captureSuperUnicodeDigits(): Int =
    captureHexDigit() * 268435456 +
      captureHexDigit() * 16777216 +
      captureHexDigit() * 1048576 +
      captureHexDigit() * 65536 +
      captureHexDigit() * 4096 +
      captureHexDigit() * 256 +
      captureHexDigit() * 16 +
      captureHexDigit()

  private[this] def captureHexDigit(): Int = {
    IS_HEX_CHAR(cursor) || error("hex character")
    val r = hexValue(cursor)
    advance()
    r
  }

  @inline private[this] def hexValue(c: Char): Int =
    (c & 0x1f) + ((c >> 6) * 0x19) - 0x10

  private[this] def PercentEscapedCharacter(): Unit = {
    mustAdvance('%')
    append(percentEscaped0(0, new Array[Byte](1)))
  }

  @tailrec private[this] def percentEscaped0(idx: Int, buf: Array[Byte]): Array[Byte] = {
    buf(idx) = capturePercentDigits()
    cursor match {
      case '%' ⇒
        advance()
        percentEscaped0(idx + 1, grow(buf, idx + 2))
      case _ ⇒ buf
    }
  }

  private[this] def capturePercentDigits(): Byte = (
    captureHexDigit() * 16 +
    captureHexDigit()).toByte

  private[this] def captureWhile(f: Char ⇒ Boolean): Boolean = {
    capture0(f)
    f(cursor)
  }

  @tailrec private[this] def capture0(f: Char ⇒ Boolean): Boolean = {
    if (f(cursor)) append()
    advance(f) && capture0(f)
  }

  @tailrec private[this] final def ws(): Boolean = {
    if (IS_WHITESPACE(cursor)) advance() && ws()
    else true
  }

  @inline private[this] def ws(c: Char): Boolean = {
    ws()
    advance(c)
  }

  @inline private[this] def mustAdvance(c: Char): Boolean =
    advance(c) || error(c)

  @inline private[this] def mustAdvance(s: String): Boolean =
    advance(s) || error(s)

  @inline private[this] def advance(f: Char ⇒ Boolean): Boolean = {
    f(cursor) && advance()
  }

  @inline private[this] def advance(s: String): Boolean = {
    _advance(s, 0, s.length)
  }

  @tailrec
  private[this] final def _advance(s: String, i: Int, l: Int): Boolean = {
    (i == l) || (advance(s.charAt(i)) && _advance(s, i + 1, l))
  }

  @inline private[this] def advance(c: Char): Boolean = {
    cursor == c && advance()
  }

  private[this] def advance(): Boolean = {
    val m = max
    if (pos < m) {
      val c = pos + 1
      cursor = if (c == m) END else input(c)
      pos = c
      true
    }
    else false
  }

  @inline protected final def peek: Char = {
    val nextPos = pos + 1
    if (nextPos >= max) END else input(nextPos)
  }

  @inline protected final def peekpeek: Char = {
    val nextPos = pos + 2
    if (nextPos >= max) END else input(nextPos)
  }

  @inline private[this] def error(c: Char): Nothing = {
    error(Array(c))
  }

  private[this] def error(c: Array[Char]): Nothing = {
    val expected = c.length match {
      case 0 ⇒ "n/a"
      case 1 ⇒ c(0).toString
      case n ⇒ _errorMsg(c, 0, n - 1, new StringBuilder())
    }
    error(expected)
  }

  @tailrec
  private[this] def _errorMsg(cs: Array[Char], i: Int, l: Int, buf: StringBuilder): String = {
    if (i == l) buf.append(", or ").append(cs(i)).toString
    else {
      if (i > 0) buf.append(", ")
      buf.append(cs(i))
      _errorMsg(cs, i + 1, l, buf)
    }
  }

  private[this] def error(s: String): Nothing = {
    val cursorChar = cursor match {
      case END ⇒ "EOI"
      case x   ⇒ x.toString
    }
    val lineHint = if (lineNo == -1) " " else s" in line $lineNo "
    throwError(s"parse error${lineHint}at char ${pos + 1}, expected [$s], but found [$cursorChar]")
  }

  private[this] def validationError(s: String): Nothing = {
    val lineHint = if (lineNo == -1) " " else s" in line $lineNo "
    throwError(s"parse error${lineHint}at char ${pos + 1}, $s")
  }

  private[this] def throwError(text: String): Nothing = {
    val line = new String(input, 0, max)
    val mark = (List.fill(pos)(' ') ::: '^' :: Nil).mkString

    throw new ParseError((text :: line :: mark :: Nil).mkString("\n"))
  }

  private[this] def clear(): String = {
    val r = sb.toString
    sb setLength 0
    r
  }

  private[this] def reset(forLine: String): Unit = {
    parsedSubject = null
    parsedPredicate = null
    parsedObject = null

    nodes.clear()
    sb setLength 0

    pos = 0
    max = forLine.length
    grow(max)
    forLine.getChars(0, max, input, 0)

    cursor = input(0)
  }

  @inline private[this] def append(): Unit = append(cursor)

  @inline private[this] def append(c: Char): Unit = sb append c

  @inline private[this] def append(bs: Array[Byte]): Unit = append(Codec.fromUTF8(bs))

  @inline private[this] def append(cs: Array[Char]): Unit = sb append cs

  @inline private[this] def append(cp: Int): Unit = sb appendCodePoint cp

  private[this] def oversize(minTargetSize: Int): Int = {
    if (minTargetSize == 0) 0
    else {
      val extra = math.max(minTargetSize >> 3, 3)
      val newSize = minTargetSize + extra

      (newSize + 3) & 0x7ffffffc
    }
  }

  private[this] def grow(minSize: Int) = {
    assert(minSize >= 0, "size must be positive (got " + minSize + "): likely integer overflow?")
    val array = input
    if (array.length < minSize) {
      val newArray: Array[Char] = new Array[Char](oversize(minSize))
      System.arraycopy(array, 0, newArray, 0, array.length)
      input = newArray
    }
  }

  private[this] def grow(oldArray: Array[Byte], to: Int): Array[Byte] = {
    assert(to >= 0, "size must be positive (got " + to + "): likely integer overflow?")
    if (oldArray.length < to) {
      val newArray = new Array[Byte](to)
      System.arraycopy(oldArray, 0, newArray, 0, oldArray.length)
      newArray
    }
    else oldArray
  }
}
object NtParser {
  private final val END = Char.MinValue
  private final val WHITESPACE = Array(' ', '\t')
  private final val LINE_BEGIN = Array('<', '_', '#')
  private final val SUBJECT_BEGIN = Array('<', '_')
  private final val OBJECT_BEGIN = Array('<', '_', '"')
  private final val IRIREF_CHARACTERS = Array('>', '\\', '%')
  private final val LITERAL_CHARACTERS = Array('"', '\\')
  private final val UNICODE_ESCAPE_CHARS = Array('u', 'U')
  private final val SLASH_ESCAPE_CHARS = Array('\\', '"', '\'', 'b', 't', 'n', 'f', 'r', 'u', 'U')
  private final val IS_WHITESPACE = (c: Char) ⇒ c == ' ' || c == '\t'
  private final val IS_NAME_START = (c: Char) ⇒ (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
  private final val IS_NAME_CHAR = (c: Char) ⇒ IS_NAME_START(c) || c >= '0' && c <= '9'
  private final val IS_HEX_CHAR = (c: Char) ⇒ (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
  private final val IS_SCHEMA_CHAR = (c: Char) ⇒ c > 0x20 && c != ':' && c != '>' && c != '"' && c != '{' && c != '}' && c != '<' && c != '\\' && c != '%'
  private final val IS_IRIREF_CHAR = (c: Char) ⇒ c > 0x20 && c != '>' && c != '"' && c != '{' && c != '}' && c != '<' && c != '\\' && c != '%'
  private final val IS_LITERAL_CHAR = (c: Char) ⇒ c != '"' && c != '\\' && c != '\n' && c != '\r'
  private final val IS_LONG_LITERAL_CHAR = (c: Char) ⇒ c != '"' && c != '\\'

  def strict: NtParser = new StrictParser
  def lenient: NtParser = new LenientParser

  private final class StrictParser extends NtParser {
    protected def LiteralNode(): Literal = ShortLiteralNode()
  }

  private final class LenientParser extends NtParser {
    protected def LiteralNode(): Literal = {
      if (peek == '"' && peekpeek == '"') LongLiteralNode()
      else ShortLiteralNode()
    }
  }
}
private[ntparser] trait NtParserCompanion {
  /**
   * Parse a file or resource, assuming UTF-8.
   *
   * @param fileName The name of a nt file or resource
   * @return An Iterator of all [[Statement]]s
   */
  final def apply(fileName: String): Iterator[Statement] =
    apply(fileName, Codec.UTF8)

  /**
   * Parse a file or resource in the given codec.
   *
   * @param fileName The name of a nt file or resource
   * @param codec The codec that will be used to open the file
   * @return An Iterator of all [[Statement]]s
   */
  final def apply(fileName: String, codec: Codec): Iterator[Statement] =
    apply(Loader.getLines(fileName, codec))

  /**
   * Parse lines from an InputStream, assuming UTF-8.
   *
   * @param is the InputStream
   * @return An Iterator of all [[Statement]]s
   */
  final def apply(is: InputStream): Iterator[Statement] =
    apply(is, Codec.UTF8)

  /**
   * Parse lines from an InputStream in the given coded.
   *
   * @param is the InputStream
   * @param codec The codec that will be used to read the stream
   * @return An Iterator of all [[Statement]]s
   */
  final def apply(is: InputStream, codec: Codec): Iterator[Statement] =
    apply(Loader.getLines(is, codec))

  /**
   * Parse lines from an Iterable of Strings.
   *
   * @param lines An Iterable of all lines of an nt document
   * @return An Iterator of all [[Statement]]s
   */
  final def apply(lines: GenIterable[String]): Iterator[Statement] =
    apply(lines.iterator)

  /**
   * Parse lines from an Iterator of Strings.
   *
   * @param lines An Iterator of all lines of an nt document
   * @return An Iterator of all [[Statement]]s
   */
  final def apply(lines: Iterator[String]): Iterator[Statement] =
    parsingIterator(lines)

  /**
   * Parse a file or resource, assuming UTF-8.
   *
   * @param fileName The name of a nt file or resource
   * @return An Iterator of all [[Statement]]s
   */
  final def parse(fileName: String): JIterator[Statement] =
    apply(fileName).asJava

  /**
   * Parse a file or resource in the given codec.
   *
   * @param fileName The name of a nt file or resource
   * @param encoding The encoding that will be used to open the file
   * @return An Iterator of all [[Statement]]s
   */
  final def parse(fileName: String, encoding: Charset): JIterator[Statement] =
    apply(fileName, Codec.charset2codec(encoding)).asJava

  /**
   * Parse lines from an InputStream, assuming UTF-8.
   *
   * @param is the InputStream
   * @return An Iterator of all [[Statement]]s
   */
  final def parse(is: InputStream): JIterator[Statement] =
    apply(is).asJava

  /**
   * Parse lines from an InputStream in the given coded.
   *
   * @param is the InputStream
   * @param encoding The encoding that will be used to read the stream
   * @return An Iterator of all [[Statement]]s
   */
  final def parse(is: InputStream, encoding: Charset): JIterator[Statement] =
    apply(is, Codec.charset2codec(encoding)).asJava

  /**
   * Parse lines from an Iterable of Strings.
   *
   * @param lines An Iterable of all lines of an nt document
   * @return An Iterator of all [[Statement]]s
   */
  final def parse(lines: JIterable[String]): JIterator[Statement] =
    apply(lines.iterator().asScala).asJava

  /**
   * Parse lines from an Iterator of Strings.
   *
   * @param lines An Iterator of all lines of an nt document
   * @return An Iterator of all [[Statement]]s
   */
  final def parse(lines: JIterator[String]): JIterator[Statement] =
    apply(lines.asScala).asJava

  /**
   * Closes all opened InputStreams.
   * Should be called only after parsing is finished and the
   * Statement Iterator is consumed.
   */
  final def close(): Unit =
    Loader.shutdown()

  protected def parsingIterator(lines: Iterator[String]): Iterator[Statement]
}

/**
 * Stops parsing and throws the ParseError on parse error.
 */
object StrictNtParser extends NtParserCompanion {

  protected def parsingIterator(lines: Iterator[String]): Iterator[Statement] =
    new ParsingIterator(NtParser.strict, lines)

  private class ParsingIterator(p: NtParser, underlying: Iterator[String]) extends Iterator[Statement] {
    private var nextStatement: Statement = _
    private var currentLineNo = 0

    def hasNext: Boolean = nextStatement ne null

    def next(): Statement = {
      if (nextStatement eq null) Iterator.empty.next()
      advance()
    }

    advance()

    @tailrec private def advance0(): Statement = {
      if (!underlying.hasNext) null
      else {
        currentLineNo += 1
        val nextStatement = p.parse(underlying.next(), currentLineNo)
        if (nextStatement eq null) advance0()
        else nextStatement
      }
    }

    private def advance(): Statement = {
      val before = nextStatement
      nextStatement = advance0()
      before
    }
  }
}

/**
 * Swallows parse errors and continues parsing.
 */
object NonStrictNtParser extends NtParserCompanion {

  protected def parsingIterator(lines: Iterator[String]): Iterator[Statement] =
    new ParsingIterator(NtParser.lenient, lines)

  private class ParsingIterator(p: NtParser, underlying: Iterator[String]) extends Iterator[Statement] {
    private var nextStatement: Statement = _
    private var currentLineNo = 0

    def hasNext: Boolean = nextStatement ne null

    def next(): Statement = {
      if (nextStatement eq null) Iterator.empty.next()
      advance()
    }

    advance()

    @tailrec private def advance0(): Statement = {
      if (!underlying.hasNext) null
      else {
        currentLineNo += 1
        val line = p.parseOrNull(underlying.next(), currentLineNo)
        if (line ne null) line
        else advance0()
      }
    }

    private def advance(): Statement = {
      val before = nextStatement
      nextStatement = advance0()
      before
    }
  }
}
