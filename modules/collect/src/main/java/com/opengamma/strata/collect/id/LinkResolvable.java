/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

/**
 * Provides a mechanism to resolve links within an object graph.
 * <p>
 * A data structure may contain one of more {@linkplain Link links} either directly as
 * instance variables, or nested within instance variables.
 * All objects within the object graph that may contain links should implement this interface.
 * When the resolve method is called, the result is an object of the same, or compatible, type
 * that has all links resolved.
 * <p>
 * To resolve an arbitrary object, which may or may not implement {@code LinkResolvable},
 * see {@link LinkResolver#resolveLinksIn(Object)}.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 * 
 * @param <T>  the resolved type, which is the type implementing this interface
 */
public interface LinkResolvable<T> {

  /**
   * Resolves all links in the object graph.
   * <p>
   * This is called to resolve any links within the object graph.
   * The target object, implementing this interface, must not be altered.
   * <p>
   * Implementations must check if they contain any links and resolve them.
   * To resolve the links, the specified resolver is used, typically accessing an underlying data store.
   * If the link cannot be resolved then a {@code LinkResolutionException} will be thrown.
   * If the implementation does not need to resolve anything, {@code this} must be returned.
   * 
   * @param resolver  the resolver to use
   * @return the resolved instance, which must be compatible with the type implementing this interface,
   *   {@code this} if no resolution occurs
   * @throws LinkResolutionException if a link cannot be resolved
   */
  public abstract T resolveLinks(LinkResolver resolver);

}
