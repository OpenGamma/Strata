/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitorAdapter;
import com.opengamma.analytics.financial.provider.calculator.discounting.PV01CurveParametersCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParRateDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueCurveSensitivityDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.FutureTradeConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.sesame.trade.InterestRateFutureTrade;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Default calculator for interest rate futures.
 */
public class InterestRateFutureDiscountingCalculator implements InterestRateFutureCalculator {

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

  /**
   * Derivative form of the security.
   */
  private final InstrumentDerivative _derivative;

  /**
   * The multicurve bundle.
   */
  private final MulticurveProviderInterface _bundle;
  
  public InterestRateFutureDiscountingCalculator(InterestRateFutureTrade irFutureTrade,
                                                 MulticurveProviderInterface bundle,
                                                 FutureTradeConverter tradeConverter,
                                                 ZonedDateTime valuationTime,
                                                 FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                 HistoricalTimeSeriesBundle fixings) {
    _derivative = createInstrumentDerivative(irFutureTrade, tradeConverter, valuationTime, definitionToDerivativeConverter, fixings);
    _bundle = bundle;
  }
  
  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(calculateResult(PVDC));
  }

  @Override
  public Result<Double> calculateParRate() {
    return Result.success(calculateResult(PRDC));
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01() {
    return Result.success(calculateResult(PV01C));
  }

  private <T> T calculateResult(InstrumentDerivativeVisitorAdapter<MulticurveProviderInterface, T> calculator) {
    return _derivative.accept(calculator, _bundle);
  }

  private ReferenceAmount<Pair<String, Currency>> calculateResult(InstrumentDerivativeVisitor<MulticurveProviderInterface, ReferenceAmount<Pair<String, Currency>>> calculator) {
    return _derivative.accept(calculator, _bundle);
  }
  
  private InstrumentDerivative createInstrumentDerivative(InterestRateFutureTrade irFutureTrade,
                                                          FutureTradeConverter converter,
                                                          ZonedDateTime valuationTime,
                                                          FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                          HistoricalTimeSeriesBundle fixings) {
    InstrumentDefinition<?> definition = converter.convert(irFutureTrade);
    return definitionToDerivativeConverter.convert(irFutureTrade.getSecurity(), definition, valuationTime, fixings);
  }
}
