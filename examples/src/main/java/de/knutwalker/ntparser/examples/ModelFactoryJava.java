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

import de.knutwalker.ntparser.ModelFactory;
import de.knutwalker.ntparser.StrictNtParser;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

public class ModelFactoryJava {

  static class StringsModel implements ModelFactory<String, String, String, String[]> {
    @Override
    public String iriRef(String uri) {
      return String.format("<%s>", uri);
    }

    @Override
    public String blankNode(String id) {
      return String.format("_:%s", id);
    }

    @Override
    public String predicate(String uri) {
      return iriRef(uri);
    }

    @Override
    public String literal(String lexical) {
      return String.format("\"%s\"", lexical);
    }

    @Override
    public String taggedLiteral(String lexical, String lang) {
      return String.format("\"%s\"@%s", lexical, lang);
    }

    @Override
    public String typedLiteral(String lexical, String dt) {
      return String.format("\"%s\"^^<%s>", lexical, dt);
    }

    @Override
    public String[] statement(String s, String p, String o) {
      return new String[]{s, p, o};
    }

    @Override
    public void reset() {
    }

    static final StringsModel INSTANCE = new StringsModel();
  }

  public static void main(String[] args) {

    InputStream inputStream = ModelFactoryJava.class.getResourceAsStream("/dnb_dump_000001.nt");
    Iterator<String[]> statementIterator = StrictNtParser.parse(inputStream, StringsModel.INSTANCE);

    while (statementIterator.hasNext()) {
      String[] stmt = statementIterator.next();
      System.out.println(Arrays.toString(stmt));
    }
  }
}
