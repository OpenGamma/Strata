/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
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
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
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
 * and calibrated to the market data under the penalty constraint.
 * The penalty is based on the second-order finite difference differentiation along the expiry dimension.
 * <p>
 * The resultant volatility type is the same as the input volatility type. e.g., 
 * Black caplet volatilities {@code BlackIborCapletFloorletExpiryFlatVolatilities} are returned 
 * if Black cap volatilities are plugged in, 
 * and normal caplet volatilities {@code NormalIborCapletFloorletExpiryFlatVolatilities} are returned otherwise. 
 * <p>
 * The calibration is conducted once the cap volatilities are converted to cap prices. 
 * Thus the error values in {@code RawOptionData} are applied in the price space rather than the volatility space.
 */
public class DirectIborCapletFloorletFlatVolatilityCalibrator extends IborCapletFloorletVolatilityCalibrator {

  /**
   * Standard implementation. 
   */
  private static final DirectIborCapletFloorletFlatVolatilityCalibrator STANDARD =
      of(VolatilityIborCapFloorLegPricer.DEFAULT, 1.0e-8, ReferenceData.standard());

  /**
   * The positive function.
   * <p>
   * The function returns true if the new trial position is positive or zero.
   */
  private static final Function<DoubleArray, Boolean> POSITIVE = new PositiveOrZero();
  /**
   * The non-linear least square with penalty. 
   */
  private final NonLinearLeastSquareWithPenalty solver;

  //-------------------------------------------------------------------------
  /** 
   * Obtains the standard instance. 
   * 
   * @return the instance
   */
  public static DirectIborCapletFloorletFlatVolatilityCalibrator standard() {
    return DirectIborCapletFloorletFlatVolatilityCalibrator.STANDARD;
  }

  /**
   * Obtains an instance. 
   * <p>
   * The epsilon is the parameter used in {@link NonLinearLeastSquareWithPenalty},
   * where the iteration stops when certain quantities are smaller than this parameter.
   * 
   * @param pricer  the cap pricer
   * @param epsilon  the epsilon parameter
   * @param referenceData  the reference data
   * @return the instance
   */
  public static DirectIborCapletFloorletFlatVolatilityCalibrator of(
      VolatilityIborCapFloorLegPricer pricer,
      double epsilon,
      ReferenceData referenceData) {

    return new DirectIborCapletFloorletFlatVolatilityCalibrator(pricer, epsilon, referenceData);
  }

  // private constructor
  private DirectIborCapletFloorletFlatVolatilityCalibrator(
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
    ArgChecker.isTrue(definition instanceof DirectIborCapletFloorletFlatVolatilityDefinition,
        "definition should be DirectIborCapletFloorletFlatVolatilityDefinition");
    DirectIborCapletFloorletFlatVolatilityDefinition directDefinition =
        (DirectIborCapletFloorletFlatVolatilityDefinition) definition;
    DoubleArray strikes = capFloorData.getStrikes();
    ArgChecker.isTrue(strikes.size() == 1, "strike size should be 1");
    // unpack cap data, create node caps
    IborIndex index = directDefinition.getIndex();
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate baseDate = index.getEffectiveDateOffset().adjust(calibrationDate, getReferenceData());
    LocalDate startDate = baseDate.plus(index.getTenor());
    Function<Curve, IborCapletFloorletVolatilities> volatilitiesFunction = flatVolatilitiesFunction(
        directDefinition, calibrationDateTime, capFloorData);
    CurveMetadata metadata = directDefinition.createCurveMetadata(capFloorData);
    List<Period> expiries = capFloorData.getExpiries();
    int nExpiries = expiries.size();
    List<Double> timeList = new ArrayList<>();
    List<Double> volList = new ArrayList<>();
    List<ResolvedIborCapFloorLeg> capList = new ArrayList<>();
    List<Double> priceList = new ArrayList<>();
    List<Double> errorList = new ArrayList<>();
    DoubleMatrix errorMatrix = capFloorData.getError().orElse(DoubleMatrix.filled(nExpiries, 1, 1d));
    int[] startIndex = new int[nExpiries + 1];
    for (int i = 0; i < nExpiries; ++i) {
      LocalDate endDate = baseDate.plus(expiries.get(i));
      double strike = strikes.get(0);
      double volatilityForTime = capFloorData.getData().row(i).get(0);
      double errorForTime = errorMatrix.row(i).get(0);
      reduceRawData(directDefinition, ratesProvider, strike, volatilityForTime, errorForTime, startDate,
          endDate, metadata, volatilitiesFunction, timeList, volList, capList, priceList, errorList);
      startIndex[i + 1] = volList.size();
      ArgChecker.isTrue(startIndex[i + 1] > startIndex[i], "no valid option data for {}", expiries.get(i));
    }
    // create caplet nodes and initial caplet vol curve
    ResolvedIborCapFloorLeg cap = capList.get(capList.size() - 1);
    int nCaplets = cap.getCapletFloorletPeriods().size();
    DoubleArray capletExpiries = DoubleArray.of(nCaplets, n -> directDefinition.getDayCount().relativeYearFraction(
        calibrationDate, cap.getCapletFloorletPeriods().get(n).getFixingDateTime().toLocalDate()));
    Pair<DoubleArray, DoubleArray> capletNodes;
    DoubleArray initialVols = DoubleArray.copyOf(volList);
    InterpolatedNodalCurve capVolCurve = InterpolatedNodalCurve.of(
        metadata, DoubleArray.copyOf(timeList), initialVols, CurveInterpolators.LINEAR);
    capletNodes = createCapletNodes(capVolCurve, capletExpiries);
    InterpolatedNodalCurve baseCurve = InterpolatedNodalCurve.of(
        metadata, capletNodes.getFirst(), capletNodes.getSecond(), CurveInterpolators.LINEAR);
    DoubleMatrix penaltyMatrix = directDefinition.computePenaltyMatrix(capletExpiries);
    // solve least square
    LeastSquareResults res = solver.solve(
        DoubleArray.copyOf(priceList),
        DoubleArray.copyOf(errorList),
        getPriceFunction(capList, ratesProvider, volatilitiesFunction, baseCurve),
        getJacobianFunction(capList, ratesProvider, volatilitiesFunction, baseCurve),
        capletNodes.getSecond(),
        penaltyMatrix,
        POSITIVE);
    InterpolatedNodalCurve resCurve = InterpolatedNodalCurve.of(
        metadata,
        capletNodes.getFirst(),
        res.getFitParameters(),
        directDefinition.getInterpolator(),
        directDefinition.getExtrapolatorLeft(),
        directDefinition.getExtrapolatorRight());
    IborCapletFloorletVolatilityCalibrationResult calibrationResult = IborCapletFloorletVolatilityCalibrationResult.ofLeastSquare(
        volatilitiesFunction.apply(resCurve), res.getChiSq());
    return calibrationResult;
  }

  //-------------------------------------------------------------------------
  private Pair<DoubleArray, DoubleArray> createCapletNodes(
      InterpolatedNodalCurve capVolCurve, DoubleArray capletExpiries) {

    List<Double> timeCapletList = new ArrayList<>();
    List<Double> volCapletList = new ArrayList<>();
    int nTimes = capletExpiries.size();
    for (int i = 0; i < nTimes; ++i) {
      double expiry = capletExpiries.get(i);
      timeCapletList.add(expiry);
      volCapletList.add(capVolCurve.yValue(expiry)); // initial guess
    }
    return Pair.of(DoubleArray.copyOf(timeCapletList), DoubleArray.copyOf(volCapletList));
  }

  private Function<DoubleArray, DoubleArray> getPriceFunction(
      List<ResolvedIborCapFloorLeg> capList,
      RatesProvider ratesProvider,
      Function<Curve, IborCapletFloorletVolatilities> volatilitiesFunction,
      InterpolatedNodalCurve baseCurve) {

    int nCaps = capList.size();
    Function<DoubleArray, DoubleArray> priceFunction = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray capletVols) {
        IborCapletFloorletVolatilities newVols = volatilitiesFunction.apply(baseCurve.withYValues(capletVols));
        return DoubleArray.of(nCaps, n -> getLegPricer().presentValue(capList.get(n), ratesProvider, newVols).getAmount());
      }
    };
    return priceFunction;
  }

  private Function<DoubleArray, DoubleMatrix> getJacobianFunction(
      List<ResolvedIborCapFloorLeg> capList,
      RatesProvider ratesProvider,
      Function<Curve, IborCapletFloorletVolatilities> volatilitiesFunction,
      InterpolatedNodalCurve baseCurve) {

    int nCaps = capList.size();
    int nNodes = baseCurve.getParameterCount();
    Function<DoubleArray, DoubleMatrix> jacobianFunction = new Function<DoubleArray, DoubleMatrix>() {
      @Override
      public DoubleMatrix apply(DoubleArray capletVols) {
        IborCapletFloorletVolatilities newVols = volatilitiesFunction.apply(baseCurve.withYValues(capletVols));
        return DoubleMatrix.ofArrayObjects(
            nCaps,
            nNodes,
            n -> newVols.parameterSensitivity(
                getLegPricer().presentValueSensitivityModelParamsVolatility(capList.get(n), ratesProvider, newVols).build())
                .getSensitivities()
                .get(0)
                .getSensitivity());
      }
    };
    return jacobianFunction;
  }

  private void reduceRawData(
      IborCapletFloorletVolatilityDefinition definition,
      RatesProvider ratesProvider,
      double strike,
      double volatility,
      double error,
      LocalDate startDate,
      LocalDate endDate,
      CurveMetadata metadata,
      Function<Curve, IborCapletFloorletVolatilities> volatilityFunction,
      List<Double> timeList,
      List<Double> volList,
      List<ResolvedIborCapFloorLeg> capList,
      List<Double> priceList,
      List<Double> errorList) {

    ResolvedIborCapFloorLeg capFloor = definition.createCap(startDate, endDate, strike).resolve(getReferenceData());
    capList.add(capFloor);
    volList.add(volatility);
    ConstantCurve constVolCurve = ConstantCurve.of(metadata, volatility);
    IborCapletFloorletVolatilities vols = volatilityFunction.apply(constVolCurve);
    timeList.add(vols.relativeTime(capFloor.getFinalFixingDateTime()));
    priceList.add(getLegPricer().presentValue(capFloor, ratesProvider, vols).getAmount());
    errorList.add(error);
  }

  // function creating volatilities object from curve
  private Function<Curve, IborCapletFloorletVolatilities> flatVolatilitiesFunction(
      IborCapletFloorletVolatilityDefinition definition,
      ZonedDateTime calibrationDateTime,
      RawOptionData capFloorData) {

    IborIndex index = definition.getIndex();
    if (capFloorData.getDataType().equals(BLACK_VOLATILITY)) {
      return blackVolatilitiesFunction(index, calibrationDateTime);
    } else if (capFloorData.getDataType().equals(NORMAL_VOLATILITY)) {
      return normalVolatilitiesFunction(index, calibrationDateTime);
    }
    throw new IllegalArgumentException("Data type not supported");
  }

  private Function<Curve, IborCapletFloorletVolatilities> blackVolatilitiesFunction(
      IborIndex index,
      ZonedDateTime calibrationDateTime) {

    Function<Curve, IborCapletFloorletVolatilities> func = new Function<Curve, IborCapletFloorletVolatilities>() {
      @Override
      public IborCapletFloorletVolatilities apply(Curve s) {
        return BlackIborCapletFloorletExpiryFlatVolatilities.of(index, calibrationDateTime, s);
      }
    };
    return func;
  }

  private Function<Curve, IborCapletFloorletVolatilities> normalVolatilitiesFunction(
      IborIndex index,
      ZonedDateTime calibrationDateTime) {

    Function<Curve, IborCapletFloorletVolatilities> func = new Function<Curve, IborCapletFloorletVolatilities>() {
      @Override
      public IborCapletFloorletVolatilities apply(Curve s) {
        return NormalIborCapletFloorletExpiryFlatVolatilities.of(index, calibrationDateTime, s);
      }
    };
    return func;
  }

}
