/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.currency.Currency;

/**
 * A trade representing the purchase or sale of a security,
 * where the security is embedded ready for mark-to-market pricing.
 * <p>
 * This trade represents a trade in a security, defined by a quantity and price.
 * The security is embedded directly, however the underlying product model is not available.
 * The security may be of any kind, including equities, bonds and exchange traded derivative (ETD).
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface SecurityTrade
    extends FinanceTrade {

  /**
   * Gets the security that was traded.
   * 
   * @return the security
   */
  public abstract Security getSecurity();

  /**
   * Gets the quantity that was traded.
   * <p>
   * This will be positive if buying and negative if selling.
   * 
   * @return the quantity
   */
  public abstract long getQuantity();

  /**
   * Gets the price that was traded.
   * <p>
   * This is the price agreed when the trade occurred.
   * 
   * @return the price
   */
  public abstract double getPrice();

  //-------------------------------------------------------------------------
  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * 
   * @return the security identifier
   */
  public default SecurityId getSecurityId() {
    return getSecurity().getSecurityId();
  }

  /**
   * Gets the currency of the trade.
   * <p>
   * This is the currency of the security.
   * 
   * @return the trading currency
   */
  public default Currency getCurrency() {
    return getSecurity().getCurrency();
  }

}
