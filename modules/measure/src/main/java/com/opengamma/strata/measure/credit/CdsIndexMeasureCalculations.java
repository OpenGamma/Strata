/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.pricer.credit.AccrualOnDefaultFormula;
import com.opengamma.strata.pricer.credit.AnalyticSpreadSensitivityCalculator;
import com.opengamma.strata.pricer.credit.CdsMarketQuoteConverter;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;
import com.opengamma.strata.pricer.credit.IsdaHomogenousCdsIndexTradePricer;
import com.opengamma.strata.pricer.credit.JumpToDefault;
import com.opengamma.strata.pricer.credit.SpreadSensitivityCalculator;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.credit.ResolvedCdsIndexTrade;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;

/**
 * Multi-scenario measure calculations for CDS index trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class CdsIndexMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final CdsIndexMeasureCalculations DEFAULT =
      new CdsIndexMeasureCalculations(new IsdaHomogenousCdsIndexTradePricer(AccrualOnDefaultFormula.CORRECT));

  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedCdsIndexTrade}.
   */
  private final IsdaHomogenousCdsIndexTradePricer tradePricer;
  /**
   * Spread sensitivity calculator.
   */
  private final SpreadSensitivityCalculator cs01Calculator;
  /**
   * Market quote converter.
   */
  private final CdsMarketQuoteConverter converter;

  /**
   * Creates an instance. 
   * 
   * @param tradePricer  the pricer for {@link ResolvedCdsTrade}
   */
  public CdsIndexMeasureCalculations(IsdaHomogenousCdsIndexTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
    this.cs01Calculator = new AnalyticSpreadSensitivityCalculator(tradePricer.getAccrualOnDefaultFormula());
    this.converter = new CdsMarketQuoteConverter(tradePricer.getAccrualOnDefaultFormula());
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> presentValue(trade, marketData.scenario(i).creditRatesProvider(), PriceType.DIRTY, refData));
  }

  // calculates present value for one scenario
  CurrencyAmount presentValue(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      PriceType priceType,
      ReferenceData refData) {

    return tradePricer.presentValue(trade, ratesProvider, priceType, refData);
  }

  //-------------------------------------------------------------------------
  // calculates principal for all scenarios
  CurrencyScenarioArray principal(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> principal(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates principal for one scenario
  CurrencyAmount principal(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return tradePricer.presentValueOnSettle(trade, ratesProvider, PriceType.CLEAN, refData);
  }

  //-------------------------------------------------------------------------
  // calculates price for all scenarios
  DoubleScenarioArray unitPrice(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return DoubleScenarioArray.of(
        marketData.getScenarioCount(),
        i -> unitPrice(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates price for one scenario
  double unitPrice(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    double puf = tradePricer.price(trade, ratesProvider, PriceType.CLEAN, refData);
    return converter.cleanPriceFromPointsUpfront(puf);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated parallel IR01 for all scenarios
  CurrencyScenarioArray ir01CalibratedParallel(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> ir01CalibratedParallel(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates calibrated parallel IR01 for one scenario
  CurrencyAmount ir01CalibratedParallel(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueOnSettleSensitivity(trade, ratesProvider, refData);
    CurrencyParameterSensitivity irSensitivity = ratesProvider.singleDiscountCurveParameterSensitivity(
        pointSensitivity,
        trade.getProduct().getCurrency());
    return irSensitivity.total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed IR01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivity> ir01CalibratedBucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> ir01CalibratedBucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates calibrated bucketed IR01 for one scenario
  CurrencyParameterSensitivity ir01CalibratedBucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueOnSettleSensitivity(trade, ratesProvider, refData);
    CurrencyParameterSensitivity irSensitivity = ratesProvider.singleDiscountCurveParameterSensitivity(
        pointSensitivity,
        trade.getProduct().getCurrency());
    return irSensitivity.multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote parallel IR01 for all scenarios
  MultiCurrencyScenarioArray ir01MarketQuoteParallel(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> ir01MarketQuoteParallel(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates market quote parallel IR01 for one scenario
  MultiCurrencyAmount ir01MarketQuoteParallel(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueOnSettleSensitivity(trade, ratesProvider, refData);
    CurrencyParameterSensitivity parameterSensitivity =
        ratesProvider.singleDiscountCurveParameterSensitivity(pointSensitivity, trade.getProduct().getCurrency());
    CurrencyParameterSensitivities irSensitivity = MARKET_QUOTE_SENS.sensitivity(
        CurrencyParameterSensitivities.of(parameterSensitivity),
        ratesProvider);
    return irSensitivity.total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed IR01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> ir01MarketQuoteBucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> ir01MarketQuoteBucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates market quote bucketed IR01 for one scenario
  CurrencyParameterSensitivities ir01MarketQuoteBucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueOnSettleSensitivity(trade, ratesProvider, refData);
    CurrencyParameterSensitivity parameterSensitivity = ratesProvider.singleDiscountCurveParameterSensitivity(
        pointSensitivity,
        trade.getProduct().getCurrency());
    CurrencyParameterSensitivities irSensitivity = MARKET_QUOTE_SENS.sensitivity(
        CurrencyParameterSensitivities.of(parameterSensitivity),
        ratesProvider);
    return irSensitivity.multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedSum(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider, refData);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider, refData);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01MarketQuoteSum(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteSum(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider, refData);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    CurrencyParameterSensitivities quoteSensitivity = MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider);
    return quoteSensitivity.total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteBucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider, refData);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates parallel CS01 for all scenarios
  CurrencyScenarioArray cs01Parallel(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> cs01Parallel(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates parallel CS01 for one scenario
  CurrencyAmount cs01Parallel(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return cs01Calculator.parallelCs01(trade, ratesProvider, refData);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed CS01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivity> cs01Bucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> cs01Bucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates bucketed CS01 for one scenario
  CurrencyParameterSensitivity cs01Bucketed(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return cs01Calculator.bucketedCs01(trade, ratesProvider, refData);
  }

  //-------------------------------------------------------------------------
  // calculates recovery01 for all scenarios
  CurrencyScenarioArray recovery01(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> recovery01(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates recovery01 for one scenario
  CurrencyAmount recovery01(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return tradePricer.recovery01OnSettle(trade, ratesProvider, refData);
  }

  //-------------------------------------------------------------------------
  // calculates jump-to-default for all scenarios
  ScenarioArray<JumpToDefault> jumpToDefault(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> jumpToDefault(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates jump-to-default for one scenario
  JumpToDefault jumpToDefault(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return tradePricer.jumpToDefault(trade, ratesProvider, refData);
  }

  //-------------------------------------------------------------------------
  // calculates expected loss for all scenarios
  CurrencyScenarioArray expectedLoss(
      ResolvedCdsIndexTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> expectedLoss(trade, marketData.scenario(i).creditRatesProvider()));
  }

  // calculates expected loss for one scenario
  CurrencyAmount expectedLoss(
      ResolvedCdsIndexTrade trade,
      CreditRatesProvider ratesProvider) {

    return tradePricer.expectedLoss(trade, ratesProvider);
  }

}
