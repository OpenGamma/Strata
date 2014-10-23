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

/**
 * A link resolver backed by a Source. The source is
 * used to get the target of the link.
 */
public class SourceLinkResolver implements LinkResolver {

  /**
   * Source used to retrieve the link target.
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

  @Override
  public <T extends IdentifiableBean> T resolve(ResolvableLink<T> link) {
    Result<T> result = source.get(link.getIdentifier(), link.getLinkType());
    if (result.isSuccess()) {
      return result.getValue();
    } else {
      throw new LinkResolutionException(link, result.getFailure());
    }
  }
}
