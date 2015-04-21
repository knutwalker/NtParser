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

package de.knutwalker.ntparser.model

sealed trait Node {
  def n3: String
}

case class Resource(uri: String) extends Node {
  private[this] final val dataValue = uri

  lazy final val n3 = s"<$dataValue>"

  override def toString: String = dataValue
}

case class Literal(lexical: String, lang: Option[String], dt: Resource) extends Node {
  private[this] final val dataValue = lexical

  private[this] lazy final val langValue = lang.fold("")("@" + _)
  private[this] lazy final val dtValue = "^^" + dt.n3
  private[this] lazy final val stringValue = s""""$dataValue"$langValue$dtValue"""

  lazy final val n3 = stringValue

  override def toString: String = dataValue
}
object Literal {
  private final val SimpleStringType = Resource("http://www.w3.org/2001/XMLSchema#string")
  private final val LangStringType = Resource("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")

  def simple(lexical: String): Literal =
    new Literal(lexical, None, SimpleStringType)

  def tagged(lexical: String, lang: String): Literal =
    new Literal(lexical, Some(lang), LangStringType)

  def typed(lexical: String, dt: Resource): Literal =
    new Literal(lexical, None, dt)
}

case class BNode(nodeId: String) extends Node {
  private[this] final val dataValue = nodeId

  lazy final val n3 = s"_:$dataValue"

  override def toString: String = dataValue
}

sealed abstract class Statement(val s: Node, val p: Resource, val o: Node) extends Node {
  lazy final val n3 = s"${s.n3} ${p.n3} ${o.n3} ."

  override def toString: String = s"$s $p $o"
}

object Statement {
  val Empty: Statement = Triple(BNode(""), Resource(""), BNode(""))
}

case class Triple(override val s: Node, override val p: Resource, override val o: Node) extends Statement(s, p, o)


