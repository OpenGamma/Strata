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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
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
 * The SABR parameters are represented by {@code NodalCurve} and the node positions on the curves are flexible.
 * The resulting volatilities object will be {@link SabrParametersIborCapletFloorletVolatilities}.
 * <p>
 * The calibration to SABR is computed once the option volatility date is converted to prices. 
 * Thus the error values in {@code RawOptionData} are applied in the price space rather than the volatility space.
 */
public class SabrIborCapletFloorletVolatilityCalibrator
    extends IborCapletFloorletVolatilityCalibrator {

  /**
   * Default implementation.
   */
  public static final SabrIborCapletFloorletVolatilityCalibrator DEFAULT = of(
      VolatilityIborCapFloorLegPricer.DEFAULT, SabrIborCapFloorLegPricer.DEFAULT, 1.0e-10, ReferenceData.standard());

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
    TRANSFORMS[0] = new SingleRangeLimitTransform(0.0, LimitType.GREATER_THAN); // alpha > 0
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
   * SABR pricer for cap/floor leg.
   */
  private final SabrIborCapFloorLegPricer sabrPricer;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * <p>
   * The epsilon is the parameter used in {@link NonLinearLeastSquare}, where the iteration stops when certain 
   * quantities are smaller than this parameter.
   * 
   * @param pricer  the cap pricer
   * @param sabrPricer  the SABR cap pricer
   * @param epsilon  the epsilon parameter
   * @param referenceData  the reference data
   * @return the instance
   */
  public static SabrIborCapletFloorletVolatilityCalibrator of(
      VolatilityIborCapFloorLegPricer pricer,
      SabrIborCapFloorLegPricer sabrPricer,
      double epsilon,
      ReferenceData referenceData) {

    NonLinearLeastSquare solver = new NonLinearLeastSquare(SV_COMMONS, OG_ALGEBRA, epsilon);
    return new SabrIborCapletFloorletVolatilityCalibrator(pricer, sabrPricer, solver, referenceData);
  }

  // private constructor
  private SabrIborCapletFloorletVolatilityCalibrator(
      VolatilityIborCapFloorLegPricer pricer,
      SabrIborCapFloorLegPricer sabrPricer,
      NonLinearLeastSquare solver,
      ReferenceData referenceData) {

    super(pricer, referenceData);
    this.sabrPricer = ArgChecker.notNull(sabrPricer, "sabrPricer");
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
    ArgChecker.isTrue(definition instanceof SabrIborCapletFloorletVolatilityCalibrationDefinition,
        "definition should be SabrIborCapletFloorletVolatilityCalibrationDefinition");
    SabrIborCapletFloorletVolatilityCalibrationDefinition sabrDefinition =
        (SabrIborCapletFloorletVolatilityCalibrationDefinition) definition;
    // unpack cap data, create node caps
    IborIndex index = sabrDefinition.getIndex();
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate baseDate = index.getEffectiveDateOffset().adjust(calibrationDate, referenceData);
    LocalDate startDate = baseDate.plus(index.getTenor());
    Function<Surface, IborCapletFloorletVolatilities> volatilitiesFunction = volatilitiesFunction(
        sabrDefinition, calibrationDateTime, capFloorData);
    SurfaceMetadata metadata = sabrDefinition.createMetadata(capFloorData);
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
      reduceRawData(sabrDefinition, ratesProvider, capFloorData.getStrikes(), volatilityForTime, errorForTime, startDate,
          endDate, metadata, volatilitiesFunction, timeList, strikeList, volList, capList, priceList, errorList);
      startIndex[i + 1] = volList.size();
      ArgChecker.isTrue(startIndex[i + 1] > startIndex[i], "no valid option data for {}", expiries.get(i));
    }
    // create initial caplet vol surface
    List<CurveMetadata> metadataList = sabrDefinition.createSabrParameterMetadata();
    DoubleArray initialValues = sabrDefinition.createFullInitialValues();
    List<Curve> curveList = sabrDefinition.createSabrParameterCurve(metadataList, initialValues);
    SabrParameters sabrParamsInitial = SabrParameters.of(
        curveList.get(0),
        curveList.get(1),
        curveList.get(2),
        curveList.get(3),
        sabrDefinition.getShiftCurve(),
        sabrDefinition.getSabrVolatilityFormula());
    SabrParametersIborCapletFloorletVolatilities vols = SabrParametersIborCapletFloorletVolatilities.of(
        sabrDefinition.getName(), index, calibrationDateTime, sabrParamsInitial);
    // solve least square
    UncoupledParameterTransforms transform = new UncoupledParameterTransforms(
        initialValues, sabrDefinition.createFullTransform(TRANSFORMS), new BitSet());
    Function<DoubleArray, DoubleArray> valueFunction = createPriceFunction(
        sabrDefinition, ratesProvider, vols, capList, priceList);
    Function<DoubleArray, DoubleMatrix> jacobianFunction = createJacobianFunction(
        sabrDefinition, ratesProvider, vols, capList, priceList, index.getCurrency());
    NonLinearTransformFunction transFunc = new NonLinearTransformFunction(valueFunction, jacobianFunction, transform);
    LeastSquareResults res = solver.solve(
        DoubleArray.filled(priceList.size(), 1d),
        DoubleArray.copyOf(errorList),
        transFunc.getFittingFunction(),
        transFunc.getFittingJacobian(),
        transform.transform(initialValues));
    LeastSquareResultsWithTransform resTransform = new LeastSquareResultsWithTransform(res, transform);
    vols = updateParameters(sabrDefinition, vols, resTransform.getModelParameters());

    return IborCapletFloorletVolatilityCalibrationResult.ofLeastSquare(vols, res.getChiSq());
  }

  // price function
  private Function<DoubleArray, DoubleArray> createPriceFunction(
      SabrIborCapletFloorletVolatilityCalibrationDefinition sabrDefinition,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities,
      List<ResolvedIborCapFloorLeg> capList,
      List<Double> priceList) {

    Function<DoubleArray, DoubleArray> priceFunction = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray x) {
        SabrParametersIborCapletFloorletVolatilities volsNew = updateParameters(sabrDefinition, volatilities, x);
        return DoubleArray.of(capList.size(),
            n -> sabrPricer.presentValue(capList.get(n), ratesProvider, volsNew).getAmount() / priceList.get(n));
      }
    };
    return priceFunction;
  }

  // node sensitivity function
  private Function<DoubleArray, DoubleMatrix> createJacobianFunction(
      SabrIborCapletFloorletVolatilityCalibrationDefinition sabrDefinition,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities,
      List<ResolvedIborCapFloorLeg> capList,
      List<Double> priceList,
      Currency currency) {

    int nCaps = capList.size();
    SabrParameters sabrParams = volatilities.getParameters();
    CurveName alphaName = sabrParams.getAlphaCurve().getName();
    CurveName betaName = sabrParams.getBetaCurve().getName();
    CurveName rhoName = sabrParams.getRhoCurve().getName();
    CurveName nuName = sabrParams.getNuCurve().getName();
    Function<DoubleArray, DoubleMatrix> jacobianFunction = new Function<DoubleArray, DoubleMatrix>() {
      @Override
      public DoubleMatrix apply(DoubleArray x) {
        SabrParametersIborCapletFloorletVolatilities volsNew = updateParameters(sabrDefinition, volatilities, x);
        double[][] jacobian = new double[nCaps][];
        for (int i = 0; i < nCaps; ++i) {
          PointSensitivities point =
              sabrPricer.presentValueSensitivityModelParamsSabr(capList.get(i), ratesProvider, volsNew).build();
          CurrencyParameterSensitivities sensi = volsNew.parameterSensitivity(point);
          double targetPriceInv = 1d / priceList.get(i);
          DoubleArray sensitivities = sensi.getSensitivity(alphaName, currency).getSensitivity();
          if (sabrDefinition.getBetaCurve().isPresent()) { // beta fixed
            sensitivities = sensitivities.concat(sensi.getSensitivity(rhoName, currency).getSensitivity());
          } else { // rho fixed
            sensitivities = sensitivities.concat(sensi.getSensitivity(betaName, currency).getSensitivity());
          }
          jacobian[i] = sensitivities.concat(sensi.getSensitivity(nuName, currency).getSensitivity())
              .multipliedBy(targetPriceInv)
              .toArray();
        }
        return DoubleMatrix.ofUnsafe(jacobian);
      }
    };
    return jacobianFunction;
  }

  // update vols
  private SabrParametersIborCapletFloorletVolatilities updateParameters(
      SabrIborCapletFloorletVolatilityCalibrationDefinition sabrDefinition,
      SabrParametersIborCapletFloorletVolatilities volatilities,
      DoubleArray newValues) {

    SabrParameters sabrParams = volatilities.getParameters();
    CurveMetadata alphaMetadata = sabrParams.getAlphaCurve().getMetadata();
    CurveMetadata betaMetadata = sabrParams.getBetaCurve().getMetadata();
    CurveMetadata rhoMetadata = sabrParams.getRhoCurve().getMetadata();
    CurveMetadata nuMetadata = sabrParams.getNuCurve().getMetadata();
    List<Curve> newCurveList = sabrDefinition.createSabrParameterCurve(
        ImmutableList.of(alphaMetadata, betaMetadata, rhoMetadata, nuMetadata), newValues);
    SabrParameters newSabrParams = SabrParameters.of(
        newCurveList.get(0),
        newCurveList.get(1),
        newCurveList.get(2),
        newCurveList.get(3),
        sabrDefinition.getShiftCurve(),
        sabrDefinition.getSabrVolatilityFormula());
    SabrParametersIborCapletFloorletVolatilities newVols = SabrParametersIborCapletFloorletVolatilities.of(
        volatilities.getName(), volatilities.getIndex(), volatilities.getValuationDateTime(), newSabrParams);
    return newVols;
  }

}
