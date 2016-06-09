/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.ResolvedTrade;

/**
 * Provides the calibration value.
 * <p>
 * This provides the value from the specified {@link CalibrationMeasures} instance
 * in matrix form suitable for use in curve calibration root finding.
 * The value will typically be par spread or converted present value.
 */
class CalibrationValue
    implements Function<DoubleArray, DoubleArray> {

  /**
   * The trades.
   */
  private final List<ResolvedTrade> trades;
  /**
   * The calibration measures.
   */
  private final CalibrationMeasures measures;
  /**
   * The provider generator, used to create child providers.
   */
  private final RatesProviderGenerator providerGenerator;

  /**
   * Creates an instance.
   * 
   * @param trades  the trades
   * @param measures  the calibration measures
   * @param providerGenerator  the provider generator, used to create child providers
   */
  CalibrationValue(
      List<ResolvedTrade> trades,
      CalibrationMeasures measures,
      RatesProviderGenerator providerGenerator) {

    this.trades = trades;
    this.measures = measures;
    this.providerGenerator = providerGenerator;
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleArray apply(DoubleArray x) {
    // create child provider from matrix
    ImmutableRatesProvider childProvider = providerGenerator.generate(x);
    // calculate value for each trade using the child provider
    return DoubleArray.of(trades.size(), i -> measures.value(trades.get(i), childProvider));
  }

}
