/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import java.util.Map;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.future.calculator.FuturesPriceBlackSTIRFuturesCalculator;
import com.opengamma.analytics.financial.provider.calculator.blackstirfutures.PositionDeltaSTIRFutureOptionCalculator;
import com.opengamma.analytics.financial.provider.calculator.blackstirfutures.PositionGammaSTIRFutureOptionCalculator;
import com.opengamma.analytics.financial.provider.calculator.blackstirfutures.PositionThetaSTIRFutureOptionCalculator;
import com.opengamma.analytics.financial.provider.calculator.blackstirfutures.PositionVegaSTIRFutureOptionCalculator;
import com.opengamma.analytics.financial.provider.calculator.blackstirfutures.PresentValueBlackSTIRFutureOptionCalculator;
import com.opengamma.analytics.financial.provider.calculator.blackstirfutures.PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.BlackSTIRFuturesProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.parameter.ParameterSensitivityParameterCalculator;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.InterestRateFutureOptionTradeConverter;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.sesame.ZeroIRDeltaBucketingUtils;
import com.opengamma.sesame.trade.IRFutureOptionTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Interest rate future option Black calculator.
 */
public class IRFutureOptionBlackCalculator implements IRFutureOptionCalculator {

  /**
   * Calculator for the present value of the interest rate future option.
   */
  private static final PresentValueBlackSTIRFutureOptionCalculator PV_CALC = PresentValueBlackSTIRFutureOptionCalculator
      .getInstance();

  /**
   * Calculator for the PV01 of the interest rate future option.
   */
  private static final PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator PV01_CALC =
      PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator.getInstance();

  /**
   * Calculator the model price of the interest rate future option.
   */
  private static final FuturesPriceBlackSTIRFuturesCalculator PRICE_CALC = FuturesPriceBlackSTIRFuturesCalculator
      .getInstance();
  /**
   * Calculate for the delta of the interest rate future option.
   */
  private static final PositionDeltaSTIRFutureOptionCalculator DELTA_CALC = PositionDeltaSTIRFutureOptionCalculator
      .getInstance();
  /**
   * Calculate for the gamma of the interest rate future option.
   */
  private static final PositionGammaSTIRFutureOptionCalculator GAMMA_CALC = PositionGammaSTIRFutureOptionCalculator
      .getInstance();
  /**
   * Calculate for the vega of the interest rate future option.
   */
  private static final PositionVegaSTIRFutureOptionCalculator VEGA_CALC = PositionVegaSTIRFutureOptionCalculator
      .getInstance();
  /**
   * Calculate for the theta of the interest rate future option.
   */
  private static final PositionThetaSTIRFutureOptionCalculator THETA_CALC = PositionThetaSTIRFutureOptionCalculator
      .getInstance();
  /**
   * Calculator for the curve sensitivity of the interest rate future option.
   */
  private static final PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator PVCSBFOC =
      PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator.getInstance();
  /**
   * Calculator for the parameter sensitivity of the interest rate future option.
   */
  private static final ParameterSensitivityParameterCalculator<BlackSTIRFuturesProviderInterface> PSSFC =
      new ParameterSensitivityParameterCalculator<>(PVCSBFOC);

  /**
   * Derivative form of the security.
   */
  private final InstrumentDerivative _derivative;

  /**
   * Black volatility data.
   */
  private final BlackSTIRFuturesProviderInterface _black;

  /**
   * Map containing the curve definition for each curve in the multicurve.
   */
  private final Map<String, CurveDefinition> _curveDefinitions;

  /**
   * Constructs a calculator for interest rate future options using the Black model.
   * @param trade the interest rate future option trade, not null.
   * @param converter the converter used to create the definition of the interest rate future option, not null.
   * @param black the Black volatility data, not null.
   * @param valTime the valuation date time, not null.
   * @param definitionToDerivativeConverter the converter to create the derivative form of the future option, not null.
   * @param fixings the historical prices of the underlying interest rate future, not null.
   * @param curveDefinitions map containing the per curve definition
   */
  public IRFutureOptionBlackCalculator(IRFutureOptionTrade trade,
      InterestRateFutureOptionTradeConverter converter,
      BlackSTIRFuturesProviderInterface black,
      ZonedDateTime valTime,
      FixedIncomeConverterDataProvider definitionToDerivativeConverter,
      HistoricalTimeSeriesBundle fixings,
      Map<String, CurveDefinition> curveDefinitions) {
    _derivative = createInstrumentDerivative(ArgumentChecker.notNull(trade, "trade"),
        ArgumentChecker.notNull(converter, "converter"),
        ArgumentChecker.notNull(valTime, "valTime"),
        ArgumentChecker.notNull(definitionToDerivativeConverter, "definitionToDerivativeConverter"),
        ArgumentChecker.notNull(fixings, "fixings"));
    _black = ArgumentChecker.notNull(black, "black");
    _curveDefinitions = ArgumentChecker.notNull(curveDefinitions, "curveDefinitions");
  }

  private InstrumentDerivative createInstrumentDerivative(IRFutureOptionTrade trade,
      InterestRateFutureOptionTradeConverter converter,
      ZonedDateTime valTime,
      FixedIncomeConverterDataProvider definitionToDerivativeConverter,
      HistoricalTimeSeriesBundle fixings) {
    InstrumentDefinition<?> definition = converter.convert(trade);
    return definitionToDerivativeConverter.convert(trade.getSecurity(), definition, valTime, fixings);
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(_derivative.accept(PV_CALC, _black));
  }

  @Override
  public Result<MultipleCurrencyMulticurveSensitivity> calculatePV01() {
    return Result.success(_derivative.accept(PV01_CALC, _black));
  }

  @Override
  public Result<Double> calculateModelPrice() {
    return Result.success(_derivative.accept(PRICE_CALC, _black));
  }

  @Override
  public Result<Double> calculateDelta() {
    return Result.success(_derivative.accept(DELTA_CALC, _black));
  }

  @Override
  public Result<Double> calculateGamma() {
    return Result.success(_derivative.accept(GAMMA_CALC, _black));
  }

  @Override
  public Result<Double> calculateVega() {
    return Result.success(_derivative.accept(VEGA_CALC, _black));
  }

  @Override
  public Result<Double> calculateTheta() {
    return Result.success(_derivative.accept(THETA_CALC, _black));
  }

  @Override
  public Result<BucketedCurveSensitivities> calculateBucketedZeroIRDelta() {
    MultipleCurrencyParameterSensitivity sensitivities = PSSFC.calculateSensitivity(_derivative, _black);
    BucketedCurveSensitivities bucketedCurveSensitivities = 
        ZeroIRDeltaBucketingUtils.getBucketedCurveSensitivities(sensitivities, _curveDefinitions);

    return Result.success(bucketedCurveSensitivities);
  }
}
