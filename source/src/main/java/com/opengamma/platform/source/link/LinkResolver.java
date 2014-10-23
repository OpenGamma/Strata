/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.link;

import com.opengamma.platform.source.Source;
import com.opengamma.platform.source.id.IdentifiableBean;

/**
 * A link resolver is responsible for resolving {@link ResolvableLink}s
 * using an appropriate {@link Source} for looking up the data.
 * If the link is not resolvable (because the id not exist, or the
 * declared type is incorrect), then a {@link LinkResolutionException}
 * will be thrown
 */
public interface LinkResolver {

  /**
   * Resolve the supplied link, returning the realised target
   * of the link. If the link cannot be resolved a
   * {@code LinkResolutionException} will be thrown.
   *
   * @param <T>  the type of the target of the link
   * @param link  the link to be resolved
   * @return the resolved target of the link
   * @throws LinkResolutionException if the link cannot be resolved
   */
  public abstract <T extends IdentifiableBean> T resolve(ResolvableLink<T> link);

}
