/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityQuantity;

/**
 * A position in an ETD, where the security is embedded ready for mark-to-market pricing.
 * <p>
 * This represents a position in an ETD, defined by long and short quantity.
 * The ETD security is embedded directly.
 * <p>
 * The net quantity of the position is stored using two fields - {@code longQuantity} and {@code shortQuantity}.
 * These two fields must not be negative.
 * In many cases, only a long quantity or short quantity will be present with the other set to zero.
 * However it is also possible for both to be non-zero, allowing long and short positions to be treated separately.
 * The net quantity is available via {@link #getQuantity()}.
 */
public interface EtdPosition
    extends Position, SecurityQuantity {

  /**
   * Gets the currency of the position.
   * <p>
   * This is the currency of the security.
   *
   * @return the trading currency
   */
  public default Currency getCurrency() {
    return getSecurity().getCurrency();
  }

  /**
   * Gets the underlying ETD security.
   * 
   * @return the ETD security
   */
  public abstract EtdSecurity getSecurity();

  /**
   * Gets the type of the contract - future or option.
   * 
   * @return the type, future or option
   */
  public default EtdType getType() {
    return getSecurity().getType();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the security identifier.
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
   * Gets the net quantity of the security.
   * <p>
   * This returns the <i>net</i> quantity of the underlying security.
   * The result is positive if the net position is <i>long</i> and negative
   * if the net position is <i>short</i>.
   * <p>
   * This is calculated by subtracting the short quantity from the long quantity.
   *
   * @return the net quantity of the underlying security
   */
  @Override
  public abstract double getQuantity();

  /**
   * Gets the long quantity of the security.
   * <p>
   * This is the quantity of the underlying security that is held.
   * The quantity cannot be negative, as that would imply short selling.
   * 
   * @return the long quantity
   */
  public abstract double getLongQuantity();

  /**
   * Gets the short quantity of the security.
   * <p>
   * This is the quantity of the underlying security that has been short sold.
   * The quantity cannot be negative, as that would imply the position is long.
   * 
   * @return the short quantity
   */
  public abstract double getShortQuantity();

}
