/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import java.util.HashMap;
import java.util.Map;

import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.Maps;
import com.opengamma.analytics.financial.instrument.swap.SwapDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitorAdapter;
import com.opengamma.analytics.financial.provider.calculator.discounting.PV01CurveParametersCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParRateDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueCurveSensitivityDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.MarketQuoteSensitivityBlockCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.parameter.ParameterSensitivityParameterCalculator;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.DoubleLabelledMatrix1D;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.InterestRateSwapSecurityConverter;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.financial.analytics.model.multicurve.MultiCurveUtils;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.financial.security.irs.PayReceiveType;
import com.opengamma.financial.analytics.model.fixedincome.CashFlowDetailsCalculator;
import com.opengamma.financial.analytics.model.fixedincome.CashFlowDetailsProvider;
import com.opengamma.financial.analytics.model.fixedincome.SwapLegCashFlows;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Calculator for Discounting swaps.
 */
public class DiscountingInterestRateSwapCalculator implements InterestRateSwapCalculator {

  /**
   * Calculator for cash flows.
   */
  private static final CashFlowDetailsCalculator CFDC = new CashFlowDetailsCalculator();

  /**
   * Calculator for present value.
   */
  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();

  /**
   * Calculator for par rate.
   */
  private static final ParRateDiscountingCalculator PRDC = ParRateDiscountingCalculator.getInstance();

  /**
   * Calculator for PV01
   */
  private static final PV01CurveParametersCalculator<MulticurveProviderInterface> PV01C =
      new PV01CurveParametersCalculator<>(PresentValueCurveSensitivityDiscountingCalculator.getInstance());

  /** The curve sensitivity calculator */
  private static final InstrumentDerivativeVisitor<MulticurveProviderInterface, MultipleCurrencyMulticurveSensitivity> PVCSDC =
      PresentValueCurveSensitivityDiscountingCalculator.getInstance();
  /** The parameter sensitivity calculator */
  private static final ParameterSensitivityParameterCalculator<MulticurveProviderInterface> PSC =
      new ParameterSensitivityParameterCalculator<>(PVCSDC);
  /** The market quote sensitivity calculator */
  private static final MarketQuoteSensitivityBlockCalculator<MulticurveProviderInterface> BUCKETED_PV01_CALCULATOR =
      new MarketQuoteSensitivityBlockCalculator<>(PSC);

  /**
   * Provides scaling to/from basis points.
   */
  private static final double BASIS_POINT_FACTOR = 1.0E-4;

  /**
   * Derivative form of the security.
   */
  private final InstrumentDerivative _derivative;

  /**
   * The multicurve bundle.
   */
  private final MulticurveProviderInterface _bundle;

  /**
   * The curve building block bundle.
   */
  private final CurveBuildingBlockBundle _curveBuildingBlockBundle;

  /**
   * The swap definition.
   */
  private final SwapDefinition _definition;

  /**
   * The swap security.
   */
  private final InterestRateSwapSecurity _security;

  /**
   * The ZonedDateTime valuation time.
   */
  private final ZonedDateTime _valuationTime;

  /**
   * The curve definitions
   */
  private final Map<String, CurveDefinition> _curveDefinitions;

  /**
   * Creates a calculator for a InterestRateSwapSecurity.
   *
   * @param security the swap to calculate values for
   * @param bundle the multicurve bundle, including the curves
   * @param curveBuildingBlockBundle the curve block building bundle
   * @param swapConverter the InterestRateSwapSecurityConverter
   * @param valuationTime the ZonedDateTime
   * @param definitionConverter the FixedIncomeConverterDataProvider
   * @param fixings the HistoricalTimeSeriesBundle, a collection of historical time-series objects
   * @param curveDefinitions the curve definitions
   */
  public DiscountingInterestRateSwapCalculator(InterestRateSwapSecurity security,
                                               MulticurveProviderInterface bundle,
                                               CurveBuildingBlockBundle curveBuildingBlockBundle,
                                               InterestRateSwapSecurityConverter swapConverter,
                                               ZonedDateTime valuationTime,
                                               FixedIncomeConverterDataProvider definitionConverter,
                                               HistoricalTimeSeriesBundle fixings,
                                               Map<String, CurveDefinition> curveDefinitions) {
    ArgumentChecker.notNull(security, "security");
    ArgumentChecker.notNull(swapConverter, "swapConverter");
    ArgumentChecker.notNull(valuationTime, "valuationTime");
    ArgumentChecker.notNull(definitionConverter, "definitionConverter");
    ArgumentChecker.notNull(fixings, "fixings");
    _definition = (SwapDefinition) security.accept(swapConverter);
    _derivative = createInstrumentDerivative(security, valuationTime, definitionConverter, fixings);
    _bundle = ArgumentChecker.notNull(bundle, "bundle");
    _curveBuildingBlockBundle = ArgumentChecker.notNull(curveBuildingBlockBundle, "curveBuildingBlockBundle");
    _valuationTime = valuationTime;
    _security = security;
    _curveDefinitions = ArgumentChecker.notNull(curveDefinitions, "curveDefinitions");
    ArgumentChecker.isTrue(curveDefinitions.size() == curveBuildingBlockBundle.getData().size(),
                           "Require same number of curves & definitions");
    for (String curveName : curveBuildingBlockBundle.getData().keySet()) {
      ArgumentChecker.isTrue(curveDefinitions.containsKey(curveName), "curve definition not present {}", curveName);
    }
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(calculateResult(PVDC));
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePv(MulticurveProviderInterface bundle) {
    ArgumentChecker.notNull(bundle, "curve bundle");
    return Result.success(_derivative.accept(PVDC, bundle));
  }

  @Override
  public Result<Double> calculateRate() {
    return Result.success(calculateResult(PRDC));
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01() {
    return Result.success(calculateResult(PV01C));
  }

  @Override
  public Result<BucketedCurveSensitivities> calculateBucketedPV01() {
    MultipleCurrencyParameterSensitivity sensitivity = BUCKETED_PV01_CALCULATOR
        .fromInstrument(_derivative, _bundle, _curveBuildingBlockBundle)
        .multipliedBy(BASIS_POINT_FACTOR);
    Map<Pair<String, Currency>, DoubleLabelledMatrix1D> labelledMatrix1DMap = new HashMap<>();
    for (Map.Entry<Pair<String, Currency>, DoubleMatrix1D> entry : sensitivity.getSensitivities().entrySet()) {
      CurveDefinition curveDefinition = _curveDefinitions.get(entry.getKey().getFirst());
      DoubleLabelledMatrix1D matrix = MultiCurveUtils.getLabelledMatrix(entry.getValue(), curveDefinition);
      labelledMatrix1DMap.put(entry.getKey(), matrix);
    }
    return Result.success(BucketedCurveSensitivities.of(labelledMatrix1DMap));
  }

  @Override
  public Result<SwapLegCashFlows> calculatePayLegCashFlows() {
    return Result.success(_derivative.accept(CFDC, createCashFlowDetailsProvider(PayReceiveType.PAY)));
  }

  @Override
  public Result<SwapLegCashFlows> calculateReceiveLegCashFlows() {
    return Result.success(_derivative.accept(CFDC, createCashFlowDetailsProvider(PayReceiveType.RECEIVE)));
  }

  private <T> T calculateResult(InstrumentDerivativeVisitorAdapter<MulticurveProviderInterface, T> calculator) {
    return _derivative.accept(calculator, _bundle);
  }

  private ReferenceAmount<Pair<String, Currency>> calculateResult(InstrumentDerivativeVisitor<MulticurveProviderInterface, ReferenceAmount<Pair<String, Currency>>> calculator) {
    return _derivative.accept(calculator, _bundle);
  }

  private InstrumentDerivative createInstrumentDerivative(InterestRateSwapSecurity security,
                                                          ZonedDateTime valuationTime,
                                                          FixedIncomeConverterDataProvider definitionConverter,
                                                          HistoricalTimeSeriesBundle fixings) {
    return definitionConverter.convert(security, _definition, valuationTime, fixings);
  }

  private CashFlowDetailsProvider createCashFlowDetailsProvider(PayReceiveType type) {
    return new CashFlowDetailsProvider(_bundle, _valuationTime, _definition, _security, type);
  }

}
