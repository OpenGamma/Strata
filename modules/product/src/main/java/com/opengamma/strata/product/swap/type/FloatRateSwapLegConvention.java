/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.RateIndex;

/**
 * A market convention for the floating leg of rate swap trades based on an Ibor index or an Overnight index.
 */
public interface FloatRateSwapLegConvention
    extends SwapLegConvention {

  /**
   * Gets the index of the convention.
   * 
   * @return the underlying rate index
   */
  public abstract RateIndex getIndex();

  /**
   * Gets the currency of the convention.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * Gets the day count of the convention.
   * 
   * @return the day count
   */
  public abstract DayCount getDayCount();

  /**
   * Gets the offset of the payment date from the base date.
   * 
   * @return the payment day offset
   */
  public abstract DaysAdjustment getPaymentDateOffset();

  /**
   * Gets the business day adjustment to apply to the start date.
   * 
   * @return the start date business day adjustment, not null
   */
  public abstract BusinessDayAdjustment getStartDateBusinessDayAdjustment();

  /**
   * Gets the business day adjustment to apply to the end date.
   * 
   * @return the end date business day adjustment, not null
   */
  public abstract BusinessDayAdjustment getEndDateBusinessDayAdjustment();

}
