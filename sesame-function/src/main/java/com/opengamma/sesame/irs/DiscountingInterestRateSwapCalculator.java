/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.swap.SwapDefinition;
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
import com.opengamma.financial.analytics.conversion.InterestRateSwapSecurityConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.financial.security.irs.PayReceiveType;
import com.opengamma.sesame.cashflows.CashFlowDetailsCalculator;
import com.opengamma.sesame.cashflows.CashFlowDetailsProvider;
import com.opengamma.sesame.cashflows.SwapLegCashFlows;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Calculator for Discounting swaps.
 */
public class DiscountingInterestRateSwapCalculator implements InterestRateSwapCalculator {

  /**
   * Calculator for cash flows.
   */
  private static final CashFlowDetailsCalculator CFDC = CashFlowDetailsCalculator.getInstance();

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
   * Creates a calculator for a InterestRateSwapSecurity.
   *
   * @param security the swap to calculate values for, not null
   * @param bundle the multicurve bundle, including the curves, not null
   * @param swapConverter the InterestRateSwapSecurityConverter, not null
   * @param valuationTime the ZonedDateTime, not null
   * @param definitionConverter the FixedIncomeConverterDataProvider, not null
   * @param fixings the HistoricalTimeSeriesBundle, a collection of historical time-series objects, not null
   */
  public DiscountingInterestRateSwapCalculator(InterestRateSwapSecurity security,
                                               MulticurveProviderInterface bundle,
                                               InterestRateSwapSecurityConverter swapConverter,
                                               ZonedDateTime valuationTime,
                                               FixedIncomeConverterDataProvider definitionConverter,
                                               HistoricalTimeSeriesBundle fixings) {
    ArgumentChecker.notNull(security, "security");
    ArgumentChecker.notNull(swapConverter, "swapConverter");
    ArgumentChecker.notNull(valuationTime, "valuationTime");
    ArgumentChecker.notNull(definitionConverter, "definitionConverter");
    ArgumentChecker.notNull(fixings, "fixings");
    _definition = (SwapDefinition) security.accept(swapConverter);
    _derivative = createInstrumentDerivative(security, valuationTime, definitionConverter, fixings);
    _bundle = bundle;
    _valuationTime = valuationTime;
    _security = security;
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(calculateResult(PVDC));
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
