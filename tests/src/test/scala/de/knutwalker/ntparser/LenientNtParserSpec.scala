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

import org.scalatest.FunSuite

class LenientNtParserSpec extends FunSuite {

  test("parse turtle single-line long quotes") {
    val line = "_:foo <b:ar> \"\"\"baz\"\"\" ."
    val statement = parse(line)
    assert(statement.s == BNode("foo"))
    assert(statement.p == Resource("b:ar"))
    assert(statement.o == Literal.simple("baz"))
  }

  test("parse turtle multiple-line long quotes") {
    val line = "_:foo <b:ar> \"\"\"baz\nqux\"\"\" ."
    val statement = parse(line)
    assert(statement.s == BNode("foo"))
    assert(statement.p == Resource("b:ar"))
    assert(statement.o == Literal.simple("baz\nqux"))
  }

  private def parse(line: String) = {
    val parser = NtParser.lenient
    parser.parse(line)
  }
}
