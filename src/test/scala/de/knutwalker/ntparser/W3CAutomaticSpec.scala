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

import com.hp.hpl.jena.rdf.model.{ Resource ⇒ JenaResource, _ }
import com.hp.hpl.jena.util.FileManager
import com.hp.hpl.jena.vocabulary.{ RDF, RDFS }
import org.scalatest.{ Ignore, FunSuite }

class W3CAutomaticSpec extends FunSuite {
  import de.knutwalker.ntparser.implicits._

  val model = {
    val file = getClass.getResource("/manifest.ttl").getFile
    val in = FileManager.get().open(file)
    val m = ModelFactory.createDefaultModel()
    m.read(in, null, "TURTLE")
  }

  val mf = model.getNsPrefixURI("mf")
  val rdft = model.getNsPrefixURI("rdft")

  val manifest = model.s(RDF.`type`, mf :< "Manifest")
  val entries = model.list(model.o(manifest, mf := "entries").asResource())

  val positiveTest = rdft :< "TestNTriplesPositiveSyntax"
  val negativeTest = rdft :< "TestNTriplesNegativeSyntax"
  val proposed = rdft :< "Proposed"

  def makeTestCase(s: JenaResource): Option[TestCase] = {
    val testTypeResource = model.o(s, RDF.`type`).asResource()
    val testType = if (testTypeResource == positiveTest)
      Some(Positive)
    else if (testTypeResource == negativeTest)
      Some(Negative)
    else
      None
    testType map { tt ⇒
      val name = model.o(s, mf := "name").asLiteral().getLexicalForm
      val comment = model.o(s, RDFS.comment).asLiteral().getLexicalForm
      val action = model.o(s, mf := "action").asResource().getLocalName
      val approval = model.oo(s, rdft := "approval").
        filter(_.isResource).
        filter(_.asResource() == proposed).
        fold[Approval](Approved)(_ ⇒ Proposed)
      TestCase(name, comment, action, tt, approval)
    }
  }

  val testCases = entries.flatMap(s ⇒ makeTestCase(s).toList.toStream)

  def generateTests(): Unit = {
    testCases foreach { testCase ⇒
      testCase.testType match {
        case Positive ⇒
          test(s"[AUTO] ${testCase.comment} (${testCase.name})") {
            StrictNtParser(testCase.file).toList
          }
        case Negative ⇒
          test(s"[AUTO] ${testCase.comment} (${testCase.name})") {
            intercept[ParseError] {
              StrictNtParser(testCase.file).toList
            }
          }
      }
    }
  }

  generateTests()
}
