/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.id;

/**
 * A link to a target object that has an identifier.
 * <p>
 * A link provides loose coupling between different parts of the object model.
 * For example, an equity trade can hold a link to the underlying equity.
 * The two objects can be updated independently of each other.
 * If the link resolver supports historic versions, then the correct version of the
 * target will be returned when resolved.
 * <p>
 * Two styles of link are provided. The first, {@link ResolvableLink}, is the standard
 * implementation that links to the target using a {@link StandardId} and type.
 * The second, {@link ResolvedLink}, embeds the target object within the link.
 * <p>
 * Links are expected to be resolvable. It is reasonable to expect that when
 * {@link #resolve(LinkResolver)} is called, the target of the link is available.
 * For this reason, if the target is not found, a {@link LinkResolutionException} will be thrown.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 *
 * @param <T> type of the link, which ensures that when the link
 *   is resolved no casting is required by the caller
 */
public interface Link<T extends IdentifiableBean> {

  /**
   * Create a resolvable link for the specified identifier and type.
   * <p>
   * When, at some subsequent point, the {@link #resolve(LinkResolver)}
   * method is called on the created link, the target for the
   * link will be looked up and returned to the caller.
   *
   * @param identifier  the identifier for the link's target
   * @param linkType  the type of the link target
   * @return a new resolvable link
   */
  public static <T extends IdentifiableBean> Link<T> resolvable(StandardId identifier, Class<T> linkType) {
    return new ResolvableLink<>(identifier, linkType);
  }

  /**
   * Create a link with the link target embedded directly.
   * <p>
   * When, at some subsequent point, the {@link #resolve(LinkResolver)}
   * method is called on the created link, the target embedded
   * in the link will be returned to the caller.
   *
   * @param linkTarget  the link target
   * @return a new resolved link
   */
  public static <T extends IdentifiableBean> Link<T> resolved(T linkTarget) {
    return new ResolvedLink<>(linkTarget);
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves the link using the supplied resolver.
   * <p>
   * A {@link ResolvableLink} will use the specified resolver to return the link target.
   * A {@link ResolvedLink} will return the target stored when it was created.
   *
   * @param linkResolver  the resolver used to resolve the link
   * @return the resolved target
   * @throws LinkResolutionException if the link cannot be resolved
   */
  public abstract T resolve(LinkResolver linkResolver);

}
