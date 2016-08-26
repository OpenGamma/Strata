/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.time.LocalDate;
import java.util.function.Function;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.RatesProviderDecoratedForward;

/**
 * Computes the theta for interest rate products.
 * <p>
 * Reference: OpenGamma Documentation n28, Generic Interest Rate Theta Calculators and P/L. August 2016.
 */
public class ThetaCalculator {
  
  /**
   * Computes the theta by difference of PV between two dates.
   * <p>
   * The theta is the change of value between two dates. The valuation should have no adjustments, i.e. no cash flow
   * payments, between the original valuation date and the forward date. Adjustment should be only on the start date 
   * and represented by the adjustment function.
   * 
   * @param multicurve  the starting rates provider
   * @param forwardDate  the forward date
   * @param valueFunction  the function used to compute the value, usually the present value
   * @param adjustmentFunction  the function used to compute the adjustment, usually the current cash method
   * @return the adjusted change of value between the underlying valuation date and the forward date
   */
  public MultiCurrencyAmount theta(
      RatesProvider multicurve,
      LocalDate forwardDate,
      Function<RatesProvider, MultiCurrencyAmount> valueFunction,
      Function<RatesProvider, MultiCurrencyAmount> adjustmentFunction) {
    MultiCurrencyAmount valueStart = valueFunction.apply(multicurve);
    MultiCurrencyAmount adjustmentStart = adjustmentFunction.apply(multicurve);
    RatesProvider multicurveForward = RatesProviderDecoratedForward.of(multicurve, forwardDate);
    MultiCurrencyAmount valueEnd = valueFunction.apply(multicurveForward);
    return valueEnd.minus(valueStart.minus(adjustmentStart));
  }

}
