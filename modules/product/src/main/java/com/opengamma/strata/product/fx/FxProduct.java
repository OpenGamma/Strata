/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.product.Product;

/**
 * A foreign exchange product, such as an FX forward, FX spot or FX option.
 * <p>
 * FX products operate on two different currencies.
 * For example, it might represent the payment of USD 1,000 and the receipt of EUR 932.
 */
public interface FxProduct extends Product {

  /**
   * Gets the currency pair that the FX trade is based on, in conventional order.
   * <p>
   * This represents the main currency pair of the FX. If the trade settles in a
   * third currency, that is not recorded here.
   * 
   * @return the currency pair
   */
  public abstract CurrencyPair getCurrencyPair();

}
