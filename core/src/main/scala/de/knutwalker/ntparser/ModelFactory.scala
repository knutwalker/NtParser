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

package de.knutwalker.ntparser

import annotation.implicitNotFound

/**
 * A typeclass (or facade, if you will), that creates different parts
 * of a statement graph. The parser uses this to construct its results.
 *
 * @tparam S The type of the Subject. Must b a subtype of the object node and
 *           is probably a supertype of a blank node and a uri node.
 * @tparam P The type of the predicate. Must b a subtype of the object node.
 * @tparam O The type of the object node.
 * @tparam T The type of a triple or statement node.
 */
@implicitNotFound("Cannot find a ModelFactory to use for parsing, did you provide one (implicitly)?")
trait ModelFactory[S <: O, P <: S, O <: AnyRef, T <: AnyRef] {
  /**
   * Get called before many lines are parsed, but not when using the
   * raw parser.
   */
  def reset(): Unit

  /**
   * Gets called when a iri-ref node shall be created
   * @param uri the URI for this iri-ref
   * @return a new instance of `S`, must never be null.
   */
  def iriRef(uri: String): S

  /**
   * Gets called when a blank node shall be created
   * @param id the ID for this blank node
   * @return a new instance of `S`, must never be null.
   */
  def blankNode(id: String): S

  /**
   * Gets called when a predicate shall be created
   * @param uri the URI for this predicate
   * @return a new instance of `P`, must never be null.
   */
  def predicate(uri: String): P

  /**
   * Gets called when a new simple literal shall be created
   * @param lexical the value of the literal
   * @return a new instance of `O`, must never be null.
   */
  def literal(lexical: String): O

  /**
   * Gets called when a new tagged literal shall be created
   * @param lexical the value of the literal
   * @param lang the language of the literal
   * @return a new instance of `O`, must never be null.
   */
  def taggedLiteral(lexical: String, lang: String): O

  /**
   * Gets called when a new typed literal shall be created
   * @param lexical the value of the literal
   * @param dt the URI of the datatype of the literal
   * @return a new instance of `O`, must never be null.
   */
  def typedLiteral(lexical: String, dt: String): O

  /**
   * Gets called when a new statement shall be created
   * @param s the instance for the subject of this statement
   * @param p the instance for the predicate of this statement
   * @param o the instance for the object of this statement
   * @return a new instance of `T`, must never be null.
   */
  def statement(s: S, p: P, o: O): T
}
