/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Curve calibrator.
 * <p>
 * This calibrator takes an abstract curve definition and produces real curves.
 * <p>
 * Curves are calibrated in groups or one or more curves.
 * In addition, more than one group may be calibrated together.
 * <p>
 * Each curve is defined using two or more {@linkplain CurveNode nodes}.
 * Each node primarily defines enough information to produce a reference trade.
 * Calibration involves pricing, and re-pricing, these trades to find the best fit
 * using a root finder.
 * <p>
 * Once calibrated, the curves are then available for use.
 * Each node in the curve definition becomes a parameter in the matching output curve.
 */
public interface CurveCalibrator {

  /**
   * Obtains a curve calibrator, specifying tolerances and measures to use.
   *
   * @param toleranceAbs the absolute tolerance
   * @param toleranceRel the relative tolerance
   * @param stepMaximum the maximum steps
   * @param measures the calibration measures, used to compute the function for which the root is found
   * @return the curve calibrator
   */
  public static CurveCalibrator of(
      double toleranceAbs,
      double toleranceRel,
      int stepMaximum,
      CalibrationMeasures measures) {

    return StandardCurveCalibrator.of(toleranceAbs, toleranceRel, stepMaximum, measures);
  }

  /**
   * The default curve calibrator.
   * <p>
   * This uses the default tolerance of 1e-9, a maximum of 1000 steps and the
   * default {@link CalibrationMeasures} instance.
   *
   * @return the default curve calibrator
   */
  public static CurveCalibrator defaultCurveCalibrator() {
    return StandardCurveCalibrator.DEFAULT;
  }

  //-------------------------------------------------------------------------

  /**
   * Calibrates a single curve group, containing one or more curves.
   * <p>
   * The calibration is defined using {@link CurveGroupDefinition}.
   * Observable market data, time-series and FX are also needed to complete the calibration.
   *
   * @param curveGroupDefn the curve group definition
   * @param valuationDate the validation date
   * @param marketData the market data required to build a trade for the instrument
   * @param timeSeries the time-series
   * @return the rates provider resulting from the calibration
   */
  public abstract ImmutableRatesProvider calibrate(
      CurveGroupDefinition curveGroupDefn,
      LocalDate valuationDate,
      MarketData marketData,
      Map<Index, LocalDateDoubleTimeSeries> timeSeries);

  //-------------------------------------------------------------------------

  /**
   * Calibrates a list of curve groups, each containing one or more curves.
   * <p>
   * The calibration is defined using a list of {@link CurveGroupDefinition}.
   * Observable market data and existing known data are also needed to complete the calibration.
   * <p>
   * A curve must only exist in one group.
   *
   * @param allGroupsDefn the curve group definitions
   * @param knownData the starting data for the calibration
   * @param marketData the market data required to build a trade for the instrument
   * @return the rates provider resulting from the calibration
   */
  public abstract ImmutableRatesProvider calibrate(
      List<CurveGroupDefinition> allGroupsDefn,
      ImmutableRatesProvider knownData,
      MarketData marketData);

}
