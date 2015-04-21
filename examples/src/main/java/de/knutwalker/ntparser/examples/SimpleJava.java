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

package de.knutwalker.ntparser.examples;

import de.knutwalker.ntparser.StrictNtParser;
import de.knutwalker.ntparser.model.NtModelFactory;
import de.knutwalker.ntparser.model.Statement;

import java.util.Iterator;


public class SimpleJava {

  public static void main(String[] args) {

    Iterator<Statement> statements = StrictNtParser.parse("dnb_dump_000001.nt", NtModelFactory.INSTANCE());

    while (statements.hasNext()) {
      Statement stmt = statements.next();
      System.out.println(stmt.n3());
    }
  }
}
