/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.id;

/**
 * A resolver that can provide the target when resolving links.
 * <p>
 * A {@link Link} provides loose coupling between different parts of the object model.
 * When the target of a link is needed, it is resolved by passing in a link resolver.
 * <p>
 * Link resolution will typically be implemented to access an underlying data store.
 * If the link specifies an identifier and/or that is not resolvable, an exception is thrown.
 */
public interface LinkResolver {

  /**
   * Resolves the supplied link, returning the realized target of the link.
   * <p>
   * Resolution will be triggered by a call to {@link Link#resolve(LinkResolver)}.
   * The implementation of this interface may perform any thread-safe action to obtain
   * the link target. Typically this will involve accessing an underlying data store.
   * If the link cannot be resolved then a {@code LinkResolutionException} will be thrown.
   *
   * @param <T>  the type of the target of the link
   * @param link  the link to be resolved
   * @return the resolved target of the link
   * @throws LinkResolutionException if the link cannot be resolved
   */
  public abstract <T extends IdentifiableBean> T resolve(ResolvableLink<T> link);

}
