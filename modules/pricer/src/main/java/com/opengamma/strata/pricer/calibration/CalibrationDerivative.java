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
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Provides the calibration derivative.
 * <p>
 * This provides the value sensitivity from the specified {@link CalibrationMeasures}
 * instance in matrix form suitable for use in curve calibration root finding.
 * The value will typically be par spread or converted present value.
 */
class CalibrationDerivative
    extends Function1D<DoubleMatrix1D, DoubleMatrix2D> {

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
   * Provide the order in which the curves appear in the long vector result.
   * The expected number of parameters for each curve is also provided.
   */
  private final List<CurveParameterSize> curveOrder;

  /**
   * Creates an instance.
   * 
   * @param trades  the trades
   * @param measures  the calibration measures
   * @param providerGenerator  the provider generator, used to create child providers
   * @param curveOrder  the curve order
   */
  public CalibrationDerivative(
      List<Trade> trades,
      CalibrationMeasures measures,
      RatesProviderGenerator providerGenerator,
      List<CurveParameterSize> curveOrder) {

    this.measures = measures;
    this.trades = trades;
    this.providerGenerator = providerGenerator;
    this.curveOrder = curveOrder;
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
    // create child provider from matrix
    double[] data = x.getData();
    ImmutableRatesProvider provider = providerGenerator.generate(data);
    // calculate derivative for each trade using the child provider
    int size = trades.size();
    double[][] measure = new double[size][size];
    for (int i = 0; i < size; i++) {
      measure[i] = measures.derivative(trades.get(i), provider, curveOrder);
    }
    return new DoubleMatrix2D(measure);
  }

}
