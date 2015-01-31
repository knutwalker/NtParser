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

import java.util.concurrent.TimeUnit


@State(Scope.Benchmark)
@Threads(value = 1)
@Fork(value = 1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class NtParserBenchmark {

  private[this] val parserB = new NtParserB

  @Benchmark
  def baseline(bh: Blackhole): Unit = {}

  @Benchmark
  def reset(bh: Blackhole): Unit = {
    parserB.reset("""<http://example.com/s> <http://example.com/p> <http://example.com/o> .""")
  }

  @Benchmark
  def fastReset(bh: Blackhole): Unit = {
    parserB.fastReset("""<http://example.com/s> <http://example.com/p> <http://example.com/o> .""")
  }
}
