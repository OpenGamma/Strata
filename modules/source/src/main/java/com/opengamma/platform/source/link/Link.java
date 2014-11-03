/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.link;

import com.opengamma.platform.source.Source;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * Represents a link to an object using the object's identifier
 * and type which can be resolved on demand. Use of links allows
 * references to be made between separate serialized objects.
 * These objects can then be updated independently of each other
 * and the link will point at the correct version (dependent on
 * the link resolution that is performed).
 * <p>
 * Links also support a form where the link's target is embedded
 * in the link itself. When these are resolved, the embedded
 * object is returned directly.
 * <p>
 * Links are expected to be resolvable (i.e. it is reasonable to
 * expect that when {@link #resolve(LinkResolver)} is called, the
 * target of the link is available. For this reason, if the
 * target is not found, a {@link LinkResolutionException} will
 * be thrown.
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
   * link will be looked up (generally from a {@link Source})
   * and returned to the caller.
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
   * @param linkable  the link target
   * @return a new resolved link
   */
  public static <T extends IdentifiableBean> Link<T> resolved(T linkable) {
    return new ResolvedLink<>(linkable);
  }

  //-------------------------------------------------------------------------
  /**
   * Resolve the link using the supplied resolver.
   *
   * @param linkResolver  the resolver used to resolve the link
   * @return the resolved target
   * @throws LinkResolutionException if the link cannot be resolved
   */
  public abstract T resolve(LinkResolver linkResolver);

}
