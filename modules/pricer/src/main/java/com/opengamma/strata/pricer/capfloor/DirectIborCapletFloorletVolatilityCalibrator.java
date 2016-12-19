/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Triple;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.math.impl.linearalgebra.CholeskyDecompositionCommons;
import com.opengamma.strata.math.impl.minimization.PositiveOrZero;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResults;
import com.opengamma.strata.math.impl.statistics.leastsquare.NonLinearLeastSquareWithPenalty;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Caplet volatilities calibration to cap volatilities. 
 * <p>
 * The volatilities of the constituent caplets in the market caps are "model parameters"  
 * and calibrated to the market data under a certain penalty constraint.
 * <p>
 * If the shift curve is not present in {@code DirectIborCapletFloorletVolatilityDefinition}, 
 * the resultant volatility type is the same as the input volatility type. e.g., 
 * Black caplet volatilities are returned if Black cap volatilities are plugged in, 
 * and normal caplet volatilities are returned otherwise. 
 * On the other hand, if the shift curve is present in {@code DirectIborCapletFloorletVolatilityDefinition}, 
 * Black caplet volatilities are returned for any input volatility type.  
 * <p>
 * The calibration is conducted once the cap volatilities are converted to cap prices. 
 * Thus the error values in {@code RawOptionData} are applied in the price space rather than the volatility space.
 */
public class DirectIborCapletFloorletVolatilityCalibrator
    extends IborCapletFloorletVolatilityCalibrator {

  /**
   * Default implementation. 
   */
  public static final DirectIborCapletFloorletVolatilityCalibrator DEFAULT =
      of(VolatilityIborCapFloorLegPricer.DEFAULT, 1.0e-8, ReferenceData.standard());

  /**
   * The positive function.
   * <p>
   * The function returns true if the new trial position is positive or zero.
   */
  private static final Function<DoubleArray, Boolean> POSITIVE = new PositiveOrZero();
  /**
   * The conventional surface interpolator for the calibration.
   * <p>
   * Since node points are always hit in the calibration, the calibration does not rely on this interpolator generally.
   */
  private static final GridSurfaceInterpolator INTERPOLATOR =
      GridSurfaceInterpolator.of(CurveInterpolators.LINEAR, CurveInterpolators.LINEAR);
  /**
   * The non-linear least square with penalty. 
   */
  private final NonLinearLeastSquareWithPenalty solver;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance. 
   * <p>
   * The epsilon is the parameter used in {@link NonLinearLeastSquareWithPenalty}, where the iteration stops when certain 
   * quantities are smaller than this parameter.
   * 
   * @param pricer  the cap pricer
   * @param epsilon  the epsilon parameter
   * @param referenceData  the reference data
   * @return the instance
   */
  public static DirectIborCapletFloorletVolatilityCalibrator of(
      VolatilityIborCapFloorLegPricer pricer,
      double epsilon,
      ReferenceData referenceData) {

    return new DirectIborCapletFloorletVolatilityCalibrator(pricer, epsilon, referenceData);
  }

  // private constructor
  private DirectIborCapletFloorletVolatilityCalibrator(
      VolatilityIborCapFloorLegPricer pricer,
      double epsilon,
      ReferenceData referenceData) {

    super(pricer, referenceData);
    this.solver = new NonLinearLeastSquareWithPenalty(new CholeskyDecompositionCommons(), epsilon);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborCapletFloorletVolatilityCalibrationResult calibrate(
      IborCapletFloorletVolatilityDefinition definition,
      ZonedDateTime calibrationDateTime,
      RawOptionData capFloorData,
      RatesProvider ratesProvider) {

    ArgChecker.isTrue(ratesProvider.getValuationDate().equals(calibrationDateTime.toLocalDate()),
        "valuationDate of ratesProvider should be coherent to calibrationDateTime");
    ArgChecker.isTrue(definition instanceof DirectIborCapletFloorletVolatilityDefinition,
        "definition should be DirectIborCapletFloorletVolatilityDefinition");
    DirectIborCapletFloorletVolatilityDefinition directDefinition = (DirectIborCapletFloorletVolatilityDefinition) definition;
    // unpack cap data, create node caps
    IborIndex index = directDefinition.getIndex();
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate baseDate = index.getEffectiveDateOffset().adjust(calibrationDate, referenceData);
    LocalDate startDate = baseDate.plus(index.getTenor());
    Function<Surface, IborCapletFloorletVolatilities> volatilitiesFunction = volatilitiesFunction(
        directDefinition, calibrationDateTime, capFloorData);
    SurfaceMetadata metadata = directDefinition.createMetadata(capFloorData);
    List<Period> expiries = capFloorData.getExpiries();
    DoubleArray strikes = capFloorData.getStrikes();
    int nExpiries = expiries.size();
    List<Double> timeList = new ArrayList<>();
    List<Double> strikeList = new ArrayList<>();
    List<Double> volList = new ArrayList<>();
    List<ResolvedIborCapFloorLeg> capList = new ArrayList<>();
    List<Double> priceList = new ArrayList<>();
    List<Double> errorList = new ArrayList<>();
    DoubleMatrix errorMatrix = capFloorData.getError().orElse(DoubleMatrix.filled(nExpiries, strikes.size(), 1d));
    int[] startIndex = new int[nExpiries + 1];
    for (int i = 0; i < nExpiries; ++i) {
      LocalDate endDate = baseDate.plus(expiries.get(i));
      DoubleArray volatilityForTime = capFloorData.getData().row(i);
      DoubleArray errorForTime = errorMatrix.row(i);
      reduceRawData(directDefinition, ratesProvider, capFloorData.getStrikes(), volatilityForTime, errorForTime, startDate,
          endDate, metadata, volatilitiesFunction, timeList, strikeList, volList, capList, priceList, errorList);
      startIndex[i + 1] = volList.size();
      ArgChecker.isTrue(startIndex[i + 1] > startIndex[i], "no valid option data for {}", expiries.get(i));
    }
    // create caplet nodes and initial caplet vol surface
    ResolvedIborCapFloorLeg cap = capList.get(capList.size() - 1);
    int nCaplets = cap.getCapletFloorletPeriods().size();
    DoubleArray capletExpiries = DoubleArray.of(nCaplets, n -> directDefinition.getDayCount().relativeYearFraction(
        calibrationDate, cap.getCapletFloorletPeriods().get(n).getFixingDateTime().toLocalDate()));
    Triple<DoubleArray, DoubleArray, DoubleArray> capletNodes;
    DoubleArray initialVols = DoubleArray.copyOf(volList);
    if (directDefinition.getShiftCurve().isPresent()) {
      metadata = Surfaces.blackVolatilityByExpiryStrike(directDefinition.getName().getName(), directDefinition.getDayCount());
      Curve shiftCurve = directDefinition.getShiftCurve().get();
      if (capFloorData.getDataType().equals(NORMAL_VOLATILITY)) {
      initialVols = DoubleArray.of(capList.size(), n -> volList.get(n) /
          (ratesProvider.iborIndexRates(index).rate(capList.get(n).getFinalPeriod().getIborRate().getObservation()) +
              shiftCurve.yValue(timeList.get(n))));
      }
      InterpolatedNodalSurface capVolSurface = InterpolatedNodalSurface.of(
          metadata, DoubleArray.copyOf(timeList), DoubleArray.copyOf(strikeList), initialVols, INTERPOLATOR);
      capletNodes = createCapletNodes(capVolSurface, capletExpiries, strikes,
          directDefinition.getShiftCurve().get());
      volatilitiesFunction = createShiftedBlackVolatilitiesFunction(index, calibrationDateTime, shiftCurve);
    } else {
      InterpolatedNodalSurface capVolSurface = InterpolatedNodalSurface.of(
          metadata, DoubleArray.copyOf(timeList), DoubleArray.copyOf(strikeList), initialVols, INTERPOLATOR);
      capletNodes = createCapletNodes(capVolSurface, capletExpiries, strikes);
    }
    InterpolatedNodalSurface baseSurface = InterpolatedNodalSurface.of(
        metadata, capletNodes.getFirst(), capletNodes.getSecond(), capletNodes.getThird(), INTERPOLATOR);
    DoubleMatrix penaltyMatrix = directDefinition.computePenaltyMatrix(strikes, capletExpiries);
    // solve least square
    LeastSquareResults res = solver.solve(
        DoubleArray.copyOf(priceList),
        DoubleArray.copyOf(errorList),
        getPriceFunction(capList, ratesProvider, volatilitiesFunction, baseSurface),
        getJacobianFunction(capList, ratesProvider, volatilitiesFunction, baseSurface),
        capletNodes.getThird(),
        penaltyMatrix,
        POSITIVE);
    InterpolatedNodalSurface resSurface = InterpolatedNodalSurface.of(
        metadata, capletNodes.getFirst(), capletNodes.getSecond(), res.getFitParameters(), directDefinition.getInterpolator());
    return IborCapletFloorletVolatilityCalibrationResult.ofLeastSquare(volatilitiesFunction.apply(resSurface), res.getChiSq());
  }

  private Function<Surface, IborCapletFloorletVolatilities> createShiftedBlackVolatilitiesFunction(
      IborIndex index,
      ZonedDateTime calibrationDateTime,
      Curve shiftCurve) {

    Function<Surface, IborCapletFloorletVolatilities> func = new Function<Surface, IborCapletFloorletVolatilities>() {
      @Override
      public IborCapletFloorletVolatilities apply(Surface s) {
        return ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(index, calibrationDateTime, s, shiftCurve);
      }
    };
    return func;
  }

  //-------------------------------------------------------------------------
  private Triple<DoubleArray, DoubleArray, DoubleArray> createCapletNodes(
      InterpolatedNodalSurface capVolSurface,
      DoubleArray capletExpiries,
      DoubleArray strikes) {

    List<Double> timeCapletList = new ArrayList<>();
    List<Double> strikeCapletList = new ArrayList<>();
    List<Double> volCapletList = new ArrayList<>();
    int nTimes = capletExpiries.size();
    int nStrikes = strikes.size();
    for (int i = 0; i < nTimes; ++i) {
      double expiry = capletExpiries.get(i);
      timeCapletList.addAll(DoubleArray.filled(nStrikes, expiry).toList());
      strikeCapletList.addAll(strikes.toList());
      volCapletList.addAll(DoubleArray.of(nStrikes, n -> capVolSurface.zValue(expiry, strikes.get(n))).toList()); // initial guess
    }
    return Triple.of(DoubleArray.copyOf(timeCapletList), DoubleArray.copyOf(strikeCapletList), DoubleArray.copyOf(volCapletList));
  }

  private Triple<DoubleArray, DoubleArray, DoubleArray> createCapletNodes(
      InterpolatedNodalSurface capVolSurface,
      DoubleArray capletExpiries,
      DoubleArray strikes,
      Curve shiftCurve) {

    List<Double> timeCapletList = new ArrayList<>();
    List<Double> strikeCapletList = new ArrayList<>();
    List<Double> volCapletList = new ArrayList<>();
    int nTimes = capletExpiries.size();
    int nStrikes = strikes.size();
    for (int i = 0; i < nTimes; ++i) {
      double expiry = capletExpiries.get(i);
      double shift = shiftCurve.yValue(expiry);
      timeCapletList.addAll(DoubleArray.filled(nStrikes, expiry).toList());
      strikeCapletList.addAll(strikes.plus(shift).toList());
      volCapletList.addAll(DoubleArray.of(nStrikes, n -> capVolSurface.zValue(expiry, strikes.get(n) + shift)).toList()); // initial guess
    }
    return Triple.of(DoubleArray.copyOf(timeCapletList), DoubleArray.copyOf(strikeCapletList), DoubleArray.copyOf(volCapletList));
  }

  private Function<DoubleArray, DoubleArray> getPriceFunction(
      List<ResolvedIborCapFloorLeg> capList,
      RatesProvider ratesProvider,
      Function<Surface, IborCapletFloorletVolatilities> volatilitiesFunction,
      InterpolatedNodalSurface baseSurface) {

    int nCaps = capList.size();
    Function<DoubleArray, DoubleArray> priceFunction = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray capletVols) {
        IborCapletFloorletVolatilities newVols = volatilitiesFunction.apply(baseSurface.withZValues(capletVols));
        return DoubleArray.of(nCaps, n -> pricer.presentValue(capList.get(n), ratesProvider, newVols).getAmount());
      }
    };
    return priceFunction;
  }

  private Function<DoubleArray, DoubleMatrix> getJacobianFunction(
      List<ResolvedIborCapFloorLeg> capList,
      RatesProvider ratesProvider,
      Function<Surface, IborCapletFloorletVolatilities> volatilitiesFunction,
      InterpolatedNodalSurface baseSurface) {

    int nCaps = capList.size();
    int nNodes = baseSurface.getParameterCount();
    Function<DoubleArray, DoubleMatrix> jacobianFunction = new Function<DoubleArray, DoubleMatrix>() {
      @Override
      public DoubleMatrix apply(DoubleArray capletVols) {
        IborCapletFloorletVolatilities newVols = volatilitiesFunction.apply(baseSurface.withZValues(capletVols));
        return DoubleMatrix.ofArrayObjects(
            nCaps,
            nNodes,
            n -> newVols.parameterSensitivity(
                pricer.presentValueSensitivityModelParamsVolatility(capList.get(n), ratesProvider, newVols).build())
                .getSensitivities()
                .get(0)
                .getSensitivity());
      }
    };
    return jacobianFunction;
  }

}
