/*
 * Copyright 2014 – 2015 Paul Horn
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

package de.knutwalker.ntparser.jena

import de.knutwalker.ntparser.ModelFactory

import com.hp.hpl.jena.rdf.model.{AnonId, Statement, RDFNode, Property, Resource, Model, ModelFactory ⇒ JModelFactory}

object JenaModelFactory extends JenaModelFactory(JModelFactory.createDefaultModel()) {
  /** java api */
  final val INSTANCE: JenaModelFactory = this
}
class JenaModelFactory(val model: Model) extends ModelFactory[Resource, Property, RDFNode, Statement] {

  def reset(): Unit =
    model.removeAll()

  def iriRef(uri: String): Resource =
    model.createResource(uri)

  def blankNode(id: String): Resource =
    model.createResource(AnonId.create(id))

  def predicate(uri: String): Property =
    model.createProperty(uri)

  def literal(lexical: String): RDFNode =
    model.createLiteral(lexical)

  def taggedLiteral(lexical: String, lang: String): RDFNode =
    model.createLiteral(lexical, lang)

  def typedLiteral(lexical: String, dt: String): RDFNode =
    model.createTypedLiteral(lexical, dt)

  def statement(s: Resource, p: Property, o: RDFNode): Statement = {
    val stmt = model.createStatement(s, p, o)
    model.add(stmt)
    stmt
  }
}
