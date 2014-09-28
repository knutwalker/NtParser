package de.knutwalker.ntparser

import org.scalatest.FunSuite

class W3CTestSpec extends FunSuite {

  test("Empty file (nt-syntax-file-01)") {
    val statements = StrictNtParser("nt-syntax-file-01.nt").toList
    assert(statements.isEmpty)
  }

  test("Only comment (nt-syntax-file-02)") {
    val statements = StrictNtParser("nt-syntax-file-02.nt").toList
    assert(statements.isEmpty)
  }

  test("One comment, one empty line (nt-syntax-file-03)") {
    val statements = StrictNtParser("nt-syntax-file-03.nt").toList
    assert(statements.isEmpty)
  }

  test("Only IRIs (nt-syntax-uri-01)") {
    val statements = StrictNtParser("nt-syntax-uri-01.nt").toList
    assert(statements == List(Triple(Resource("http://example/s"), Resource("http://example/p"), Resource("http://example/o"))))
  }

  test("IRIs with Unicode escape (nt-syntax-uri-02)") {
    val statements = StrictNtParser("nt-syntax-uri-02.nt").toList
    assert(statements == List(Triple(Resource("http://example/S"), Resource("http://example/p"), Resource("http://example/o"))))
  }

  test("IRIs with long Unicode escape (nt-syntax-uri-03)") {
    val statements = StrictNtParser("nt-syntax-uri-03.nt").toList
    assert(statements == List(Triple(Resource("http://example/S"), Resource("http://example/p"), Resource("http://example/o"))))
  }

  test("Legal IRIs (nt-syntax-uri-04)") {
    val statements = StrictNtParser("nt-syntax-uri-04.nt").toList
    assert(statements == List(
      Triple(
        Resource("http://example/s"),
        Resource("http://example/p"),
        Resource("scheme:!$%&'()*+,-./0123456789:/@ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~?#")
      )))
  }

  test("string literal (nt-syntax-string-01)") {
    val statements = StrictNtParser("nt-syntax-string-01.nt").toList
    assert(statements == List(Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.simple("string"))))
  }

  test("langString literal (nt-syntax-string-02)") {
    val statements = StrictNtParser("nt-syntax-string-02.nt").toList
    assert(statements == List(Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.tagged("string", "en"))))
  }

  test("langString literal with region (nt-syntax-string-03)") {
    val statements = StrictNtParser("nt-syntax-string-03.nt").toList
    assert(statements == List(Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.tagged("string", "en-uk"))))
  }

  test("string literal with escaped newline (nt-syntax-str-esc-01)") {
    val statements = StrictNtParser("nt-syntax-str-esc-01.nt").toList
    assert(statements == List(Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.simple("a\n"))))
  }

  test("string literal with Unicode escape (nt-syntax-str-esc-02)") {
    val statements = StrictNtParser("nt-syntax-str-esc-02.nt").toList
    assert(statements == List(Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.simple("a b"))))
  }

  test("string literal with long Unicode escape (nt-syntax-str-esc-03)") {
    val statements = StrictNtParser("nt-syntax-str-esc-03.nt").toList
    assert(statements == List(Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.simple("a b"))))
  }

  test("bnode subject (nt-syntax-bnode-01)") {
    val statements = StrictNtParser("nt-syntax-bnode-01.nt").toList
    assert(statements == List(Triple(BNode("a"), Resource("http://example/p"), Resource("http://example/o"))))
  }

  test("bnode object (nt-syntax-bnode-02)") {
    val statements = StrictNtParser("nt-syntax-bnode-02.nt").toList
    assert(statements == List(
      Triple(Resource("http://example/s"), Resource("http://example/p"), BNode("a")),
      Triple(BNode("a"), Resource("http://example/p"), Resource("http://example/o"))
    ))
  }

  test("Blank node labels may start with a digit (nt-syntax-bnode-03)") {
    val statements = StrictNtParser("nt-syntax-bnode-03.nt").toList
    assert(statements == List(
      Triple(Resource("http://example/s"), Resource("http://example/p"), BNode("1a")),
      Triple(BNode("1a"), Resource("http://example/p"), Resource("http://example/o"))
    ))
  }

  test("xsd:byte literal (nt-syntax-datatypes-01)") {
    val statements = StrictNtParser("nt-syntax-datatypes-01.nt").toList
    assert(statements == List(Triple(
      Resource("http://example/s"),
      Resource("http://example/p"),
      Literal.typed("123", Resource("http://www.w3.org/2001/XMLSchema#byte"))
    )))
  }

  test("integer as xsd:string (nt-syntax-datatypes-02)") {
    val statements = StrictNtParser("nt-syntax-datatypes-02.nt").toList
    assert(statements == List(Triple(
      Resource("http://example/s"),
      Resource("http://example/p"),
      Literal.typed("123", Resource("http://www.w3.org/2001/XMLSchema#string"))
    )))
  }

  test("Bad IRI : space (negative test) (nt-syntax-bad-uri-01)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-uri-01.nt").toList
    }
    assert(error.getMessage.contains("but found [ ]"))
  }

  test("Bad IRI : bad escape (negative test) (nt-syntax-bad-uri-02)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-uri-02.nt").toList
    }
    assert(error.getMessage.contains("expected [hex character], but found [Z]"))
  }

  test("Bad IRI : bad long escape (negative test) (nt-syntax-bad-uri-03)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-uri-03.nt").toList
    }
    assert(error.getMessage.contains("expected [hex character], but found [Z]"))
  }

  test("Bad IRI : character escapes not allowed (negative test) (nt-syntax-bad-uri-04)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-uri-04.nt").toList
    }
    assert(error.getMessage.contains("expected [u, or U], but found [n]"))
  }

  test("Bad IRI : character escapes not allowed (2) (negative test) (nt-syntax-bad-uri-05)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-uri-05.nt").toList
    }
    assert(error.getMessage.contains("expected [u, or U], but found [/]"))
  }

  test("Bad IRI : relative IRI not allowed in subject (negative test) (nt-syntax-bad-uri-06)") {
    val error = intercept[ParseError] {
      val statements = StrictNtParser("nt-syntax-bad-uri-06.nt").toList
      println(s"statements = $statements")
      println("still failing")
    }
    assert(error.getMessage.contains("but found [ ]"))
  }

  test("Bad IRI : relative IRI not allowed in predicate (negative test) (nt-syntax-bad-uri-07)") {
    val error = intercept[ParseError] {
      val statements = StrictNtParser("nt-syntax-bad-uri-07.nt").toList
      println(s"statements = $statements")
      println("still failing")
    }
    assert(error.getMessage.contains("but found [ ]"))
  }

  test("Bad IRI : relative IRI not allowed in object (negative test) (nt-syntax-bad-uri-08)") {
    val error = intercept[ParseError] {
      val statements = StrictNtParser("nt-syntax-bad-uri-08.nt").toList
      println(s"statements = $statements")
      println("still failing")
    }
    assert(error.getMessage.contains("but found [ ]"))
  }

  test("Bad IRI : relative IRI not allowed in datatype (negative test) (nt-syntax-bad-uri-09)") {
    val error = intercept[ParseError] {
      val statements = StrictNtParser("nt-syntax-bad-uri-09.nt").toList
      println(s"statements = $statements")
      println("still failing")
    }
    assert(error.getMessage.contains("but found [ ]"))
  }

  test("@prefix not allowed in n-triples (negative test) (nt-syntax-bad-prefix-01)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-prefix-01.nt").toList
    }
    assert(error.getMessage.contains("expected [<, _, or #], but found [@]"))
  }

  test("@base not allowed in N-Triples (negative test) (nt-syntax-bad-base-01)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-base-01.nt").toList
    }
    assert(error.getMessage.contains("expected [<, _, or #], but found [@]"))
  }

  test("N-Triples does not have objectList (negative test) (nt-syntax-bad-struct-01)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-struct-01.nt").toList
    }
    assert(error.getMessage.contains("expected [.], but found [,]"))
  }

  test("N-Triples does not have predicateObjectList (negative test) (nt-syntax-bad-struct-02)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-struct-02.nt").toList
    }
    assert(error.getMessage.contains("expected [.], but found [;]"))
  }

  test("langString with bad lang (negative test) (nt-syntax-bad-lang-01)") {
    val error = intercept[ParseError] {
      val statements = StrictNtParser("nt-syntax-bad-lang-01.nt").toList
      println(s"statements = $statements")
      println("still failing")
    }
    assert(error.getMessage.contains("but found [ ]"))
  }

  test("Bad string escape (negative test) (nt-syntax-bad-esc-01)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-esc-01.nt").toList
    }
    assert(error.getMessage.contains("expected [\\, \", ', b, t, n, f, r, u, or U], but found [z]"))
  }

  test("Bad string escape (negative test) (nt-syntax-bad-esc-02)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-esc-02.nt").toList
    }
    assert(error.getMessage.contains("expected [hex character], but found [W]"))
  }

  test("Bad string escape (negative test) (nt-syntax-bad-esc-03)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-esc-03.nt").toList
    }
    assert(error.getMessage.contains("expected [hex character], but found [W]"))
  }

  test("mismatching string literal open/close (negative test) (nt-syntax-bad-string-01)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-string-01.nt").toList
    }
    assert(error.getMessage.contains("expected [\", or \\], but found [EOI]"))
  }

  test("mismatching string literal open/close (negative test) (nt-syntax-bad-string-02)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-string-02.nt").toList
    }
    assert(error.getMessage.contains("expected [<, _, or \"], but found [1]"))
  }

  test("single quotes (negative test) (nt-syntax-bad-string-03)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-string-03.nt").toList
    }
    assert(error.getMessage.contains("expected [<, _, or \"], but found [1]"))
  }

  test("long single string literal (negative test) (nt-syntax-bad-string-04)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-string-04.nt").toList
    }
    assert(error.getMessage.contains("expected [<, _, or \"], but found [']"))
  }

  test("long double string literal (negative test) (nt-syntax-bad-string-05)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-string-05.nt").toList
    }
    assert(error.getMessage.contains("expected [.], but found [\"]"))
  }

  test("string literal with no end (negative test) (nt-syntax-bad-string-06)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-string-06.nt").toList
    }
    assert(error.getMessage.contains("expected [\", or \\], but found [EOI]"))
  }

  test("string literal with no start (negative test) (nt-syntax-bad-string-07)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-string-07.nt").toList
    }
    assert(error.getMessage.contains("expected [<, _, or \"], but found [a]"))
  }

  test("no numbers in N-Triples (integer) (negative test) (nt-syntax-bad-num-01)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-num-01.nt").toList
    }
    assert(error.getMessage.contains("expected [<, _, or \"], but found [1]"))
  }

  test("no numbers in N-Triples (decimal) (negative test) (nt-syntax-bad-num-02)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-num-02.nt").toList
    }
    assert(error.getMessage.contains("expected [<, _, or \"], but found [1]"))
  }

  test("no numbers in N-Triples (float) (negative test) (nt-syntax-bad-num-03)") {
    val error = intercept[ParseError] {
      StrictNtParser("nt-syntax-bad-num-03.nt").toList
    }
    assert(error.getMessage.contains("expected [<, _, or \"], but found [1]"))
  }

  test("Submission test from Original RDF Test Cases (nt-syntax-subm-01)") {
    val statements = StrictNtParser("nt-syntax-subm-01.nt").toList
    val expecteds = List(
      Triple(Resource("http://example.org/resource1"), Resource("http://example.org/property"), Resource("http://example.org/resource2")),
      Triple(BNode("anon"), Resource("http://example.org/property"), Resource("http://example.org/resource2")),
      Triple(Resource("http://example.org/resource2"), Resource("http://example.org/property"), BNode("anon")),
      Triple(Resource("http://example.org/resource3"), Resource("http://example.org/property"), Resource("http://example.org/resource2")),
      Triple(Resource("http://example.org/resource4"), Resource("http://example.org/property"), Resource("http://example.org/resource2")),
      Triple(Resource("http://example.org/resource5"), Resource("http://example.org/property"), Resource("http://example.org/resource2")),
      Triple(Resource("http://example.org/resource6"), Resource("http://example.org/property"), Resource("http://example.org/resource2")),
      Triple(Resource("http://example.org/resource7"), Resource("http://example.org/property"), Literal.simple("simple literal")),
      Triple(Resource("http://example.org/resource8"), Resource("http://example.org/property"), Literal.simple("backslash:\\")),
      Triple(Resource("http://example.org/resource9"), Resource("http://example.org/property"), Literal.simple("dquote:\"")),
      Triple(Resource("http://example.org/resource10"), Resource("http://example.org/property"), Literal.simple("newline:\n")),
      Triple(Resource("http://example.org/resource11"), Resource("http://example.org/property"), Literal.simple("return\r")),
      Triple(Resource("http://example.org/resource12"), Resource("http://example.org/property"), Literal.simple("tab:\t")),
      Triple(Resource("http://example.org/resource13"), Resource("http://example.org/property"), Resource("http://example.org/resource2")),
      Triple(Resource("http://example.org/resource14"), Resource("http://example.org/property"), Literal.simple("x")),
      Triple(Resource("http://example.org/resource15"), Resource("http://example.org/property"), BNode("anon")),
      Triple(Resource("http://example.org/resource16"), Resource("http://example.org/property"), Literal.simple("é")),
      Triple(Resource("http://example.org/resource17"), Resource("http://example.org/property"), Literal.simple("€")),
      Triple(Resource("http://example.org/resource21"), Resource("http://example.org/property"), Literal.typed("", Resource("http://www.w3.org/2000/01/rdf-schema#XMLLiteral"))),
      Triple(Resource("http://example.org/resource22"), Resource("http://example.org/property"), Literal.typed(" ", Resource("http://www.w3.org/2000/01/rdf-schema#XMLLiteral"))),
      Triple(Resource("http://example.org/resource23"), Resource("http://example.org/property"), Literal.typed("x", Resource("http://www.w3.org/2000/01/rdf-schema#XMLLiteral"))),
      Triple(Resource("http://example.org/resource23"), Resource("http://example.org/property"), Literal.typed("\"", Resource("http://www.w3.org/2000/01/rdf-schema#XMLLiteral"))),
      Triple(Resource("http://example.org/resource24"), Resource("http://example.org/property"), Literal.typed("<a></a>", Resource("http://www.w3.org/2000/01/rdf-schema#XMLLiteral"))),
      Triple(Resource("http://example.org/resource25"), Resource("http://example.org/property"), Literal.typed("a <b></b>", Resource("http://www.w3.org/2000/01/rdf-schema#XMLLiteral"))),
      Triple(Resource("http://example.org/resource26"), Resource("http://example.org/property"), Literal.typed("a <b></b> c", Resource("http://www.w3.org/2000/01/rdf-schema#XMLLiteral"))),
      Triple(Resource("http://example.org/resource26"), Resource("http://example.org/property"), Literal.typed("a\n<b></b>\nc", Resource("http://www.w3.org/2000/01/rdf-schema#XMLLiteral"))),
      Triple(Resource("http://example.org/resource27"), Resource("http://example.org/property"), Literal.typed("chat", Resource("http://www.w3.org/2000/01/rdf-schema#XMLLiteral"))),
      Triple(Resource("http://example.org/resource30"), Resource("http://example.org/property"), Literal.tagged("chat", "fr")),
      Triple(Resource("http://example.org/resource31"), Resource("http://example.org/property"), Literal.tagged("chat", "en")),
      Triple(Resource("http://example.org/resource32"), Resource("http://example.org/property"), Literal.typed("abc", Resource("http://example.org/datatype1")))
    )
    statements.zip(expecteds) foreach {
      case (actual, expected) ⇒
        assert(actual == expected)
    }
  }

  test("Tests comments after a triple (comment_following_triple)") {
    val statements = StrictNtParser("comment_following_triple.nt").toList
    val expecteds = List(
      Triple(Resource("http://example/s"), Resource("http://example/p"), Resource("http://example/o")),
      Triple(Resource("http://example/s"), Resource("http://example/p"), BNode("o")),
      Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.simple("o")),
      Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.typed("o", Resource("http://example/dt"))),
      Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.tagged("o", "en"))
    )
    statements.zip(expecteds) foreach {
      case (actual, expected) ⇒
        assert(actual == expected)
    }
  }

  test("literal \"\"\"x\"\"\" (literal)") {
    val statements = StrictNtParser("literal.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"), Literal.simple("x"))))
  }

  test("literal_all_controls '\\x00\\x01\\x02\\x03\\x04...' (literal_all_controls)") {
    val statements = StrictNtParser("literal_all_controls.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"), Literal.simple(
      new String((0 to 31).filter(x ⇒ x != 10 && x != 13).map(_.toChar).toArray)))))
  }

  test("literal_all_punctuation '!\"#$%&()...' (literal_all_punctuation)") {
    val statements = StrictNtParser("literal_all_punctuation.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple(" !\"#$%&():;<=>?@[]^_`{|}~"))))
  }

  test("literal_ascii_boundaries '\\x00\\x26\\x28...' (literal_ascii_boundaries)") {
    val statements = StrictNtParser("literal_ascii_boundaries.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("\u0000\t\u000B\f\u000E&([]\u007F"))))
  }

  test("literal with 2 squotes \"\"\"a\"\"b\"\"\" (literal_with_2_dquotes)") {
    val statements = StrictNtParser("literal_with_2_dquotes.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("x\"\"y"))))
  }

  test("literal with 2 squotes \"x''y\" (literal_with_2_squotes)") {
    val statements = StrictNtParser("literal_with_2_squotes.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("x''y"))))
  }

  test("literal with BACKSPACE (literal_with_BACKSPACE)") {
    val statements = StrictNtParser("literal_with_BACKSPACE.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("\b"))))
  }

  test("literal with CARRIAGE RETURN (literal_with_CARRIAGE_RETURN)") {
    val statements = StrictNtParser("literal_with_CARRIAGE_RETURN.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("\r"))))
  }

  test("literal with CHARACTER TABULATION (literal_with_CHARACTER_TABULATION)") {
    val statements = StrictNtParser("literal_with_CHARACTER_TABULATION.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("\t"))))
  }

  test("literal with dquote \"x\"y\" (literal_with_dquote)") {
    val statements = StrictNtParser("literal_with_dquote.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("x\"y"))))
  }

  test("literal with FORM FEED (literal_with_FORM_FEED)") {
    val statements = StrictNtParser("literal_with_FORM_FEED.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("\f"))))
  }

  test("literal with LINE FEED (literal_with_LINE_FEED)") {
    val statements = StrictNtParser("literal_with_LINE_FEED.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("\n"))))
  }

  test("literal with numeric escape4 \\u (literal_with_numeric_escape4)") {
    val statements = StrictNtParser("literal_with_numeric_escape4.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("o"))))
  }

  test("literal with numeric escape8 \\U (literal_with_numeric_escape8)") {
    val statements = StrictNtParser("literal_with_numeric_escape8.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("o"))))
  }

  test("literal with REVERSE SOLIDUS (literal_with_REVERSE_SOLIDUS)") {
    val statements = StrictNtParser("literal_with_REVERSE_SOLIDUS.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("\\"))))
  }

  test("REVERSE SOLIDUS at end of literal (literal_with_REVERSE_SOLIDUS2)") {
    val statements = StrictNtParser("literal_with_REVERSE_SOLIDUS2.nt").toList
    assert(statements == List(Triple(Resource("http://example.org/ns#s"), Resource("http://example.org/ns#p1"),
      Literal.simple("test-\\"))))
  }

  test("literal with squote \"x'y\" (literal_with_squote)") {
    val statements = StrictNtParser("literal_with_squote.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("x'y"))))
  }

  test("literal_with_UTF8_boundaries '\\x80\\x7ff\\x800\\xfff...' (literal_with_UTF8_boundaries)") {
    val statements = StrictNtParser("literal_with_UTF8_boundaries.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.simple("\u0080\u07FFࠀ\u0FFFက쿿퀀\uD7FF\uE000�\uD800\uDC00\uD8BF\uDFFD\uD8C0\uDC00\uDBBF\uDFFD\uDBC0\uDC00\uDBFF\uDFFD"))))
  }

  test("langtagged string \"x\"@en (langtagged_string)") {
    val statements = StrictNtParser("langtagged_string.nt").toList
    assert(statements == List(Triple(Resource("http://a.example/s"), Resource("http://a.example/p"),
      Literal.tagged("chat", "en"))))
  }

  test("lantag with subtag \"x\"@en-us (lantag_with_subtag)") {
    val statements = StrictNtParser("lantag_with_subtag.nt").toList
    assert(statements == List(Triple(Resource("http://example.org/ex#a"), Resource("http://example.org/ex#b"),
      Literal.tagged("Cheers", "en-UK"))))
  }

  test("tests absense of whitespace between subject, predicate, object and end-of-statement (minimal_whitespace)") {
    val statements = StrictNtParser("minimal_whitespace.nt").toList
    val expecteds = List(
      Triple(Resource("http://example/s"), Resource("http://example/p"), Resource("http://example/o")),
      Triple(Resource("http://example/s"), Resource("http://example/p"), Literal.simple("Alice")),
      Triple(Resource("http://example/s"), Resource("http://example/p"), BNode("o")),
      Triple(BNode("s"), Resource("http://example/p"), Resource("http://example/o")),
      Triple(BNode("s"), Resource("http://example/p"), Literal.simple("Alice")),
      Triple(BNode("s"), Resource("http://example/p"), BNode("bnode1"))
    )
    statements.zip(expecteds) foreach {
      case (actual, expected) ⇒
        assert(actual == expected)
    }
  }
}
