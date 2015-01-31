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

import java.io.FileInputStream
import java.util.concurrent.TimeUnit


@State(Scope.Benchmark)
@Threads(value = 1)
@Fork(value = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class ParseBenchmark {

//  @Param(Array("category_labels_de.nt", "skos_categories_de.nt", "labels_de.nt", "instance_types_de.nt", "article_categories_de.nt"))
//  @Param(Array("category_labels_de.nt", "skos_categories_de.nt", "labels_de.nt"))
  @Param(Array("category_labels_de.nt"))
  var fileName: String = _

  private[this] def fileLocation =
    "/Users/knut/Development/Scala/dbpedia-neo4j/data-files/" + fileName

  @Benchmark
  def testJenaParser(bh: Blackhole): Unit = {
    val m = ModelFactory.createDefaultModel()
    bh.consume(m.read(new FileInputStream(fileLocation), null, "N-TRIPLE"))
  }

  @Benchmark
  def testNxParser(bh: Blackhole): Unit = {
    val parser = new NxParser(new FileInputStream(fileLocation))
    while (parser.hasNext) {
      bh.consume(parser.next())
    }
  }

  @Benchmark
  def testNtParser(bh: Blackhole): Unit = {
    bh.consume(StrictNtParser(fileLocation).foreach(_ ⇒ ()))
  }
}
