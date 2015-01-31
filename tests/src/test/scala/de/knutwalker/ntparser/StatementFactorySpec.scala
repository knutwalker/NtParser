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

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import com.hp.hpl.jena.rdf.model.{Resource ⇒ JResource, Statement ⇒ JStatement, AnonId, Model, ModelFactory, Property, RDFNode}
import org.scalatest.FunSuite

import scala.collection.JavaConverters._
import scala.Predef.println

class StatementFactorySpec extends FunSuite {

  object JenaFactory extends JenaFactory(ModelFactory.createDefaultModel())
  class JenaFactory(val model: Model) extends StatementFactory[JResource, Property, RDFNode, JStatement] {

    def reset(): Unit =
      model.removeAll()

    def iriRef(uri: String): JResource =
      model.createResource(uri)

    def blankNode(id: String): JResource =
      model.createResource(AnonId.create(id))

    def predicate(uri: String): Property =
      model.createProperty(uri)

    def literal(lexical: String): RDFNode =
      model.createLiteral(lexical)

    def taggedLiteral(lexical: String, lang: String): RDFNode =
      model.createLiteral(lexical, lang)

    def typedLiteral(lexical: String, dt: String): RDFNode =
      model.createTypedLiteral(lexical, dt)

    def statement(s: JResource, p: Property, o: RDFNode): JStatement = {
      val stmt = model.createStatement(s, p, o)
      model.add(stmt)
      stmt
    }
  }

  test("parse simple line") {
    val line = "_:abc <d:ef> \"ghi\" ."
    val statement = parse(line)
    assert(statement.getSubject.isAnon)
    assert(statement.getSubject.asResource.getId.getLabelString == "abc")
    assert(statement.getPredicate.getNameSpace == "d:")
    assert(statement.getPredicate.getLocalName == "ef")
    assert(statement.getObject.isLiteral)
    assert(statement.getObject.asLiteral.getLexicalForm == "ghi")
  }

  test("parse line with language hint") {
    val line = """<http://de.dbpedia.org/resource/FooBar> <http://www.w3.org/2000/01/rdf-schema#label> "FooBar"@de . """
    val statement = parse(line)
    assert(statement.getSubject.isURIResource)
    assert(statement.getSubject.asResource.getURI == "http://de.dbpedia.org/resource/FooBar")
    println(s"statement.getPredicate = ${statement.getPredicate}")
    println(s"statement.getPredicate = ${statement.getPredicate.getClass}")
    println(s"statement.getPredicate = ${statement.getPredicate.getNameSpace}")
    println(s"statement.getPredicate = ${statement.getPredicate.getLocalName}")
    assert(statement.getPredicate.getNameSpace == "http://www.w3.org/2000/01/rdf-schema#")
    assert(statement.getPredicate.getLocalName == "label")
    assert(statement.getObject.isLiteral)
    assert(statement.getObject.asLiteral.getLexicalForm == "FooBar")
    assert(statement.getObject.asLiteral.getLanguage == "de")
  }

  test("parse line with literal type") {
    val line = """<http://de.dbpedia.org/resource/FooBar> <http://www.w3.org/2000/01/rdf-schema#label> "12"^^<http://www.w3.org/2001/XMLSchema#int> . """
    val statement = parse(line)
    assert(statement.getSubject.isURIResource)
    assert(statement.getSubject.asResource.getURI == "http://de.dbpedia.org/resource/FooBar")
    assert(statement.getPredicate.getNameSpace == "http://www.w3.org/2000/01/rdf-schema#")
    assert(statement.getPredicate.getLocalName == "label")
    assert(statement.getObject.isLiteral)
    assert(statement.getObject.asLiteral.getInt == 12)
    assert(statement.getObject.asLiteral.getDatatype == XSDDatatype.XSDint)
  }

  test("parse line with bnode object") {
    val line = """<http://de.dbpedia.org/resource/FooBar> <http://www.w3.org/2000/01/rdf-schema#label> _:baz . """
    val statement = parse(line)
    assert(statement.getSubject.isURIResource)
    assert(statement.getSubject.asResource.getURI == "http://de.dbpedia.org/resource/FooBar")
    assert(statement.getPredicate.getNameSpace == "http://www.w3.org/2000/01/rdf-schema#")
    assert(statement.getPredicate.getLocalName == "label")
    assert(statement.getObject.isAnon)
    assert(statement.getObject.asResource.getId.getLabelString == "baz")
  }

  test("parse line with uri ref object") {
    val line = """<http://de.dbpedia.org/resource/FooBar> <http://www.w3.org/2000/01/rdf-schema#label> <http://de.dbpedia.org/resource/BarBaz> . """
    val statement = parse(line)
    assert(statement.getSubject.isURIResource)
    assert(statement.getSubject.asResource.getURI == "http://de.dbpedia.org/resource/FooBar")
    assert(statement.getPredicate.getNameSpace == "http://www.w3.org/2000/01/rdf-schema#")
    assert(statement.getPredicate.getLocalName == "label")
    assert(statement.getObject.isURIResource)
    assert(statement.getObject.asResource.getURI == "http://de.dbpedia.org/resource/BarBaz")
  }

  test("parse and traverse graph") {
    val line1 = """<http://de.dbpedia.org/resource/FooBar> <http://www.w3.org/2000/01/rdf-schema#qux> <http://de.dbpedia.org/resource/BarBaz> . """
    val line2 = """<http://de.dbpedia.org/resource/BarBaz> <http://www.w3.org/2000/01/rdf-schema#label> "42"^^<http://www.w3.org/2001/XMLSchema#int> . """
    val parsed@List(s1, s2) = StrictNtParser(List(line1, line2))(JenaFactory).toList
    assert(s1.getObject == s2.getSubject)
    assert(JenaFactory.model.listStatements(s1.getObject.asResource(), null, null).nextStatement().getInt == 42)
    assert(JenaFactory.model.listStatements.asScala.toList.reverse == parsed)
  }

  private def parse(line: String): JStatement = {
    val parser = NtParser.strict(JenaFactory)
    parser.parse(line)
  }
}
