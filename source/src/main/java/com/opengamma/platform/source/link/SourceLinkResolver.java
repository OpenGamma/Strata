/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.link;

import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.result.Result;
import com.opengamma.platform.source.Source;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * A link resolver backed by a {@link Source}.
 * <p>
 * The source is used to get the target of the link. If
 * it is not possible to resolve the link using the source
 * then a {@link LinkResolutionException} will be thrown.
 */
public class SourceLinkResolver implements LinkResolver {

  /**
   * The source used to retrieve the link target.
   */
  private final Source source;

  /**
   * Resolve the link using the supplied source.
   *
   * @param source  the source to use to resolve the link
   */
  public SourceLinkResolver(Source source) {
    this.source = ArgChecker.notNull(source, "source");
  }

  /**
   * Resolve the supplied link using the source, returning the
   * realised target of the link.
   * <p>
   * A call is made to {@link Source#get(StandardId, Class)} and
   * if the returned result indicates a failure, a
   * {@code LinkResolutionException} will be thrown with the
   * failure reason taken from the Result.
   *
   * @param <T>  the type of the target of the link
   * @param link  the link to be resolved
   * @return the resolved target of the link
   * @throws LinkResolutionException if the link cannot be resolved
   */
  @Override
  public <T extends IdentifiableBean> T resolve(ResolvableLink<T> link) {
    Result<T> result = source.get(link.getIdentifier(), link.getLinkType());
    if (result.isSuccess()) {
      return result.getValue();
    } else {
      throw new LinkResolutionException(link, result.getFailure());
    }
  }

  @Override
  public String toString() {
    return "SourceLinkResolver[source=" + source + "]";
  }
}
