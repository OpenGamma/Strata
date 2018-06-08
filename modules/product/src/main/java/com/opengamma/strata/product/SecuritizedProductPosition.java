/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;

/**
 * A position that is directly based on a securitized product.
 * <p>
 * This defines a position in a securitized product.
 * A securitized product contains the structure of a financial instrument that is traded as a {@link Security}.
 * See {@link SecuritizedProduct} to understand the difference between a security and a securitized product.
 * <p>
 * When storing positions, the standard trade type is {@link SecurityPosition}.
 * That type relies on securities being looked up in {@link ReferenceData}.
 * One use for positions that implement {@code SecuritizedProductPosition} is to price
 * and hold positions without needing to populate reference data, because the securitized
 * product representation completely models the trade.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface SecuritizedProductPosition
    extends Position {

  /**
   * Gets the product of the security that was traded.
   * 
   * @return the product
   */
  public abstract SecuritizedProduct getProduct();

  /**
   * Gets the currency of the position.
   * <p>
   * This is typically the same as the currency of the product.
   * 
   * @return the trading currency
   */
  public default Currency getCurrency() {
    return getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified info.
   * 
   * @param info  the new info
   * @return the instance with the specified info
   */
  @Override
  public abstract SecuritizedProductPosition withInfo(PositionInfo info);

  /**
   * Returns an instance with the specified quantity.
   * 
   * @param quantity  the new quantity
   * @return the instance with the specified quantity
   */
  @Override
  public abstract SecuritizedProductPosition withQuantity(double quantity);

}
