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

import org.scalatest.{ Matchers, FlatSpec }
import org.scalatest.prop.PropertyChecks
import org.scalacheck.{ Gen, Shrink }
import scala.collection.mutable.ListBuffer

// format: +preserveSpaceBeforeArguments
// format: -rewriteArrowSymbols
class NtParserRegressionSpec extends FlatSpec with Matchers with PropertyChecks {

  "The NtParser" should "parse valid lines" in {

    forAllNoShrink(NtGen.ValidTriple -> "triple", MinSuccessful(200), MaxSize(500)) {
      case (line, expected) =>
        val parser = new NtParser
        parser.parse(line) shouldBe expected
    }
  }

  it should "fail to parse invalid predicates" in {

    forAllNoShrink(NtGen.InvalidPredicate -> "triple") {
      case (line, (atPos, wrongChar)) =>
        val parser = new NtParser
        val expected = s"parse error at char $atPos, expected [<], but found [$wrongChar]"

        val thrown = the[ParseError] thrownBy parser.parse(line)

        thrown.getMessage.split('\n').head shouldBe expected
    }
  }

  it should "fail to parse invalid subjects" in {

    forAllNoShrink(NtGen.InvalidSubject -> "triple") {
      case (line, _) =>
        val parser = new NtParser
        val expected = s"parse error at char 1, expected [<, _, or #], but found [${'"'}]"

        val thrown = the[ParseError] thrownBy parser.parse(line)
        thrown.getMessage.split('\n').head shouldBe expected
    }
  }

  it should "fail to parse lines that are missing the terminating full stop" in {

    forAllNoShrink(NtGen.MissingDot -> "triple") {
      case (line, atPos) =>
        val parser = new NtParser
        val expected = s"parse error at char $atPos, expected [.], but found [EOI]"

        val thrown = the[ParseError] thrownBy parser.parse(line)
        thrown.getMessage.split('\n').head shouldBe expected
    }

  }

  it should "be reusable" in {
    val expecteds = new ListBuffer[Statement]
    val singleInstance = new ListBuffer[Statement]
    val newInstance = new ListBuffer[Statement]

    val singleParser = new NtParser

    forAllNoShrink(NtGen.ValidTriple -> "triple", MinSuccessful(200), MaxSize(500)) {
      case (line, expected) =>
        val parser = new NtParser
        newInstance += parser.parse(line)
        singleInstance += singleParser.parse(line)
        expecteds += expected
    }

    (expecteds.result(), singleInstance.result(), newInstance.result()).zipped.foreach {
      (expected, singleResult, newResult) =>
        expected shouldBe singleResult
        expected shouldBe newResult
        singleResult shouldBe newResult
    }
  }

  it should "ignore whitespace" in {

    forAllNoShrink(NtGen.WithWhiteSpace -> "triple", MinSuccessful(200), MaxSize(500)) {
      case (line, expected) =>
        val parser = new NtParser
        parser.parse(line) shouldBe expected
    }
  }

  it should "ignore comment lines" in {

    forAllNoShrink(NtGen.CommentLine -> "triple", MinSuccessful(200), MaxSize(500)) { line =>
      val parser = new NtParser
      parser.parse(line) shouldBe null
    }
  }

  def forAllNoShrink[A](genAndNameA: (Gen[A], String), configParams: PropertyCheckConfigParam*)(fun: (A) => Unit)(implicit config: PropertyCheckConfig): Unit = {
    val noShrink: Shrink[A] = Shrink.shrinkAny
    forAll(genAndNameA, configParams: _*)(fun)(config, noShrink)
  }
}
