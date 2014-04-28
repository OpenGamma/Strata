/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.math.BigDecimal;

import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.core.security.SecurityLink;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.util.ArgumentChecker;

/**
 * Wraps a {@link PositionOrTrade} and uses a new style {@link com.opengamma.core.link.SecurityLink} in {@link #getSecurity()}.
 * The security is lazily resolved when {@link #getSecurity()} is called. This is different from the behaviour of
 * the old style {@link SecurityLink} where the link must be explicitly resolved before the security is available.
 */
public class LazyLinkedPositionOrTrade implements PositionOrTrade {

  private final PositionOrTrade _delegate;
  private final com.opengamma.core.link.SecurityLink<?> _securityLink;

  /**
   * Creates a new instance wrapping the delegate position / trade.
   *
   * @param delegate the delegate position or trade
   */
  public LazyLinkedPositionOrTrade(PositionOrTrade delegate) {
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
    ExternalIdBundle externalId = delegate.getSecurityLink().getExternalId();

    if (externalId == null) {
      throw new IllegalArgumentException("Position / trade must have a security link with an external ID bundle");
    }
    _securityLink = com.opengamma.core.link.SecurityLink.resolvable(externalId);
  }

  @Override
  public UniqueId getUniqueId() {
    return _delegate.getUniqueId();
  }

  @Override
  public BigDecimal getQuantity() {
    return _delegate.getQuantity();
  }

  /**
   * Throws {@code UnsupportedOperationException}.
   * Use {@link #getSecurity()} instead.
   *
   * @return never returns, always throws an exception
   * @throws UnsupportedOperationException always
   */
  @Override
  public SecurityLink getSecurityLink() {
    throw new UnsupportedOperationException("Use getSecurity() to retrieve the security");
  }

  /**
   * @return the security using {@link com.opengamma.core.link.SecurityLink#resolve()}
   */
  @Override
  public Security getSecurity() {
    return _securityLink.resolve();
  }
}
