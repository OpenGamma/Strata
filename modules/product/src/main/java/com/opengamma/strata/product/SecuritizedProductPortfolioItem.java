/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;

/**
 * A trade that is directly based on a securitized product.
 * <p>
 * This defines a trade or position in a securitized product.
 * A securitized product contains the structure of a financial instrument that is traded as a {@link Security}.
 * See {@link SecuritizedProduct} to understand the difference between a security and a securitized product.
 * <p>
 * When trading securities, the standard trade type is {@link SecurityTrade} and the
 * standard position type is {@link SecurityPosition}.
 * That trade type relies on securities being looked up in {@link ReferenceData}.
 * One use for trade types that implement {@code SecuritizedProductTrade} is to price
 * and hold trades without needing to populate reference data, because the securitized
 * product representation completely models the trade.
 * <p>
 * Implementations of this interface must be immutable beans.
 * 
 * @param <P> the type of securitized product
 */
public interface SecuritizedProductPortfolioItem<P extends SecuritizedProduct>
    extends PortfolioItem, SecurityQuantity {

  /**
   * Gets the product of the security that was traded.
   * 
   * @return the product
   */
  public abstract P getProduct();

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

  @Override
  public default SecurityId getSecurityId() {
    return getProduct().getSecurityId();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified quantity.
   * 
   * @param quantity  the new quantity
   * @return the instance with the specified quantity
   */
  public abstract SecuritizedProductPortfolioItem<P> withQuantity(double quantity);

}
