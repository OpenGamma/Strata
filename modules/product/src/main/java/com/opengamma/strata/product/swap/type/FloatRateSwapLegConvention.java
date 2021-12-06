/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.RateIndex;

/**
 * Ibor and Overnight
 */
public interface FloatRateSwapLegConvention
    extends SwapLegConvention {

  /**
   * s
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * d
   * @return the day count
   */
  public abstract DayCount getDayCount();

  /**
   * o
   * @return the payment day offset
   */
  public abstract DaysAdjustment getPaymentDateOffset();

  /**
   * i
   * @return the underlying rate index
   */
  public abstract RateIndex getIndex();

}
