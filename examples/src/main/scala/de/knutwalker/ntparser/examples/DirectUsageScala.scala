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

import de.knutwalker.ntparser.{ParseError, NtParser}
import de.knutwalker.ntparser.jena.jenaModel

object DirectUsageScala extends App {

  val parser = NtParser.strict
  val statement = parser.parseOrNull("<abc:def> <ghi:jkl> <mno:pqr> .")
  println(statement)

  try {
    println(parser.parse("<abc:def> <ghi:jkl> <mno:pqr>", 42))
  } catch {
    case e: ParseError ⇒ e.printStackTrace()
  }
}
