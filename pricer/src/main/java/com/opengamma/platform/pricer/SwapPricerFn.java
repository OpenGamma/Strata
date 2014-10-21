/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer;

import java.time.LocalDate;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.trade.swap.SwapTrade;

/**
 * Pricer for swaps.
 */
public interface SwapPricerFn {

  /**
   * Calculates the present value of the swap.
   * 
   * @param environment  the pricing environment
   * @param valulationDate  the valuation date
   * @param trade  the trade to price
   * @return the present value of the swap
   */
  public CurrencyAmount presentValue(MulticurveProviderInterface environment, LocalDate valulationDate, SwapTrade trade);

}
