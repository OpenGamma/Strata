/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxConvertible;

/**
 * Represents an amount of a currency associated with one leg of an instrument.
 */
public interface LegAmount extends FxConvertible<LegAmount> {

  /**
   * Gets the amount associated with the leg.
   * 
   * @return  the amount
   */
  CurrencyAmount getAmount();

}
