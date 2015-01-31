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

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import de.knutwalker.ntparser.CharacterChecks.{JBitSetChecks, BitSetChecks, ArrayChecks, MaskChecks, BoolChecks}

import java.util.concurrent.TimeUnit
import java.util.{BitSet ⇒ JBitSet}
import scala.annotation.tailrec
import scala.collection.immutable.BitSet

object CharacterChecks {

  private final class Mask(val bits: (Long, Long)) extends AnyVal

//  private def mask(chars: String): Mask = {
//    val n = chars.length
//    @tailrec
//    def loop(i: Int, l: Long, h: Long): (Long, Long) =
//      if (i == n) (l, h)
//      else {
//        val c = chars.charAt(i)
//        if (c < 64)       loop(i + 1, l | (1L << c), h)
//        else if (c < 128) loop(i + 1, l,             h | (1L << (c - 64)))
//        else              loop(i + 1, l,             h)
//      }
//    new Mask(loop(0, 0, 0))
//  }

//  private def mask(first: Char, last: Char): Mask = {
//    val fl = first min 63 max 0
//    val ll = last min 63 max 0
//    val fh = (first min 127 max 0) - 64
//    val lh = (last min 127 max 0) - 64
//    @tailrec
//    def loop(i: Int, h: Long, m: Long): Long =
//      if (i >= h) m
//      else loop(i + 1, h, m | (1L << i))
//    new Mask(loop(fl, ll, 0), loop(fh, lh, 0))
//  }

  private def mask(fn: Char ⇒ Boolean): Mask = {
    @tailrec
    def loop(i: Int, l: Long, h: Long): (Long, Long) =
      if (i >= 128) (l, h)
      else if (i < 64  && fn(i.toChar)) loop(i + 1, l | (1L << i), h)
      else if (i < 128 && fn(i.toChar)) loop(i + 1, l,             h | (1L << (i - 64)))
      else                              loop(i + 1, l,             h)
    new Mask(loop(0, 0, 0))
  }

  private def mask2(fn: Char ⇒ Boolean): Array[Boolean] = {
    val m = new Array[Boolean](128)
    @tailrec
    def loop(i: Int): Array[Boolean] =
      if (i >= 128) m
      else {
        m(i) = fn(i.toChar)
        loop(i + 1)
      }
    loop(0)
  }

  private def mask3(fn: Char ⇒ Boolean): BitSet = {
    (0 until 128).foldLeft(BitSet.empty) { (set, c) ⇒
      if (fn(c.toChar)) set + c else set
    }
  }

  private def mask4(fn: Char ⇒ Boolean): JBitSet = {
    (0 until 128).foldLeft(new JBitSet) { (set, c) ⇒
      if (fn(c.toChar)) {
        set.set(c)
        set
      } else set
    }
  }

  private def matches(low: Long, high: Long)(c: Char): Boolean =
    if (c == 0) false
    else if (c < 64) ((1L << c) & low) != 0
    else if (c < 128) ((1L << (c - 64)) & high) != 0
    else false

  @State(Scope.Benchmark)
  class BoolChecks {
    final val fn: (Char) ⇒ Boolean = (c) ⇒ c > 0x20 && c != '>' && c != '"' && c != '{' && c != '}' && c != '<' && c != '\\' && c != '%'
  }

  @State(Scope.Benchmark)
  class MaskChecks {
    final val fn: (Char) ⇒ Boolean = {
      val m = mask((new BoolChecks).fn.andThen(!_))
      matches(m.bits._1, m.bits._2)
    }
  }

  @State(Scope.Benchmark)
  class BitSetChecks {
    final val fn: (Char) ⇒ Boolean = {
      val m = mask3((new BoolChecks).fn.andThen(!_))
      c ⇒ m.contains(c)
    }
  }

  @State(Scope.Benchmark)
  class JBitSetChecks {
    final val fn: (Char) ⇒ Boolean = {
      val m = mask4((new BoolChecks).fn.andThen(!_))
      c ⇒ m.get(c)
    }
  }

  @State(Scope.Benchmark)
  class ArrayChecks {
    final val fn: (Char) ⇒ Boolean = {
      val m = mask2((new BoolChecks).fn.andThen(!_))
      c ⇒ m(c)
    }
  }
}

@State(Scope.Benchmark)
@Threads(value = 1)
@Fork(value = 1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class CharacterChecks {

//  @Param(Array("a", ">", "\\", "%"))
  @Param(Array("a", ">"))
  var ch: Char = _

  @Benchmark
  def testBoolFn(bh: Blackhole, fn: BoolChecks): Boolean = {
    fn.fn(ch)
  }

  @Benchmark
  def testMaskFn(bh: Blackhole, fn: MaskChecks): Boolean = {
    fn.fn(ch)
  }

  @Benchmark
  def testArrayFn(bh: Blackhole, fn: ArrayChecks): Boolean = {
    fn.fn(ch)
  }

  @Benchmark
  def testBitSetFn(bh: Blackhole, fn: BitSetChecks): Boolean = {
    fn.fn(ch)
  }

  @Benchmark
  def testJBitSetFn(bh: Blackhole, fn: JBitSetChecks): Boolean = {
    fn.fn(ch)
  }
}
