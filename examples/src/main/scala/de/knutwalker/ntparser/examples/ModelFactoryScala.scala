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

package de.knutwalker.ntparser.examples

import de.knutwalker.ntparser.{StrictNtParser, ModelFactory}

object ModelFactoryScala extends App {

  implicit object StringsModel extends ModelFactory[String, String, String, Array[String]] {
    def iriRef(uri: String) = s"<$uri>"
    def blankNode(id: String) = s"_:$id"
    def predicate(uri: String) = iriRef(uri)
    def literal(lexical: String) = s""""$lexical""""
    def taggedLiteral(lexical: String, lang: String) = s""""$lexical"@$lang"""
    def typedLiteral(lexical: String, dt: String) = s""""$lexical"^^<$dt>"""
    def statement(s: String, p: String, o: String) = Array(s, p, o)
    def reset(): Unit = {}
  }

  val inputStream = getClass.getResourceAsStream("/dnb_dump_000001.nt")
  StrictNtParser(inputStream) foreach { a ⇒
    println(a.mkString("[", ", ", "]"))
  }
}
