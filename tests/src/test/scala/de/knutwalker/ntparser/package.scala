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

package de.knutwalker

import com.hp.hpl.jena.rdf.model.{ Resource ⇒ JenaResource, ResourceFactory, RDFNode, Property, Model }
import com.hp.hpl.jena.vocabulary.RDF

package object ntparser {

  object implicits {

    implicit final class ExtendedModel(val m: Model) extends AnyVal {
      def s(p: Property, o: RDFNode): JenaResource =
        m.listSubjectsWithProperty(p, o).nextResource()

      def o(s: JenaResource, p: Property): RDFNode =
        m.listObjectsOfProperty(s, p).nextNode()

      def oo(s: JenaResource, p: Property): Option[RDFNode] = {
        val nodes = m.listObjectsOfProperty(s, p)
        if (nodes.hasNext)
          Some(nodes.nextNode())
        else
          None
      }

      def list(s: JenaResource): Stream[JenaResource] =
        if (s == RDF.nil)
          Stream()
        else
          o(s, RDF.first).asResource() #:: list(o(s, RDF.rest).asResource())
    }

    implicit final class NSString(val s: String) extends AnyVal {
      def :=(local: String): Property =
        ResourceFactory.createProperty(s, local)

      def :<(local: String): JenaResource =
        ResourceFactory.createResource(s + local)
    }
  }
}
