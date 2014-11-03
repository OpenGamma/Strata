/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.link;

import com.opengamma.platform.source.Source;
import com.opengamma.platform.source.id.IdentifiableBean;

/**
 * A link resolver is responsible for resolving resolvable links.
 * <p>
 * Link resolution occurs using a {@link Source} to provide the data.
 * If the link is not resolvable (because the identifier does not exist, or the
 * declared type is incorrect), then a {@link LinkResolutionException} is thrown.
 */
public interface LinkResolver {

  /**
   * Resolve the supplied link, returning the realized target of the link.
   * <p>
   * If the link cannot be resolved a {@code LinkResolutionException} will be thrown.
   *
   * @param <T>  the type of the target of the link
   * @param link  the link to be resolved
   * @return the resolved target of the link
   * @throws LinkResolutionException if the link cannot be resolved
   */
  public abstract <T extends IdentifiableBean> T resolve(ResolvableLink<T> link);

}
