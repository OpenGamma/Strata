/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolutionException;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.Result;

/**
 * A link resolver backed by a source.
 * <p>
 * The {@link Source} is used to get the target of the link.
 * If it is not possible to resolve the link using the source
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

  //-------------------------------------------------------------------------
  /**
   * Resolve the supplied link using the source, returning the
   * realized target of the link.
   * <p>
   * A call is made to {@link Source#get(StandardId, TypeToken)}.
   * If the returned result indicates a failure, a {@code LinkResolutionException}
   * will be thrown with the failure reason taken from the {@code Result}.
   *
   * @param <T>  the type of the target of the link
   * @param identifier  the identifier to be resolved
   * @param targetType  the target type of the link
   * @return the resolved target of the link
   * @throws LinkResolutionException if the link cannot be resolved
   */
  @Override
  public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
    Result<T> result = source.get(identifier, targetType);
    return result
        .getValueOrElseApply(failure -> {
          throw new LinkResolutionException("Unable to resolve link to: " + identifier + ", " + failure.getMessage());
        });
  }

  @Override
  public String toString() {
    return "SourceLinkResolver[source=" + source + "]";
  }

}
