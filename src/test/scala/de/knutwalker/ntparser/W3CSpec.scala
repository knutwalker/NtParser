package de.knutwalker.ntparser

import org.scalatest.FunSuite

class W3CSpec extends FunSuite {

  spec("1. a sequence of RDF terms", "spec_example_01.nt") {
    List(
      Triple(
        Resource("http://one.example/subject1"),
        Resource("http://one.example/predicate1"),
        Resource("http://one.example/object1")
      ),
      Triple(
        BNode("subject1"),
        Resource("http://an.example/predicate1"),
        Literal("object1")
      ),
      Triple(
        BNode("subject2"),
        Resource("http://an.example/predicate2"),
        Literal("object2")
      )
    )
  }

  spec("2.1 Simple Terms", "spec_example_02.nt") {
    List(
      Triple(
        Resource("http://example.org/#spiderman"),
        Resource("http://www.perceive.net/schemas/relationship/enemyOf"),
        Resource("http://example.org/#green-goblin")
      )
    )
  }

  spec("2.3 RDF Literals", "spec_example_03.nt") {
    List(
      Triple(
        Resource("http://example.org/show/218"),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal("That Seventies Show", None, Some(Resource("http://www.w3.org/2001/XMLSchema#string")))
      ),
      Triple(
        Resource("http://example.org/show/218"),
        Resource("http://www.w3.org/2000/01/rdf-schema#label"),
        Literal("That Seventies Show")
      ),
      Triple(
        Resource("http://example.org/show/218"),
        Resource("http://example.org/show/localName"),
        Literal("That Seventies Show", Some("en"))
      ),
      Triple(
        Resource("http://example.org/show/218"),
        Resource("http://example.org/show/localName"),
        Literal("Cette Série des Années Septante", Some("fr-be"))
      ),
      Triple(
        Resource("http://example.org/#spiderman"),
        Resource("http://example.org/text"),
        Literal("This is a multi-line\nliteral with many quotes (\"\"\"\"\")\nand two apostrophes ('').")
      ),
      Triple(
        Resource("http://en.wikipedia.org/wiki/Helium"),
        Resource("http://example.org/elements/atomicNumber"),
        Literal("2", None, Some(Resource("http://www.w3.org/2001/XMLSchema#integer")))
      ),
      Triple(
        Resource("http://en.wikipedia.org/wiki/Helium"),
        Resource("http://example.org/elements/specificGravity"),
        Literal("1.663E-4", None, Some(Resource("http://www.w3.org/2001/XMLSchema#double")))
      )
    )
  }

  spec("2.4 RDF Blank Nodes", "spec_example_04.nt") {
    List(
      Triple(BNode("alice"), Resource("http://xmlns.com/foaf/0.1/knows"), BNode("bob")),
      Triple(BNode("bob"), Resource("http://xmlns.com/foaf/0.1/knows"), BNode("alice"))
    )
  }




  private def spec(description: String, fileName: String)(f: => List[Statement]): Unit = {
    test(description) {
      val actualStatements = StrictNtParser(fileName).toList
      val expectedStatements = f
      actualStatements.zip(expectedStatements) foreach {
        case (actual, expected) =>
          assert(actual == expected)
      }
    }
  }
}
