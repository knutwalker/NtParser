/*
 * Copyright 2015 Paul Horn
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

import org.scalatest.FunSuite

class NtParserSpec extends FunSuite {

  test("parse simple line") {
    val line = "_:abc <d:ef> \"ghi\" ."
    val statement = parse(line)
    assert(statement.s == BNode("abc"))
    assert(statement.p == Resource("d:ef"))
    assert(statement.o == Literal.simple("ghi"))
  }

  test("parse line with url encoding and language hint") {
    val line = """<http://de.dbpedia.org/resource/Wiera_%22Vera%22_Gran> <http://www.w3.org/2000/01/rdf-schema#label> "Wiera \"Vera\" Gran"@de . """
    val statement = parse(line)
    assert(statement.s == Resource("http://de.dbpedia.org/resource/Wiera_\"Vera\"_Gran"))
    assert(statement.p == Resource("http://www.w3.org/2000/01/rdf-schema#label"))
    assert(statement.o == Literal.tagged("Wiera \"Vera\" Gran", "de"))
  }

  test("parse line with unicode encoding") {
    val line = """<http://de.dbpedia.org/resource/Hofer_%22W""" + '\\' + """u00E4rschtlamo%22> <http://www.w3.org/2000/01/rdf-schema#label> "Hofer \"W""" + '\\' + """u00E4rschtl""" + '\\' + '\\' + """amo\""@de . """
    val statement = parse(line)
    assert(statement.s == Resource("http://de.dbpedia.org/resource/Hofer_\"Wärschtlamo\""))
    assert(statement.p == Resource("http://www.w3.org/2000/01/rdf-schema#label"))
    assert(statement.o == Literal.tagged("Hofer \"Wärschtl\\amo\"", "de"))
  }

  test("parse line with more encodings") {
    val line = """<http://de.dbpedia.org/resource/GIGA%5C%5CGAMES> <http://www.w3.org/2000/01/rdf-schema#label> "GIGA""" + '\\' + '\\' + '\\' + '\\' + """GAMES"@de ."""
    val statement = parse(line)
    assert(statement.s == Resource("http://de.dbpedia.org/resource/GIGA\\\\GAMES"))
    assert(statement.p == Resource("http://www.w3.org/2000/01/rdf-schema#label"))
    assert(statement.o == Literal.tagged("GIGA\\\\GAMES", "de"))
  }

  test("parse line with multi byte unicode encodings") {
    val line = """<http://de.dbpedia.org/resource/\U00010332\U0001033F\U00010344\U00010339\U00010343\U0001033A> <http://www.w3.org/2000/01/rdf-schema#label> "\U00010332\U0001033F\U00010344\U00010339\U00010343\U0001033A"@de ."""
    val statement = parse(line)

    val encoded = {
      val cs = new Array[Char](12)
      val c1 = Character.toChars(Integer.parseInt("00010332", 16))
      System.arraycopy(c1, 0, cs, 0, 2)
      val c2 = Character.toChars(Integer.parseInt("0001033F", 16))
      System.arraycopy(c2, 0, cs, 2, 2)
      val c3 = Character.toChars(Integer.parseInt("00010344", 16))
      System.arraycopy(c3, 0, cs, 4, 2)
      val c4 = Character.toChars(Integer.parseInt("00010339", 16))
      System.arraycopy(c4, 0, cs, 6, 2)
      val c5 = Character.toChars(Integer.parseInt("00010343", 16))
      System.arraycopy(c5, 0, cs, 8, 2)
      val c6 = Character.toChars(Integer.parseInt("0001033A", 16))
      System.arraycopy(c6, 0, cs, 10, 2)
      new String(cs)
    }

    assert(statement.s == Resource("http://de.dbpedia.org/resource/" + encoded))
    assert(statement.p == Resource("http://www.w3.org/2000/01/rdf-schema#label"))
    assert(statement.o == Literal.tagged(encoded, "de"))
  }

  test("parse many lines") {
    val ManyUgly = List(
      """<http://de.dbpedia.org/resource/11'09%2201_""" + '\\' + """u2013_September_11> <http://www.w3.org/2000/01/rdf-schema#label> "11'09\"01 """ + '\\' + """u2013 September 11"@de .""",
      """<http://de.dbpedia.org/resource/Hofer_%22W""" + '\\' + """u00E4rschtlamo%22> <http://www.w3.org/2000/01/rdf-schema#label> "Hofer \"W""" + '\\' + """u00E4rschtlamo\""@de .""",
      """<http://de.dbpedia.org/resource/Hochschule_f""" + '\\' + """u00FCr_Musik_%22Hanns_Eisler%22> <http://www.w3.org/2000/01/rdf-schema#label> "Hochschule f""" + '\\' + """u00FCr Musik \"Hanns Eisler\""@de .""",
      """<http://de.dbpedia.org/resource/Kommunistischer_Jugendverband_Deutschlands_(Zentralorgan_%22K""" + '\\' + """u00E4mpfende_Jugend%22)> <http://www.w3.org/2000/01/rdf-schema#label> "Kommunistischer Jugendverband Deutschlands (Zentralorgan \"K""" + '\\' + """u00E4mpfende Jugend\")"@de .""",
      """<http://de.dbpedia.org/resource/Hochschule_f""" + '\\' + """u00FCr_Musik_%22Carl_Maria_von_Weber%22> <http://www.w3.org/2000/01/rdf-schema#label> "Hochschule f""" + '\\' + """u00FCr Musik \"Carl Maria von Weber\""@de .""")
    val expecteds = List(
      Triple(
        Resource("http://de.dbpedia.org/resource/11'09\"01_–_September_11"),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal.tagged("11'09\"01 – September 11", "de")
      ),
      Triple(
        Resource("http://de.dbpedia.org/resource/Hofer_\"Wärschtlamo\""),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal.tagged("Hofer \"Wärschtlamo\"", "de")
      ),
      Triple(
        Resource("http://de.dbpedia.org/resource/Hochschule_für_Musik_\"Hanns_Eisler\""),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal.tagged("Hochschule für Musik \"Hanns Eisler\"", "de")
      ),
      Triple(
        Resource("http://de.dbpedia.org/resource/Kommunistischer_Jugendverband_Deutschlands_(Zentralorgan_\"Kämpfende_Jugend\")"),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal.tagged("Kommunistischer Jugendverband Deutschlands (Zentralorgan \"Kämpfende Jugend\")", "de")
      ),
      Triple(
        Resource("http://de.dbpedia.org/resource/Hochschule_für_Musik_\"Carl_Maria_von_Weber\""),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal.tagged("Hochschule für Musik \"Carl Maria von Weber\"", "de")
      )
    )

    val statements = NonStrictNtParser(ManyUgly).toList
    statements.zip(expecteds) foreach {
      case (actual, expected) ⇒
        assert(actual == expected)
    }
  }

  test("parse weird things") {
    val line = """<t:%B2t> <:""" + '\\' + """uFFFFy> <d:> ."""
    val statement = parse(line)
    val bs = Array(116.toByte, 58.toByte, -17.toByte, -65.toByte, -67.toByte, 116.toByte)
    assert(statement.s == Resource(new String(bs)))
    assert(statement.p == Resource(new String(Array(':', 65535.toChar, 'y'))))
    assert(statement.o == Resource("d:"))
  }

  private def parse(line: String) = {
    val parser = NtParser.strict
    parser.parse(line)
  }
}
