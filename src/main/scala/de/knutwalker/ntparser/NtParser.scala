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

import java.io.InputStream
import java.lang.{ StringBuilder, Iterable ⇒ JIterable }
import java.nio.charset.Charset
import java.util.{ Iterator ⇒ JIterator }
import scala.annotation.{ switch, tailrec }
import scala.collection.GenIterable
import scala.collection.JavaConverters.{ asJavaIteratorConverter, asScalaIteratorConverter, iterableAsScalaIterableConverter }
import scala.collection.mutable.ListBuffer
import scala.io.Codec
import scala.util.Try

final class NtParser {

  private[this] val logger = LoggerFactory.getLogger(classOf[NtParser])

  private[this] var input: Array[Char] = new Array[Char](1024)

  private[this] var pos = 0
  private[this] var max = 0
  private[this] var cursor: Char = 0
  private[this] var lineNo = -1

  private[this] val sb = new StringBuilder
  private[this] val nodes: ListBuffer[Node] = ListBuffer.empty
  private[this] val statement: Array[Node] = new Array[Node](3)

  def parseOpt(line: String): Option[Statement] = Option(parseOrNull(line))

  def parseOpt(line: String, lineNumber: Int): Option[Statement] = Option(parseOrNull(line, lineNumber))

  def parseTry(line: String): Try[Option[Statement]] = Try(Option(parse(line)))

  def parseTry(line: String, lineNumber: Int): Try[Option[Statement]] = Try(Option(parse(line, lineNumber)))

  def parseOrNull(line: String): Statement = {
    try parse(line) catch {
      case pe: ParseError ⇒
        logger.warn(pe.getMessage, pe)
        null
    }
  }

  def parseOrNull(line: String, lineNumber: Int): Statement = {
    try parse(line, lineNo) catch {
      case pe: ParseError ⇒
        logger.warn(pe.getMessage, pe)
        null
    }
  }

  @throws[ParseError]("ParseError if a line could not be parsed")
  def parse(line: String): Statement = {
    lineNo = -1
    if (line.isEmpty) null
    else {
      reset(line)
      Line()
    }
  }

  @throws[ParseError]("ParseError if a line could not be parsed")
  def parse(line: String, lineNumber: Int): Statement = {
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

    if ((statement(0) ne null) && (statement(1) ne null) && (statement(2) ne null)) {
      Triple(statement(0), statement(1).asInstanceOf[Resource], statement(2))
    }
    else null
  }

  private[this] def TripleLine() = {
    Subject()
    Predicate()
    Object()
    ws('.') || error('.')
  }

  private[this] def Subject(): Unit = {
    (cursor: @switch) match {
      case '<' ⇒ statement(0) = IriRef()
      case '_' ⇒ statement(0) = NamedNode()
      case _   ⇒ error(SUBJECT_BEGIN)
    }
  }

  private[this] def Predicate(): Unit = {
    statement(1) = IriRef()
  }

  private[this] def Object(): Unit = {
    (cursor: @switch) match {
      case '<' ⇒ statement(2) = IriRef()
      case '_' ⇒ statement(2) = NamedNode()
      case '"' ⇒ statement(2) = LiteralNode()
      case _   ⇒ error(OBJECT_BEGIN)
    }
  }

  private[this] def IriRef(): Resource = {
    advance('<') || error('<')
    IriScheme()
    IriRefCharacters() // captureWhile(IS_URI_CHAR)
    advance('>') || error('>')
    ws()
    Resource(clear())
  }

  private[this] def NamedNode(): BNode = {
    advance('_') || error('_')
    advance(':') || error(':')
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

  private[this] def LiteralNode(): Literal = {
    advance('"') || error('"')
    LiteralCharacters() // captureWhile(IS_LITERAL_CHAR)
    advance('"') || error('"')
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
    val IS_SCHEMA_CHAR = (c: Char) ⇒ c > 0x20 && c != ':' && c != '>' && c != '"' && c != '{' && c != '}' && c != '<' && c != '\\' && c != '%'
    captureWhile(IS_SCHEMA_CHAR) // TODO: static
    (cursor: @switch) match {
      case ':' ⇒ // scheme finish
      case '\\' ⇒
        UnicodeEscapedCharacter()
        IriScheme()
      case '%' ⇒
        PercentEscapedCharacter()
        IriScheme() // TODO: what for n3 representation?
      case _ ⇒ validationError(s"<${clear()}> is not absolute")
    }
  }

  @tailrec private[this] def IriRefCharacters(): Unit = {
    val IS_IRIREF_CHAR = (c: Char) ⇒ c > 0x20 && c != '>' && c != '"' && c != '{' && c != '}' && c != '<' && c != '\\' && c != '%'
    captureWhile(IS_IRIREF_CHAR) // TODO: static
    (cursor: @switch) match {
      case '>' ⇒ // iriref finish
      case '\\' ⇒
        UnicodeEscapedCharacter()
        IriRefCharacters()
      case '%' ⇒
        PercentEscapedCharacter()
        IriRefCharacters() // TODO: what for n3 representation?
      case _ ⇒ error(Array('>', '\\', '%')) // TODO: NORMAL_IRIREF_CHARS
    }
  }

  @tailrec private[this] def LiteralCharacters(): Unit = {
    val IS_LITERAL_CHAR = (c: Char) ⇒ c != '"' && c != '\\' && c != '\n' && c != '\r'
    captureWhile(IS_LITERAL_CHAR) // TODO: static
    (cursor: @switch) match {
      case '"' ⇒ //string finish
      case '\\' ⇒
        SlashEscapedCharacter()
        LiteralCharacters()
      case _ ⇒ error(Array('"', '\\')) // TODO: NORMAL_LITERAL_CHARS
    }
  }

  private[this] def TypedLiteral(value: String) = {
    advance("^^") || error('^')
    Literal.typed(value, IriRef())
  }

  private[this] def LangLiteral(value: String): Literal = {
    advance('@') || error('@')
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
    advance('-') || error('-')
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
    advance('\\') || error('\\') // TODO: advance && error => mustAdvance
    (cursor: @switch) match {
      case 'u' ⇒ Unicode()
      case 'U' ⇒ SuperUnicode()
      case _   ⇒ error(Array('u', 'U')) // TODO: UNICODE_ESCAPE_SEQUENCE_CHARS
    }
  }

  private[this] def SlashEscapedCharacter(): Unit = {
    advance('\\') || error('\\') // TODO: advance && error => mustAdvance
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
      case _   ⇒ error(Array('\\', '"', '\'', 'b', 't', 'n', 'f', 'r', 'u', 'U')) // TODO: ESCAPE_SEQUENCE_CHARS
    }
  }

  private[this] def Unicode(): Unit = {
    advance('u') || error('u')
    append(captureUnicodeDigits())
  }

  private[this] def captureUnicodeDigits(): Char = (
    captureHexDigit() * 4096 +
    captureHexDigit() * 256 +
    captureHexDigit() * 16 +
    captureHexDigit()).toChar

  private[this] def SuperUnicode(): Unit = {
    advance('U') || error('U')
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

  private[this] def hexValue(c: Char): Int =
    (c & 0x1f) + ((c >> 6) * 0x19) - 0x10

  private[this] def PercentEscapedCharacter(): Unit = {
    advance('%') || error('%') // TODO: advance && error => mustAdvance
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

  @tailrec private[this] def ws(): Boolean = {
    if (IS_WHITESPACE(cursor)) advance() && ws()
    else true
  }

  private[this] def ws(c: Char): Boolean = {
    ws()
    advance(c)
  }

  private[this] def advance(f: Char ⇒ Boolean): Boolean = {
    f(cursor) && advance()
  }

  private[this] def advance(s: String): Boolean = {
    s forall advance
  }

  private[this] def advance(c: Char): Boolean = {
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

  private[this] def error(c: Char): Boolean =
    error(Array(c))

  private[this] def error(c: Array[Char]): Boolean = {
    val expected = c.length match {
      case 0 ⇒ "n/a"
      case 1 ⇒ c.head.toString
      case n ⇒ s"${c.init.mkString(", ")}, or ${c.last}"
    }
    error(expected)
  }

  private[this] def error(s: String): Boolean = {
    val cursorChar = cursor match {
      case END ⇒ "EOI"
      case x   ⇒ x.toString
    }
    val lineHint = if (lineNo == -1) " " else s" in line $lineNo "
    throwError(s"parse error${lineHint}at char ${pos + 1}, expected [$s], but found [$cursorChar]")
  }

  private[this] def validationError(s: String): Boolean = {
    val lineHint = if (lineNo == -1) " " else s" in line $lineNo "
    throwError(s"parse error${lineHint}at char ${pos + 1}, $s")
  }

  private[this] def throwError(text: String): Boolean = {
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
    statement(0) = null
    statement(1) = null
    statement(2) = null

    nodes.clear()
    sb setLength 0

    pos = 0
    max = forLine.length
    grow(max)
    forLine.getChars(0, max, input, 0)

    cursor = input(0)
  }

  private[this] def append(): Unit = append(cursor)

  private[this] def append(c: Char): Unit = sb append c

  private[this] def append(bs: Array[Byte]): Unit = append(Codec.fromUTF8(bs))

  private[this] def append(cs: Array[Char]): Unit = sb append cs

  private[this] def append(cp: Int): Unit = sb appendCodePoint cp

  private[this] def oversize(minTargetSize: Int): Int = {
    if (minTargetSize == 0) 0
    else {
      val extra = (minTargetSize >> 3) max 3
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

  private[this] val END = Char.MinValue
  private[this] val WHITESPACE = Array(' ', '\t')
  private[this] val LINE_BEGIN = Array('<', '_', '#')
  private[this] val SUBJECT_BEGIN = Array('<', '_')
  private[this] val OBJECT_BEGIN = Array('<', '_', '"')
  private[this] val IS_WHITESPACE = (c: Char) ⇒ c == ' ' || c == '\t'
  private[this] val IS_NAME_START = (c: Char) ⇒ (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
  private[this] val IS_NAME_CHAR = (c: Char) ⇒ IS_NAME_START(c) || c >= '0' && c <= '9'
  private[this] val IS_HEX_CHAR = (c: Char) ⇒ (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
}

trait NtParserCompanion {
  final def apply(fileName: String): Iterator[Statement] =
    apply(fileName, Codec.UTF8)

  final def apply(fileName: String, codec: Codec): Iterator[Statement] =
    apply(Loader.getLines(fileName, codec))

  final def apply(is: InputStream): Iterator[Statement] =
    apply(is, Codec.UTF8)

  final def apply(is: InputStream, codec: Codec): Iterator[Statement] =
    apply(Loader.getLines(is, codec))

  final def apply(lines: GenIterable[String]): Iterator[Statement] =
    apply(lines.iterator)

  final def apply(lines: Iterator[String]): Iterator[Statement] =
    parsingIterator(new NtParser, lines)

  final def parse(fileName: String): JIterator[Statement] =
    apply(fileName).asJava

  final def parse(fileName: String, encoding: Charset): JIterator[Statement] =
    apply(fileName, Codec.charset2codec(encoding)).asJava

  final def parse(is: InputStream): JIterator[Statement] =
    apply(is).asJava

  final def parse(is: InputStream, encoding: Charset): JIterator[Statement] =
    apply(is, Codec.charset2codec(encoding)).asJava

  final def parse(lines: JIterable[String]): JIterator[Statement] =
    apply(lines.asScala).asJava

  final def parse(lines: JIterator[String]): JIterator[Statement] =
    apply(lines.asScala).asJava

  protected def parsingIterator(parser: NtParser, lines: Iterator[String]): Iterator[Statement]
}

object StrictNtParser extends NtParserCompanion {

  protected def parsingIterator(parser: NtParser, lines: Iterator[String]): Iterator[Statement] =
    new ParsingIterator(parser, lines)

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

object NonStrictNtParser extends NtParserCompanion {

  protected def parsingIterator(parser: NtParser, lines: Iterator[String]): Iterator[Statement] =
    new ParsingIterator(parser, lines)

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
