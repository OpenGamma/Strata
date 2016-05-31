/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;

/**
 * A trade that is directly based on a securitized product.
 * <p>
 * This defines a trade in a securitized product.
 * A securitized product contains the structure of a financial instrument that is traded as a {@link Security}.
 * See {@link SecuritizedProduct} to understand the difference between a security and a securitized product.
 * <p>
 * When trading securities, the standard trade type is {@link SecurityTrade}.
 * That trade type relies on securities being looked up in {@link ReferenceData}.
 * One use for trade types that implement {@code SecuritizedProductTrade} is to price
 * and hold trades without needing to populate reference data, because the securitized
 * product representation completely models the trade.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface SecuritizedProductTrade
    extends ProductTrade, SecurityQuantity {

  /**
   * Gets the product of the security that was traded.
   * 
   * @return the product
   */
  @Override
  public abstract SecuritizedProduct getProduct();

  /**
   * Gets the quantity that was traded.
   * <p>
   * This will be positive if buying and negative if selling.
   * 
   * @return the quantity
   */
  @Override
  public abstract double getQuantity();

  /**
   * Gets the price that was traded.
   * <p>
   * This is the unit price agreed when the trade occurred.
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
  @Override
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
