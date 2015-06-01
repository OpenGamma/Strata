/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Represents an amount of a currency associated with one leg of an instrument.
 */
public interface LegAmount {
  
  /**
   * Gets the amount associated with the leg.
   * 
   * @return  the amount
   */
  CurrencyAmount getAmount();

}
