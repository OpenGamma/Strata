/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

import com.google.common.reflect.TypeToken;

/**
 * Defines a link to a target object using an identifier.
 * <p>
 * A link provides loose coupling between different parts of the object model.
 * For example, an equity trade can hold a link to the underlying equity.
 * The two objects can be updated independently of each other.
 * If the link resolver supports historic versions, then the correct version of the
 * target will be returned when resolved.
 * The target object should be immutable to ensure the safety of the link.
 * <p>
 * A link can be in one of two states, resolvable and resolved.
 * <p>
 * In the resolvable state, the link contains the identifier and type of the target object.
 * The target can only be obtained using a {@link LinkResolver}.
 * <p>
 * In the resolved state, the link directly embeds the target object.
 * <p>
 * Links are expected to be resolvable. It is reasonable to expect that when
 * {@link #resolve(LinkResolver)} is called, the target of the link is available.
 * For this reason, if the target is not found, a {@link LinkResolutionException} will be thrown.
 * <p>
 * The standard implementation of {@code Link} is {@link StandardLink}.
 *
 * @param <T> the type of the target
 */
public interface Link<T extends IdentifiableBean>
    extends StandardIdentifiable {

  /**
   * Checks if the link is resolved.
   * <p>
   * A resolved link contains the target directly.
   * A resolvable link only contains the identifier, acting as a pointer to the target.
   * 
   * @return true if the link is resolved, false if unresolved
   */
  public abstract boolean isResolved();

  /**
   * Gets the identifier of the target.
   * <p>
   * Returns the identifier, either directly or from the target.
   * 
   * @return the standard identifier of the target
   */
  @Override
  public abstract StandardId getStandardId();

  /**
   * Gets the target type.
   * <p>
   * Returns the target type, either directly or from the target.
   * 
   * @return the target type of the link
   */
  @SuppressWarnings("unchecked")
  public abstract Class<T> getTargetType();

  /**
   * Gets the target type token.
   * <p>
   * Returns the target type as a {@link TypeToken}, either directly or from the target.
   * A {@code TypeToken} is used to express generic parameterized types, such as {@code Trade<Swap>}:
   * <p>
   * <pre>
   *  new TypeToken&lt;Trade&lt;Swap&gt;&gt;() {};
   * </pre>
   * 
   * @return the target type of the link
   */
  @SuppressWarnings("unchecked")
  public default TypeToken<T> getTargetTypeToken() {
    return TypeToken.of(getTargetType());
  }

  /**
   * Resolves this link using the specified resolver.
   * <p>
   * In the resolvable state, the resolver is used to find the target, throwing
   * an exception of the target cannot be found.
   * In the resolved state, the directly embedded target is returned,
   * without using the resolver.
   * <p>
   * The returned target may contain other unresolved links.
   * See {@link LinkResolvable} for a mechanism to resolve all links in an object graph.
   *
   * @param resolver  the resolver to use for the resolution
   * @return the target
   * @throws LinkResolutionException if the target cannot be resolved
   */
  @SuppressWarnings("unchecked")
  public abstract T resolve(LinkResolver resolver);

}
