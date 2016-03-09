/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.Currency;

/**
 * A trade that is directly based on a security.
 * <p>
 * A security product trade is a {@link Trade} that directly contains a reference to a {@link SecurityProduct}.
 * <p>
 * A {@linkplain SecurityProduct} includes all the financial details of a security.
 * It will include any underlying securities.
 * For example, a bond future option would include the underlying bond future
 * and the basket of bonds.
 * <p>
 * The product of a security is distinct from the {@linkplain ReferenceSecurity security}.
 * The security, referred to using a {@link SecurityId}, does not include underlying securities.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface SecurityProductTrade
    extends ProductTrade {

  /**
   * Gets the product of the security that was traded.
   * 
   * @return the product
   */
  @Override
  public abstract SecurityProduct getProduct();

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
    return getProduct().getSecurityId();
  }

  /**
   * Gets the currency of the trade.
   * <p>
   * This is typically the same as the currency of the product.
   * 
   * @return the trading currency
   */
  public default Currency getCurrency() {
    return getProduct().getCurrency();
  }

}
