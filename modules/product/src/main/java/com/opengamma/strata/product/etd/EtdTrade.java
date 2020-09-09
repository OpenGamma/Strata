/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityQuantityTrade;

/**
 * A trade in an exchange traded derivative (ETD).
 * <p>
 * This represents a trade based on quantity and price for an {@link EtdSecurity}.
 */
public interface EtdTrade extends SecurityQuantityTrade {

  /**
   * Gets the currency of the trade.
   * <p>
   * This is the currency of the underlying security.
   *
   * @return the trading currency
   */
  public default Currency getCurrency() {
    return getSecurity().getCurrency();
  }

  /**
   * Gets the type of the contract that was traded.
   * <p>
   * This is the type of the underlying security - future or option.
   *
   * @return the type, future or option
   */
  public default EtdType getType() {
    return getSecurity().getType();
  }

  /**
   * Gets the security identifier of the trade.
   * <p>
   * This identifier uniquely identifies the security within the system.
   *
   * @return the security identifier
   */
  @Override
  public default SecurityId getSecurityId() {
    return getSecurity().getSecurityId();
  }

  /**
   * Gets the underlying ETD security.
   *
   * @return the ETD security
   */
  public abstract EtdSecurity getSecurity();
}
