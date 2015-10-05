/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import java.util.List;

import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Provides the calibration value.
 * <p>
 * This provides the value from the specified {@link CalibrationMeasures} instance
 * in matrix form suitable for use in curve calibration root finding.
 * The value will typically be par spread or converted present value.
 */
class CalibrationValue
    extends Function1D<DoubleMatrix1D, DoubleMatrix1D> {

  /**
   * The trades.
   */
  private final List<Trade> trades;
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
      List<Trade> trades,
      CalibrationMeasures measures,
      RatesProviderGenerator providerGenerator) {

    this.trades = trades;
    this.measures = measures;
    this.providerGenerator = providerGenerator;
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
    // create child provider from matrix
    double[] data = x.getData();
    ImmutableRatesProvider childProvider = providerGenerator.generate(data);
    // calculate value for each trade using the child provider
    int size = trades.size();
    double[] measure = new double[size];
    for (int i = 0; i < size; i++) {
      measure[i] = measures.value(trades.get(i), childProvider);
    }
    return new DoubleMatrix1D(measure);
  }

}
