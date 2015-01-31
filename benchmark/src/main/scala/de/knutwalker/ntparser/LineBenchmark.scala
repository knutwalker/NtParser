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

import com.hp.hpl.jena.rdf.model.ModelFactory
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import org.semanticweb.yars.nx.parser.NxParser

import de.knutwalker.ntparser.LineBenchmark.{NtState, NxState, JenaState}

import java.io.{Reader, StringReader}
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._

object LineBenchmark {

  @State(Scope.Benchmark)
  class JenaState {
    val parser = ModelFactory.createDefaultModel()
    private[this] val line = """<http://example.com/s> <http://example.com/p> <http://example.com/o> ."""
    def reader: Reader = new StringReader(line)
  }
  
  @State(Scope.Benchmark)
  class NxState {
    private[this] val line = """<http://example.com/s> <http://example.com/p> <http://example.com/o> ."""
    private[this] val iterator = Iterator.continually(line).asJava
    val parser = new NxParser(iterator)
  }

  @State(Scope.Benchmark)
  class NtState {
    val parser = new NtParser()
  }

}

@Threads(value = 1)
@Fork(value = 1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class LineBenchmark {

  @Benchmark
  def testJenaParser(bh: Blackhole, state: JenaState): Unit = {
    bh.consume(state.parser.read(state.reader, null, "N-TRIPLE"))
  }

  @Benchmark
  def testNxParser(bh: Blackhole, state: NxState): Unit = {
    bh.consume(state.parser.next())
  }

  @Benchmark
  def testNtParser(bh: Blackhole, state: NtState): Unit = {
    bh.consume(state.parser.parse("""<http://example.com/s> <http://example.com/p> <http://example.com/o> ."""))
  }

}
