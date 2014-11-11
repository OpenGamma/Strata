/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.Currency;

/**
 * A single leg of a swap trade.
 * <p>
 * A swap leg can be expressed in a parameterized form or in an expanded form.
 * All swaps can be converted to the expanded form, which is structured in terms of
 * a list of payment periods.
 */
public interface SwapLeg {

  /**
   * Gets the start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the period
   */
  public abstract LocalDate getStartDate();

  /**
   * Gets the end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the maturity date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date of the period
   */
  public abstract LocalDate getEndDate();

  /**
   * Gets the currency of the leg.
   * <p>
   * A swap leg has a single currency.
   * 
   * @return the currency of the leg
   */
  public abstract Currency getCurrency();

  /**
   * Converts this swap leg to the equivalent expanded swap leg.
   * <p>
   * All swap legs can be converted to this standard format.
   * 
   * @return the equivalent expanded swap leg
   * @throws RuntimeException if the swap leg is invalid
   */
  public abstract ExpandedSwapLeg toExpanded();

}
