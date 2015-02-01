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

package de.knutwalker.ntparser.model

import de.knutwalker.ntparser.StatementFactory

object NtModelFactory extends StatementFactory[Node, Resource, Node, Statement] {
  def reset(): Unit = ()
  def iriRef(uri: String): Node = Resource(uri)
  def blankNode(id: String): Node = BNode(id)
  def predicate(uri: String): Resource = Resource(uri)
  def literal(lexical: String): Node = Literal.simple(lexical)
  def taggedLiteral(lexical: String, lang: String): Node =Literal.tagged(lexical, lang)
  def typedLiteral(lexical: String, dt: String): Node = Literal.typed(lexical, Resource(dt))
  def statement(s: Node, p: Resource, o: Node): Statement = Triple(s, p, o)

  /** java api */
  final val INSTANCE: StatementFactory[Node, Resource, Node, Statement] = this
}
