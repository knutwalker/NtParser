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
import sun.misc.Unsafe

import java.lang.StringBuilder
import scala.annotation.{switch, tailrec}
import scala.io.Codec
import scala.util.Try

/**
 * An NtParser always parses a single line in different modes, depending
 * on the method that was called.
 *
 * @define swallowsErrors
 *         This methods will swallow parse errors and not
 *         throw any [[ParseError]]s
 */
final class NtParserB {
  import de.knutwalker.ntparser.NtParserB._

  val logger = LoggerFactory.getLogger(classOf[NtParserB])

  var input: Array[Char] = new Array[Char](1024)

  var pos = 0
  var max = 0
  var cursor: Char = 0
  var lineNo = -1

  val sb = new StringBuilder
  val statement: Array[Node] = new Array[Node](3)

  /**
   * Parse a single line into a [[Statement]].
   *
   * $swallowsErrors
   *
   * @param line A string that may contain one [[Statement]]
   * @return Some(statement) if a statement could be parsed, otherwise None
   */
  def parseOpt(line: String): Option[Statement] = Option(parseOrNull(line))

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
  def parseOpt(line: String, lineNumber: Int): Option[Statement] = Option(parseOrNull(line, lineNumber))

  /**
   * Parse a single line into a [[Statement]].
   *
   * $swallowsErrors
   *
   * @param line A string that may contain one [[Statement]]
   * @return Success(statement) if a statement could be parsed, otherwise Failure(parseError)
   */
  def parseTry(line: String): Try[Option[Statement]] = Try(Option(parse(line)))

  /**
   * Parse a single line into a [[Statement]] that is
   * part of a bigger file at some location.
   *
   * @param line A string that may contain one [[Statement]]
   * @param lineNumber The current line number
   * @return Success(statement) if a statement could be parsed, otherwise Failure(parseError)
   */
  def parseTry(line: String, lineNumber: Int): Try[Option[Statement]] = Try(Option(parse(line, lineNumber)))

  /**
   * Parse a single line into a [[Statement]].
   *
   * @param line A string that may contain one [[Statement]]
   * @return The [[Statement]] if it could be parsed, or null otherwise
   */
  def parseOrNull(line: String): Statement = {
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
  def parseOrNull(line: String, lineNumber: Int): Statement = {
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
  def parse(line: String): Statement = {
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
  def parse(line: String, lineNumber: Int): Statement = {
    lineNo = lineNumber
    if (line.isEmpty) null
    else {
      reset(line)
      Line()
    }
  }

  def Line(): Statement = {
    ws()
    (cursor: @switch) match {
      case '<'      ⇒ TripleLine()
      case '_'      ⇒ TripleLine()
      case '#'      ⇒ // comment line
      case '\u0000' ⇒ // empty line, inline char to allow switch statement
      case _        ⇒ error(LINE_BEGIN)
    }

    if ((statement(0) ne null) && (statement(1) ne null) && (statement(2) ne null)) {
      Triple(statement(0), statement(1).asInstanceOf[Resource], statement(2))
    }
    else null
  }

  def TripleLine(): Boolean = {
    Subject()
    Predicate()
    Object()
    ws('.') || error('.')
  }

  def Subject(): Unit = {
    (cursor: @switch) match {
      case '<' ⇒ statement(0) = IriRef()
      case '_' ⇒ statement(0) = NamedNode()
      case _   ⇒ error(SUBJECT_BEGIN)
    }
  }

  def Predicate(): Unit = {
    statement(1) = IriRef()
  }

  def Object(): Unit = {
    (cursor: @switch) match {
      case '<' ⇒ statement(2) = IriRef()
      case '_' ⇒ statement(2) = NamedNode()
      case '"' ⇒ statement(2) = LiteralNode()
      case _   ⇒ error(OBJECT_BEGIN)
    }
  }

  def IriRef(): Resource = {
    mustAdvance('<')
    IriScheme()
    IriRefCharacters()
    mustAdvance('>')
    ws()
    Resource(clear())
  }

  def NamedNode(): BNode = {
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

  def LiteralNode(): Literal = {
    mustAdvance('"')
    LiteralCharacters()
    mustAdvance('"')
    val value = clear()
    val lit = (cursor: @switch) match {
      case '@' ⇒ LangLiteral(value)
      case '^' ⇒ TypedLiteral(value)
      case _   ⇒ Literal.simple(value)
    }
    ws()
    lit
  }

  @tailrec def IriScheme(): Unit = {
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

  @tailrec def IriRefCharacters(): Unit = {
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

  @tailrec def LiteralCharacters(): Unit = {
    captureWhile(IS_LITERAL_CHAR)
    (cursor: @switch) match {
      case '"' ⇒ //string finish
      case '\\' ⇒
        SlashEscapedCharacter()
        LiteralCharacters()
      case _ ⇒ error(LITERAL_CHARACTERS)
    }
  }

  def TypedLiteral(value: String): Literal = {
    advance("^^") || error('^')
    Literal.typed(value, IriRef())
  }

  def LangLiteral(value: String): Literal = {
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

  def ExtendedLangLiteral(value: String): Literal = {
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

  def UnicodeEscapedCharacter(): Unit = {
    mustAdvance('\\')
    (cursor: @switch) match {
      case 'u' ⇒ Unicode()
      case 'U' ⇒ SuperUnicode()
      case _   ⇒ error(UNICODE_ESCAPE_CHARS)
    }
  }

  def SlashEscapedCharacter(): Unit = {
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

  def Unicode(): Unit = {
    mustAdvance('u')
    append(captureUnicodeDigits())
  }

  def captureUnicodeDigits(): Char = (
    captureHexDigit() * 4096 +
    captureHexDigit() * 256 +
    captureHexDigit() * 16 +
    captureHexDigit()).toChar

  def SuperUnicode(): Unit = {
    mustAdvance('U')
    append(captureSuperUnicodeDigits())
  }

  def captureSuperUnicodeDigits(): Int =
    captureHexDigit() * 268435456 +
      captureHexDigit() * 16777216 +
      captureHexDigit() * 1048576 +
      captureHexDigit() * 65536 +
      captureHexDigit() * 4096 +
      captureHexDigit() * 256 +
      captureHexDigit() * 16 +
      captureHexDigit()

  def captureHexDigit(): Int = {
    IS_HEX_CHAR(cursor) || error("hex character")
    val r = hexValue(cursor)
    advance()
    r
  }

  def hexValue(c: Char): Int =
    (c & 0x1f) + ((c >> 6) * 0x19) - 0x10

  def PercentEscapedCharacter(): Unit = {
    mustAdvance('%')
    append(percentEscaped0(0, new Array[Byte](1)))
  }

  @tailrec def percentEscaped0(idx: Int, buf: Array[Byte]): Array[Byte] = {
    buf(idx) = capturePercentDigits()
    cursor match {
      case '%' ⇒
        advance()
        percentEscaped0(idx + 1, grow(buf, idx + 2))
      case _ ⇒ buf
    }
  }

  def capturePercentDigits(): Byte = (
    captureHexDigit() * 16 +
    captureHexDigit()).toByte

  def captureWhile(f: Char ⇒ Boolean): Boolean = {
    capture0(f)
    f(cursor)
  }

  @tailrec def capture0(f: Char ⇒ Boolean): Boolean = {
    if (f(cursor)) append()
    advance(f) && capture0(f)
  }

  @tailrec def ws(): Boolean = {
    if (IS_WHITESPACE(cursor)) advance() && ws()
    else true
  }

  def ws(c: Char): Boolean = {
    ws()
    advance(c)
  }

  def mustAdvance(c: Char): Boolean =
    advance(c) || error(c)

  def advance(f: Char ⇒ Boolean): Boolean = {
    f(cursor) && advance()
  }

  def advance(s: String): Boolean = {
    s forall advance
  }

  def advance(c: Char): Boolean = {
    cursor == c && advance()
  }

  def advance(): Boolean = {
    val m = max
    if (pos < m) {
      val c = pos + 1
      cursor = if (c == m) END else input(c)
      pos = c
      true
    }
    else false
  }

  def error(c: Char): Boolean =
    error(Array(c))

  def error(c: Array[Char]): Boolean = {
    val expected = c.length match {
      case 0 ⇒ "n/a"
      case 1 ⇒ c.head.toString
      case n ⇒ s"${c.init.mkString(", ")}, or ${c.last}"
    }
    error(expected)
  }

  def error(s: String): Boolean = {
    val cursorChar = cursor match {
      case END ⇒ "EOI"
      case x   ⇒ x.toString
    }
    val lineHint = if (lineNo == -1) " " else s" in line $lineNo "
    throwError(s"parse error${lineHint}at char ${pos + 1}, expected [$s], but found [$cursorChar]")
  }

  def validationError(s: String): Boolean = {
    val lineHint = if (lineNo == -1) " " else s" in line $lineNo "
    throwError(s"parse error${lineHint}at char ${pos + 1}, $s")
  }

  def throwError(text: String): Boolean = {
    val line = new String(input, 0, max)
    val mark = (List.fill(pos)(' ') ::: '^' :: Nil).mkString

    throw new ParseError((text :: line :: mark :: Nil).mkString("\n"))
  }

  def clear(): String = {
    val r = sb.toString
    sb setLength 0
    r
  }

  def reset(forLine: String): Unit = {
    statement(0) = null
    statement(1) = null
    statement(2) = null

    sb setLength 0

    pos = 0
    max = forLine.length

    slowerGetChars(forLine)

    cursor = input(0)
  }

  def slowerGetChars(from: String): Unit = {
    grow(max)
    from.getChars(0, max, input, 0)
  }

  def fastGetChars(from: String): Unit = {
    input = fastCharArray(from)
  }

  def append(): Unit = append(cursor)

  def append(c: Char): Unit = sb append c

  def append(bs: Array[Byte]): Unit = append(Codec.fromUTF8(bs))

  def append(cs: Array[Char]): Unit = sb append cs

  def append(cp: Int): Unit = sb appendCodePoint cp

  def oversize(minTargetSize: Int): Int = {
    if (minTargetSize == 0) 0
    else {
      val extra = (minTargetSize >> 3) max 3
      val newSize = minTargetSize + extra

      (newSize + 3) & 0x7ffffffc
    }
  }

  def grow(minSize: Int): Unit = {
    assert(minSize >= 0, "size must be positive (got " + minSize + "): likely integer overflow?")
    val array = input
    if (array.length < minSize) {
      val newArray: Array[Char] = new Array[Char](oversize(minSize))
      System.arraycopy(array, 0, newArray, 0, array.length)
      input = newArray
    }
  }

  def grow(oldArray: Array[Byte], to: Int): Array[Byte] = {
    assert(to >= 0, "size must be positive (got " + to + "): likely integer overflow?")
    if (oldArray.length < to) {
      val newArray = new Array[Byte](to)
      System.arraycopy(oldArray, 0, newArray, 0, oldArray.length)
      newArray
    }
    else oldArray
  }
}
object NtParserB {
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

  private[this] val unsafe = scala.concurrent.util.Unsafe.instance

  private[this] val stringValueOffset = unsafe.objectFieldOffset(classOf[String].getDeclaredField("value"))

  def fastCharArray(input: String) : Array[Char] =
    unsafe.getObject(input, stringValueOffset).asInstanceOf[Array[Char]]

}
