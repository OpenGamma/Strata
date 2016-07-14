/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.swap.DispatchingSwapPaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;

/**
 * Pricer for payment periods.
 * <p>
 * This function provides the ability to price a {@link SwapPaymentPeriod}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of period
 */
public interface SwapPaymentPeriodPricer<T extends SwapPaymentPeriod> {

  /**
   * Returns the standard instance of the function.
   * <p>
   * Use this method to avoid a direct dependency on the implementation.
   * 
   * @return the payment period pricer
   */
  public static SwapPaymentPeriodPricer<SwapPaymentPeriod> standard() {
    return DispatchingSwapPaymentPeriodPricer.DEFAULT;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of a single payment period.
   * <p>
   * The amount is expressed in the currency of the period.
   * This returns the value of the period with discounting.
   * <p>
   * The payment date of the period should not be in the past.
   * The result of this method for payment dates in the past is undefined.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @return the present value of the period
   */
  public abstract double presentValue(T period, RatesProvider provider);

  /**
   * Calculates the present value sensitivity of a single payment period.
   * <p>
   * The present value sensitivity of the period is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the period
   */
  public abstract PointSensitivityBuilder presentValueSensitivity(T period, RatesProvider provider);

  //-------------------------------------------------------------------------
  /**
   * Calculates the forecast value of a single payment period.
   * <p>
   * The amount is expressed in the currency of the period.
   * This returns the value of the period without discounting.
   * <p>
   * The payment date of the period should not be in the past.
   * The result of this method for payment dates in the past is undefined.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @return the forecast value of the period
   */
  public abstract double forecastValue(T period, RatesProvider provider);

  /**
   * Calculates the forecast value sensitivity of a single payment period.
   * <p>
   * The forecast value sensitivity of the period is the sensitivity of the forecast value to
   * the underlying curves.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @return the forecast value curve sensitivity of the period
   */
  public abstract PointSensitivityBuilder forecastValueSensitivity(T period, RatesProvider provider);

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of a basis point of a period.
   * <p>
   * This calculate the amount by which, to the first order, the period present value
   * changes for a change of the rate defining the payment period. For known amount
   * payments for which there is rate, the value is 0. In absence of compounding on
   * the period, this measure is equivalent to the traditional PVBP.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @return the present value of a basis point
   */
  public abstract double pvbp(T period, RatesProvider provider);

  /**
   * Calculates the present value of a basis point sensitivity of a single payment period.
   * <p>
   * This calculate the sensitivity of the present value of a basis point (pvbp) quantity
   * to the underlying curves.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @return the present value of a basis point sensitivity
   */
  public abstract PointSensitivityBuilder pvbpSensitivity(T period, RatesProvider provider);

  //-------------------------------------------------------------------------
  /**
   * Calculates the accrued interest since the last payment.
   * <p>
   * This calculates the interest that has accrued between the start of the period
   * and the valuation date. Discounting is not applied.
   * The amount is expressed in the currency of the period.
   * It is intended that this method is called only with the period where the
   * valuation date is after the start date and before or equal to the end date.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @return the accrued interest of the period
   */
  public abstract double accruedInterest(T period, RatesProvider provider);

  //-------------------------------------------------------------------------
  /**
   * Explains the present value of a single payment period.
   * <p>
   * This adds information to the {@link ExplainMapBuilder} to aid understanding of the calculation.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @param builder  the builder to populate
   */
  public abstract void explainPresentValue(
      T period,
      RatesProvider provider,
      ExplainMapBuilder builder);

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of a single payment period.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public abstract MultiCurrencyAmount currencyExposure(T period, RatesProvider provider);

  /**
   * Calculates the current cash of a single payment period.
   * 
   * @param period  the period
   * @param provider  the rates provider
   * @return the current cash
   */
  public abstract double currentCash(T period, RatesProvider provider);
}
