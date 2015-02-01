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

package de.knutwalker.ntparser.examples;

import de.knutwalker.ntparser.NtParser;
import de.knutwalker.ntparser.ParseError;
import de.knutwalker.ntparser.jena.JenaModelFactory;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


public class DirectUsageJava {

  public static void main(String[] args) {

    NtParser<Resource, Property, RDFNode, Statement> parser = NtParser.strict(JenaModelFactory.INSTANCE());
    Statement statement = parser.parseOrNull("<abc:def> <ghi:jkl> <mno:pqr> .");
    System.out.println(statement);

    try {
      System.out.println(parser.parse("<abc:def> <ghi:jkl> <mno:pqr>", 42));
    } catch (ParseError parseError) {
      parseError.printStackTrace();
    }
  }
}
