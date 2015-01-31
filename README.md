# NtParser

A simple and performant [N-Triples][N-TRIPLES] parser for the JVM.


## Installing

NtParser is published to [Maven Central][mvn]:


```xml
<!-- for Scala 2.10 -->
<dependency>
    <groupId>de.knutwalker</groupId>
    <artifactId>ntparser_2.10</artifactId>
    <version>0.2.0</version>
</dependency>
<!-- for Scala 2.11 -->
<dependency>
    <groupId>de.knutwalker</groupId>
    <artifactId>ntparser_2.11</artifactId>
    <version>0.2.0</version>
</dependency>
```


```scala
libraryDependencies += "de.knutwalker" %% "ntparser" % "0.2.0"
```


## Using


```scala
import de.knutwalker.ntparser.StrictNtParser

StrictNtParser("my-graph.nt") foreach { stmt ⇒
  println(s"Statement: $stmt")
}
```


```java
import de.knutwalker.ntparser.Statement;
import de.knutwalker.ntparser.StrictNtParser;
import java.util.Iterator;

Iterator<Statement> statementIterator = StrictNtParser.parse("my-graph.nt");
while (statementIterator.hasNext()) {
  Statement stmt = statementIterator.next();
  System.out.println("Statement: " + stmt);
}
```


### in-depth usage

There are two static parsers, `de.knutwalker.ntparser.StrictNtParser` and `de.knutwalker.ntparser.NonStrictNtParser`.

Both take either a file name, an `InputStream`, an `Iterable` of String,
or an `Iterator` of String and always return an `Iterator` of `Statement`.
The `apply` methods are Scala API, the `parse` methods are Java API
(and are thus consuming and producing Java `Iterator`s).
The file name may be a path to any file or the file name of a resource file.
The parser can read plain nt files, as well as gzipped and bzipped files.
The `Iterable` and `Iterator` should represent single lines, that is, at-most one N-Triple per element.

The `StrictNtParser` will halt with an Exception at the first parse error.
The `NonStrictNtParser` will just log exceptions and continue parsing.

As an alternative, you can instantiate an `de.knutwalker.ntparser.NtParser` directly.
The `NtParser` has several `parse*` methods, that each take a String and assume,
that this is _one_ line of some bigger document and that is correctly delimited.
One invocation of any `parse*` method will parse at-most _one_ statement.
There are two flavours, `NtParser.strict` and `NtParser.lenient`. The lenient one
supports grammar features, that are not officially specified, such as long quotes
from Turtle.


## Specification conformity

NtParser passes [all tests provided by the W3C][rdf-test-cases].


## Roadmap

- parse into different data types, e.g. Jena Model


[N-TRIPLES]: http://www.w3.org/TR/2014/REC-n-triples-20140225/ "RDF 1.1 N-Triples"
[mvn]: http://search.maven.org/#search|ga|1|g%3A%22de.knutwalker%22%20AND%20%28a%3A%22ntparser_2.11%22%20OR%20a%3A%22ntparser_2.10%22%29
[rdf-test-cases]: http://www.w3.org/TR/2014/NOTE-rdf11-testcases-20140225/
