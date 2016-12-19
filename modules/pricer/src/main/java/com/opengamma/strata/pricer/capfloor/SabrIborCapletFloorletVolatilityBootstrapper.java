/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory.SV_COMMONS;
import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.OG_ALGEBRA;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.math.impl.minimization.DoubleRangeLimitTransform;
import com.opengamma.strata.math.impl.minimization.NonLinearTransformFunction;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform.LimitType;
import com.opengamma.strata.math.impl.minimization.SingleRangeLimitTransform;
import com.opengamma.strata.math.impl.minimization.UncoupledParameterTransforms;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResults;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResultsWithTransform;
import com.opengamma.strata.math.impl.statistics.leastsquare.NonLinearLeastSquare;
import com.opengamma.strata.pricer.model.SabrParameters;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Caplet volatilities calibration to cap volatilities based on SABR model.
 * <p>
 * The SABR model parameters are computed by bootstrapping along the expiry time dimension. 
 * The result is a complete set of curves for the SABR parameters spanned by the expiry time.  
 * The position of the node points on the resultant curves corresponds to market cap expiries, 
 * and are interpolated by a local interpolation scheme. 
 * See {@link SabrIborCapletFloorletVolatilityBootstrapDefinition} for detail.
 * <p>
 * The calibration to SABR is computed once the option volatility date is converted to prices. Thus we should note that 
 * the error values in {@code RawOptionData} are applied in the price space rather than the volatility space.
 */
public class SabrIborCapletFloorletVolatilityBootstrapper extends IborCapletFloorletVolatilityCalibrator {

  /**
   * Default implementation.
   */
  public static final SabrIborCapletFloorletVolatilityBootstrapper DEFAULT = of(
      VolatilityIborCapFloorLegPricer.DEFAULT, SabrIborCapletFloorletPeriodPricer.DEFAULT, 1.0e-10, ReferenceData.standard());

  /**
   * Transformation for SABR parameters.
   */
  private static final ParameterLimitsTransform[] TRANSFORMS;
  /**
   * SABR parameter range. 
   */
  private static final double RHO_LIMIT = 0.999;
  static {
    TRANSFORMS = new ParameterLimitsTransform[4];
    TRANSFORMS[0] = new SingleRangeLimitTransform(0, LimitType.GREATER_THAN); // alpha > 0
    TRANSFORMS[1] = new DoubleRangeLimitTransform(0.0, 1.0); // 0 <= beta <= 1
    TRANSFORMS[2] = new DoubleRangeLimitTransform(-RHO_LIMIT, RHO_LIMIT); // -1 <= rho <= 1
    TRANSFORMS[3] = new DoubleRangeLimitTransform(0.001d, 2.50d);
    // nu > 0  and limit on Nu to avoid numerical instability in formula for large nu.
  }

  /**
   * The nonlinear least square solver.
   */
  private final NonLinearLeastSquare solver;
  /**
   * SABR pricer for caplet/floorlet.
   */
  private final SabrIborCapletFloorletPeriodPricer sabrPeriodPricer;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance. 
   * <p>
   * The epsilon is the parameter used in {@link NonLinearLeastSquare}, where the iteration stops when certain 
   * quantities are smaller than this parameter.
   * 
   * @param pricer  the cap/floor pricer to convert quoted volatilities to prices
   * @param sabrPeriodPricer  the SABR pricer
   * @param epsilon  the epsilon parameter
   * @param referenceData  the reference data
   * @return the instance
   */
  public static SabrIborCapletFloorletVolatilityBootstrapper of(
      VolatilityIborCapFloorLegPricer pricer,
      SabrIborCapletFloorletPeriodPricer sabrPeriodPricer,
      double epsilon,
      ReferenceData referenceData) {

    NonLinearLeastSquare solver = new NonLinearLeastSquare(SV_COMMONS, OG_ALGEBRA, epsilon);
    return new SabrIborCapletFloorletVolatilityBootstrapper(pricer, sabrPeriodPricer, solver, referenceData);
  }

  // private constructor
  private SabrIborCapletFloorletVolatilityBootstrapper(
      VolatilityIborCapFloorLegPricer pricer,
      SabrIborCapletFloorletPeriodPricer sabrPeriodPricer,
      NonLinearLeastSquare solver,
      ReferenceData referenceData) {

    super(pricer, referenceData);
    this.sabrPeriodPricer = ArgChecker.notNull(sabrPeriodPricer, "sabrPeriodPricer");
    this.solver = ArgChecker.notNull(solver, "solver");
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
    ArgChecker.isTrue(definition instanceof SabrIborCapletFloorletVolatilityBootstrapDefinition,
        "definition should be SabrIborCapletFloorletVolatilityBootstrapDefinition");
    SabrIborCapletFloorletVolatilityBootstrapDefinition bsDefinition =
        (SabrIborCapletFloorletVolatilityBootstrapDefinition) definition;
    IborIndex index = bsDefinition.getIndex();
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate baseDate = index.getEffectiveDateOffset().adjust(calibrationDate, referenceData);
    LocalDate startDate = baseDate.plus(index.getTenor());
    Function<Surface, IborCapletFloorletVolatilities> volatilitiesFunction = volatilitiesFunction(
        bsDefinition, calibrationDateTime, capFloorData);
    SurfaceMetadata metaData = bsDefinition.createMetadata(capFloorData);
    List<Period> expiries = capFloorData.getExpiries();
    int nExpiries = expiries.size();
    DoubleArray strikes = capFloorData.getStrikes();
    DoubleMatrix errorsMatrix = capFloorData.getError().orElse(DoubleMatrix.filled(nExpiries, strikes.size(), 1d));
    List<Double> timeList = new ArrayList<>();
    List<Double> strikeList = new ArrayList<>();
    List<Double> volList = new ArrayList<>();
    List<ResolvedIborCapFloorLeg> capList = new ArrayList<>();
    List<Double> priceList = new ArrayList<>();
    List<Double> errorList = new ArrayList<>();
    int[] startIndex = new int[nExpiries + 1];
    for (int i = 0; i < nExpiries; ++i) {
      LocalDate endDate = baseDate.plus(expiries.get(i));
      DoubleArray volatilityData = capFloorData.getData().row(i);
      DoubleArray errors = errorsMatrix.row(i);
      reduceRawData(bsDefinition, ratesProvider, strikes, volatilityData, errors, startDate, endDate, metaData,
          volatilitiesFunction, timeList, strikeList, volList, capList, priceList, errorList);
      startIndex[i + 1] = volList.size();
      ArgChecker.isTrue(startIndex[i + 1] > startIndex[i], "no valid option data for {}", expiries.get(i));
    }

    List<CurveMetadata> metadataList = bsDefinition.createSabrParameterMetadata();
    DoubleArray timeToExpiries = DoubleArray.of(nExpiries, i -> timeList.get(startIndex[i]));

    BitSet fixed = new BitSet();
    Curve betaCurve;
    Curve rhoCurve;
    if (bsDefinition.getBetaCurve().isPresent()) {
      fixed.set(1);
      betaCurve = bsDefinition.getBetaCurve().get();
      rhoCurve = InterpolatedNodalCurve.of(
          metadataList.get(2),
          timeToExpiries,
          DoubleArray.filled(nExpiries),
          bsDefinition.getInterpolator(),
          bsDefinition.getExtrapolatorLeft(),
          bsDefinition.getExtrapolatorRight());
    } else {
      fixed.set(2);
      betaCurve = InterpolatedNodalCurve.of(
          metadataList.get(1),
          timeToExpiries,
          DoubleArray.filled(nExpiries),
          bsDefinition.getInterpolator(),
          bsDefinition.getExtrapolatorLeft(),
          bsDefinition.getExtrapolatorRight());
      rhoCurve = bsDefinition.getRhoCurve().get();
    }
    InterpolatedNodalCurve alphaCurve = InterpolatedNodalCurve.of(
        metadataList.get(0),
        timeToExpiries,
        DoubleArray.filled(nExpiries),
        bsDefinition.getInterpolator(),
        bsDefinition.getExtrapolatorLeft(),
        bsDefinition.getExtrapolatorRight());
    InterpolatedNodalCurve nuCurve = InterpolatedNodalCurve.of(
        metadataList.get(3),
        timeToExpiries,
        DoubleArray.filled(nExpiries),
        bsDefinition.getInterpolator(),
        bsDefinition.getExtrapolatorLeft(),
        bsDefinition.getExtrapolatorRight());
    Curve shiftCurve = bsDefinition.getShiftCurve();
    SabrParameters sabrParams = SabrParameters.of(
        alphaCurve, betaCurve, rhoCurve, nuCurve, shiftCurve, bsDefinition.getSabrVolatilityFormula());
    SabrParametersIborCapletFloorletVolatilities vols =
        SabrParametersIborCapletFloorletVolatilities.of(bsDefinition.getName(), index, calibrationDateTime, sabrParams);
    double totalChiSq = 0d;
    ZonedDateTime prevExpiry = calibrationDateTime.minusDays(1L); // included if calibrationDateTime == fixingDateTime
    for (int i = 0; i < nExpiries; ++i) {
      DoubleArray start = computeInitialValues(
          ratesProvider, betaCurve, shiftCurve, timeList, volList, capList, startIndex, i, fixed.get(1), capFloorData.getDataType());
      UncoupledParameterTransforms transform = new UncoupledParameterTransforms(start, TRANSFORMS, fixed);
      int nCaplets = startIndex[i + 1] - startIndex[i];
      int currentStart = startIndex[i];
      Function<DoubleArray, DoubleArray> valueFunction = createPriceFunction(
          ratesProvider, vols, prevExpiry, capList, priceList, startIndex, nExpiries, i, nCaplets, fixed.get(1));
      Function<DoubleArray, DoubleMatrix> jacobianFunction = createJacobianFunction(
          ratesProvider, vols, prevExpiry, capList, priceList, index.getCurrency(), startIndex, nExpiries, i, nCaplets, fixed.get(1));
      NonLinearTransformFunction transFunc = new NonLinearTransformFunction(valueFunction, jacobianFunction, transform);
      DoubleArray adjustedPrices = adjustedPrices(ratesProvider, vols, prevExpiry, capList, priceList, startIndex, i, nCaplets);
      DoubleArray errors = DoubleArray.of(nCaplets, n -> errorList.get(currentStart + n));
      LeastSquareResults res = solver.solve(adjustedPrices, errors, transFunc.getFittingFunction(),
          transFunc.getFittingJacobian(), transform.transform(start));
      LeastSquareResultsWithTransform resTransform = new LeastSquareResultsWithTransform(res, transform);
      vols = updateParameters(vols, nExpiries, i, fixed.get(1), resTransform.getModelParameters());
      totalChiSq += res.getChiSq();
      prevExpiry = capList.get(startIndex[i + 1] - 1).getFinalFixingDateTime();
    }
    return IborCapletFloorletVolatilityCalibrationResult.ofLeastSquare(vols, totalChiSq);
  }

  //-------------------------------------------------------------------------
  // computes initial guess for each time step
  private DoubleArray computeInitialValues(
      RatesProvider ratesProvider,
      Curve betaCurve,
      Curve shiftCurve,
      List<Double> timeList,
      List<Double> volList,
      List<ResolvedIborCapFloorLeg> capList,
      int[] startIndex,
      int postion,
      boolean betaFixed,
      ValueType valueType) {

    List<Double> vols = volList.subList(startIndex[postion], startIndex[postion + 1]);
    ResolvedIborCapFloorLeg cap = capList.get(startIndex[postion]);
    double fwd = ratesProvider.iborIndexRates(cap.getIndex()).rate(cap.getFinalPeriod().getIborRate().getObservation());
    double shift = shiftCurve.yValue(timeList.get(startIndex[postion]));
    double factor = valueType.equals(ValueType.BLACK_VOLATILITY) ? 1d : 1d / (fwd + shift);
    List<Double> volsEquiv = vols.stream().map(v -> v * factor).collect(Collectors.toList());
    double nuFirst;
    double betaInitial = betaFixed ? betaCurve.yValue(timeList.get(startIndex[postion])) : 0.5d;
    double alphaInitial = DoubleArray.copyOf(volsEquiv).min() * Math.pow(fwd, 1d - betaInitial);
    if (alphaInitial == volsEquiv.get(0) || alphaInitial == volsEquiv.get(volsEquiv.size() - 1)) {
      nuFirst = 0.1d;
      alphaInitial *= 0.95d;
    } else {
      nuFirst = 1d;
    }
    return DoubleArray.of(alphaInitial, betaInitial, -0.5 * betaInitial + 0.5 * (1d - betaInitial), nuFirst);
  }

  // price function
  private Function<DoubleArray, DoubleArray> createPriceFunction(
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities,
      ZonedDateTime prevExpiry,
      List<ResolvedIborCapFloorLeg> capList,
      List<Double> priceList,
      int[] startIndex,
      int nExpiries,
      int timeIndex,
      int nCaplets,
      boolean betaFixed) {

    int currentStart = startIndex[timeIndex];
    Function<DoubleArray, DoubleArray> priceFunction = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray x) {
        SabrParametersIborCapletFloorletVolatilities volsNew = updateParameters(volatilities, nExpiries, timeIndex, betaFixed, x);
        return DoubleArray.of(nCaplets,
            n -> capList.get(currentStart + n).getCapletFloorletPeriods().stream()
                .filter(p -> p.getFixingDateTime().isAfter(prevExpiry))
                .mapToDouble(p -> sabrPeriodPricer.presentValue(p, ratesProvider, volsNew).getAmount())
                .sum() / priceList.get(currentStart + n));
      }
    };
    return priceFunction;
  }

  // node sensitivity function
  private Function<DoubleArray, DoubleMatrix> createJacobianFunction(
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities,
      ZonedDateTime prevExpiry,
      List<ResolvedIborCapFloorLeg> capList,
      List<Double> priceList,
      Currency currency,
      int[] startIndex,
      int nExpiries,
      int timeIndex,
      int nCaplets,
      boolean betaFixed) {

    Curve alphaCurve = volatilities.getParameters().getAlphaCurve();
    Curve betaCurve = volatilities.getParameters().getBetaCurve();
    Curve rhoCurve = volatilities.getParameters().getRhoCurve();
    Curve nuCurve = volatilities.getParameters().getNuCurve();
    int currentStart = startIndex[timeIndex];
    Function<DoubleArray, DoubleMatrix> jacobianFunction = new Function<DoubleArray, DoubleMatrix>() {
      @Override
      public DoubleMatrix apply(DoubleArray x) {
        SabrParametersIborCapletFloorletVolatilities volsNew = updateParameters(volatilities, nExpiries, timeIndex, betaFixed, x);
        double[][] jacobian = new double[nCaplets][4];
        for (int i = 0; i < nCaplets; ++i) {
          PointSensitivities point = capList.get(currentStart + i).getCapletFloorletPeriods().stream()
              .filter(p -> p.getFixingDateTime().isAfter(prevExpiry))
              .map(p -> sabrPeriodPricer.presentValueSensitivityModelParamsSabr(p, ratesProvider, volsNew))
              .reduce((c1, c2) -> c1.combinedWith(c2))
              .get()
              .build();
          double targetPrice = priceList.get(currentStart + i);
          CurrencyParameterSensitivities sensi = volsNew.parameterSensitivity(point);
          jacobian[i][0] = sensi.getSensitivity(alphaCurve.getName(), currency).getSensitivity().get(timeIndex) / targetPrice;
          if (betaFixed) {
            jacobian[i][1] = 0d;
            jacobian[i][2] = sensi.getSensitivity(rhoCurve.getName(), currency).getSensitivity().get(timeIndex) / targetPrice;

          } else {
            jacobian[i][1] = sensi.getSensitivity(betaCurve.getName(), currency).getSensitivity().get(timeIndex) / targetPrice;
            jacobian[i][2] = 0d;

          }
          jacobian[i][3] = sensi.getSensitivity(nuCurve.getName(), currency).getSensitivity().get(timeIndex) / targetPrice;
        }
        return DoubleMatrix.ofUnsafe(jacobian);
      }
    };
    return jacobianFunction;
  }

  // update vols
  private SabrParametersIborCapletFloorletVolatilities updateParameters(
      SabrParametersIborCapletFloorletVolatilities volatilities,
      int nExpiries,
      int timeIndex,
      boolean betaFixed,
      DoubleArray newParameters) {

    int nBetaParams = volatilities.getParameters().getBetaCurve().getParameterCount();
    int nRhoParams = volatilities.getParameters().getRhoCurve().getParameterCount();
    SabrParametersIborCapletFloorletVolatilities newVols = volatilities
        .withParameter(timeIndex, newParameters.get(0))
        .withParameter(timeIndex + nExpiries + nBetaParams + nRhoParams, newParameters.get(3));
    if (betaFixed) {
      newVols = newVols.withParameter(timeIndex + nExpiries + nBetaParams, newParameters.get(2));
      return newVols;
    }
    newVols = newVols.withParameter(timeIndex + nExpiries, newParameters.get(1));
    return newVols;
  }

  // sum of caplet prices which are not fixed
  private DoubleArray adjustedPrices(
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities vols,
      ZonedDateTime prevExpiry,
      List<ResolvedIborCapFloorLeg> capList,
      List<Double> priceList,
      int[] startIndex,
      int timeIndex,
      int nCaplets) {

    if (timeIndex == 0) {
      return DoubleArray.filled(nCaplets, 1d);
    }
    int currentStart = startIndex[timeIndex];
    return DoubleArray.of(nCaplets,
        n -> (priceList.get(currentStart + n) - capList.get(currentStart + n).getCapletFloorletPeriods().stream()
            .filter(p -> !p.getFixingDateTime().isAfter(prevExpiry))
            .mapToDouble(p -> sabrPeriodPricer.presentValue(p, ratesProvider, vols).getAmount())
            .sum()) / priceList.get(currentStart + n));
  }

}
