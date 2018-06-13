/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.ReferenceData;

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
 * 
 * @param <P> the type of securitized product
 */
public interface SecuritizedProductTrade<P extends SecuritizedProduct>
    extends ProductTrade, SecurityQuantityTrade, SecuritizedProductPortfolioItem<P> {

  /**
   * Returns an instance with the specified info.
   * 
   * @param info  the new info
   * @return the instance with the specified info
   */
  @Override
  public abstract SecuritizedProductTrade<P> withInfo(TradeInfo info);

  /**
   * Returns an instance with the specified quantity.
   * 
   * @param quantity  the new quantity
   * @return the instance with the specified quantity
   */
  @Override
  public abstract SecuritizedProductTrade<P> withQuantity(double quantity);

  /**
   * Returns an instance with the specified price.
   * 
   * @param price  the new price
   * @return the instance with the specified price
   */
  @Override
  public abstract SecuritizedProductTrade<P> withPrice(double price);

}
