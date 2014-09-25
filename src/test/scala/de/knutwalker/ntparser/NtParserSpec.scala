package de.knutwalker.ntparser

import org.scalatest.FunSuite

class NtParserSpec extends FunSuite {

  test("parse simple line") {
    val line = "_:abc <def> \"ghi\" ."
    val statement = parse(line)
    assert(statement.s == BNode("abc"))
    assert(statement.p == Resource("def"))
    assert(statement.o == Literal("ghi", None, None))
  }

  test("parse line with url encoding and language hint") {
    val line = """<http://de.dbpedia.org/resource/Wiera_%22Vera%22_Gran> <http://www.w3.org/2000/01/rdf-schema#label> "Wiera \"Vera\" Gran"@de . """
    val statement = parse(line)
    assert(statement.s == Resource("http://de.dbpedia.org/resource/Wiera_\"Vera\"_Gran"))
    assert(statement.p == Resource("http://www.w3.org/2000/01/rdf-schema#label"))
    assert(statement.o == Literal("Wiera \"Vera\" Gran", Some("de"), None))
  }

  test("parse line with unicode encoding") {
    val line = """<http://de.dbpedia.org/resource/Hofer_%22W""" + '\\' + """u00E4rschtlamo%22> <http://www.w3.org/2000/01/rdf-schema#label> "Hofer \"W""" + '\\' + """u00E4rschtl""" + '\\' + '\\' + """amo\""@de . """
    val statement = parse(line)
    assert(statement.s == Resource("http://de.dbpedia.org/resource/Hofer_\"Wärschtlamo\""))
    assert(statement.p == Resource("http://www.w3.org/2000/01/rdf-schema#label"))
    assert(statement.o == Literal("Hofer \"Wärschtl\\amo\"", Some("de"), None))
  }

  test("parse line with more encodings") {
    val line = """<http://de.dbpedia.org/resource/GIGA%5C%5CGAMES> <http://www.w3.org/2000/01/rdf-schema#label> "GIGA""" + '\\' + '\\' + '\\' + '\\' + """GAMES"@de ."""
    val statement = parse(line)
    assert(statement.s == Resource("http://de.dbpedia.org/resource/GIGA\\\\GAMES"))
    assert(statement.p == Resource("http://www.w3.org/2000/01/rdf-schema#label"))
    assert(statement.o == Literal("GIGA\\\\GAMES", Some("de"), None))
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
    assert(statement.o == Literal(encoded, Some("de"), None))
  }

  // TODO: implement http://www.w3.org/2013/N-TriplesTests/

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
        Literal("11'09\"01 – September 11", Some("de"), None)
      ),
      Triple(
        Resource("http://de.dbpedia.org/resource/Hofer_\"Wärschtlamo\""),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal("Hofer \"Wärschtlamo\"", Some("de"), None)
      ),
      Triple(
        Resource("http://de.dbpedia.org/resource/Hochschule_für_Musik_\"Hanns_Eisler\""),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal("Hochschule für Musik \"Hanns Eisler\"", Some("de"), None)
      ),
      Triple(
        Resource("http://de.dbpedia.org/resource/Kommunistischer_Jugendverband_Deutschlands_(Zentralorgan_\"Kämpfende_Jugend\")"),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal("Kommunistischer Jugendverband Deutschlands (Zentralorgan \"Kämpfende Jugend\")", Some("de"), None)
      ),
      Triple(
        Resource("http://de.dbpedia.org/resource/Hochschule_für_Musik_\"Carl_Maria_von_Weber\""),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal("Hochschule für Musik \"Carl Maria von Weber\"", Some("de"), None)
      )
    )

    val statements = NtParser(ManyUgly).toList
    statements.zip(expecteds) foreach {
      case (actual, expected) ⇒
        assert(actual == expected)
    }
  }

  test("parse weird things") {
    val line = """<t%B2t\r> <""" + '\\' + """uFFFFy> <d> ."""
    val statement = parse(line)
    val bs = Array(116.toByte, -17.toByte, -65.toByte, -67.toByte, 116.toByte, 13.toByte)
    assert(statement.s == Resource(new String(bs)))
    assert(statement.p == Resource(new String(Array(65535.toChar, 'y'))))
    assert(statement.o == Resource("d"))
  }

  private def parse(line: String) = {
    val parser = new NtParser
    parser.parse(line)
  }
}
