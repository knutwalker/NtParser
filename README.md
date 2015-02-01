# NtParser

A simple and well-performing [N-Triples](http://www.w3.org/TR/2014/REC-n-triples-20140225/ "RDF 1.1 N-Triples") parser for the JVM.


## Installing

NtParser is published to [Maven Central](http://search.maven.org/#search|ga|1|g%3A%22de.knutwalker%22%20AND%20%28a%3A%22ntparser_2.11%22%20OR%20a%3A%22ntparser_2.10%22%29):

- group = de.knutwalker
- artifact = ntparser_2.10 or ntparser_2.11
- version: 0.3.0


```xml
<!-- for Scala 2.10 -->
<dependency>
    <groupId>de.knutwalker</groupId>
    <artifactId>ntparser_2.10</artifactId>
    <version>0.3.0</version>
</dependency>
<!-- for Scala 2.11 -->
<dependency>
    <groupId>de.knutwalker</groupId>
    <artifactId>ntparser_2.11</artifactId>
    <version>0.3.0</version>
</dependency>
```


```scala
libraryDependencies += "de.knutwalker" %% "ntparser" % "0.3.0"
```


You need at least one modelfactory instance as well. Available are:
- `de.knutwalker:ntparser-model_2.1x:0.3.0`
- `de.knutwalker:ntparser-jena_2.1x:0.3.0`


## Using


```scala
import de.knutwalker.ntparser.StrictNtParser
import de.knutwalker.ntparser.model.ntModel

StrictNtParser("/path/to/my/graph.nt") foreach { stmt ⇒
  println(stmt.n3)
}
```


```java
import de.knutwalker.ntparser.model.NtModelFactory;
import de.knutwalker.ntparser.model.Statement;
import de.knutwalker.ntparser.StrictNtParser;
import java.util.Iterator;

Iterator<Statement> statementIterator = StrictNtParser.parse("/path/to/my/graph.nt", NtModelFactory.INSTANCE());
while (statementIterator.hasNext()) {
  Statement stmt = statementIterator.next();
  System.out.println(stmt.n3());
}
StrictNtParser.close();
```


### in-depth usage

There are two parsers, `de.knutwalker.ntparser.StrictNtParser`
and `de.knutwalker.ntparser.NonStrictNtParser`.

Both provide a number of possibilities to parse a graph and will always
return an `Iterator` of a statement. The `apply` methods are Scala API,
the `parse` methods are Java API (and are thus consuming and producing
Java `Iterator`s).

The `StrictNtParser` will halt with an Exception at the first parse error.
The `NonStrictNtParser` will just log exceptions and continue parsing.

In addition to that, you need to provide an instance of a typeclass or factory,
that will be used to create the resulting node graph.
There are two implementations available in the artifacts `ntparser-model` and
`ntparser-jena`. The first will use a very simple Scala ADT, the second
produces a Jena model. You can also implement the interface
`de.knutwalker.ntparser.ModelFactory` to include different backends.

If you provide a String, it is treated as the filename. NtParser transparently
handles gzipped and bzipped files as well, if you use this overloading.
Other options include providing a `scala.io.Source`, for the Scala API
(`apply`), `File`, `Path` for the Java API (`parse`), as well as `InputStream`
and the respective `Iterable` and `Iterator` for both Scala and Java.
The `Iterable` and `Iterator` should represent single lines, that is,
one N-Triple per element.

As an alternative, you can instantiate an `de.knutwalker.ntparser.NtParser` directly.
The `NtParser` has several `parse*` methods, that each take a String and assume,
that this is _one_ line of some bigger document and that is correctly delimited.
One invocation of any `parse*` method will parse at-most _one_ statement.
There are two flavours, `NtParser.strict` and `NtParser.lenient`. The lenient one
supports grammar features, that are not officially specified, such as long quotes
from Turtle.


## Specification conformity

NtParser passes [all tests provided by the W3C](http://www.w3.org/TR/2014/NOTE-rdf11-testcases-20140225/)
