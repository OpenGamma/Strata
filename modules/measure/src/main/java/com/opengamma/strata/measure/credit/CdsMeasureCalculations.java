package com.opengamma.strata.measure.credit;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.SplitCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.pricer.credit.AccrualOnDefaultFormula;
import com.opengamma.strata.pricer.credit.AnalyticSpreadSensitivityCalculator;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;
import com.opengamma.strata.pricer.credit.IsdaCdsTradePricer;
import com.opengamma.strata.pricer.credit.SpreadSensitivityCalculator;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;

public class CdsMeasureCalculations {

  public static final CdsMeasureCalculations DEFAULT =
      new CdsMeasureCalculations(new IsdaCdsTradePricer(AccrualOnDefaultFormula.CORRECT));

  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;

  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;
  
  private final IsdaCdsTradePricer tradePricer;

  private final SpreadSensitivityCalculator cs01Calculator;

  public CdsMeasureCalculations(IsdaCdsTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
    this.cs01Calculator = new AnalyticSpreadSensitivityCalculator(tradePricer.getAccrualOnDefaultFormula());
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> presentValue(trade, marketData.scenario(i).creditRatesProvider(), PriceType.DIRTY, refData));
  }

  // calculates present value for one scenario
  CurrencyAmount presentValue(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      PriceType priceType,
      ReferenceData refData) {

    return tradePricer.presentValue(trade, ratesProvider, priceType, refData);
  }

  //-------------------------------------------------------------------------
  // calculates principal for all scenarios
  CurrencyScenarioArray principal(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> principal(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates principal for one scenario
  CurrencyAmount principal(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return tradePricer.presentValueOnSettle(trade, ratesProvider, PriceType.CLEAN, refData);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated parallel IR01 for all scenarios
  CurrencyScenarioArray ir01CalibratedPrallel(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> ir01CalibratedPrallel(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates calibrated parallel IR01 for one scenario
  private CurrencyAmount ir01CalibratedPrallel(
      ResolvedCdsTrade trade,
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
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> ir01CalibratedBucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates calibrated bucketed IR01 for one scenario
  private CurrencyParameterSensitivity ir01CalibratedBucketed(
      ResolvedCdsTrade trade,
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
  MultiCurrencyScenarioArray ir01MarketParallel(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> ir01MarketQuoteParallel(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates market quote parallel IR01 for one scenario
  MultiCurrencyAmount ir01MarketQuoteParallel(
      ResolvedCdsTrade trade,
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
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> ir01MarketQuoteBucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates market quote bucketed IR01 for one scenario
  CurrencyParameterSensitivities ir01MarketQuoteBucketed(
      ResolvedCdsTrade trade,
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
  // calculates calibrated parallel PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedParallel(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedParallel(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates calibrated parallel PV01 for one scenario
  private MultiCurrencyAmount pv01CalibratedParallel(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider, refData);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates calibrated bucketed PV01 for one scenario
  private CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider, refData);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote parallel PV01 for all scenarios
  MultiCurrencyScenarioArray pv01MarketQuoteParallel(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteParallel(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates market quote parallel PV01 for one scenario
  MultiCurrencyAmount pv01MarketQuoteParallel(
      ResolvedCdsTrade trade,
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
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteBucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider, refData);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates parallel CS01 for all scenarios
  CurrencyScenarioArray cs01Parallel(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> cs01Parallel(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates parallel CS01 for one scenario
  CurrencyAmount cs01Parallel(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return cs01Calculator.parallelCs01(trade, ratesProvider, refData);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed CS01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivity> cs01Bucketed(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> cs01Bucketed(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates bucketed CS01 for one scenario
  CurrencyParameterSensitivity cs01Bucketed(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return cs01Calculator.bucketedCs01(trade, ratesProvider, refData);
  }

  //-------------------------------------------------------------------------
  // calculates recovery01 for all scenarios
  CurrencyScenarioArray recovery01(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> recovery01(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates recovery01 for one scenario
  CurrencyAmount recovery01(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return tradePricer.recovery01OnSettle(trade, ratesProvider, refData);
  }

  //-------------------------------------------------------------------------
  // calculates recovery01 for all scenarios
  ScenarioArray<SplitCurrencyAmount<StandardId>> jumpToDefault(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> jumpToDefault(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates recovery01 for one scenario
  SplitCurrencyAmount<StandardId> jumpToDefault(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return tradePricer.jumpToDefault(trade, ratesProvider, refData);
  }

  //-------------------------------------------------------------------------
  // calculates recovery01 for all scenarios
  CurrencyScenarioArray expectedLoss(
      ResolvedCdsTrade trade,
      CreditRatesScenarioMarketData marketData,
      ReferenceData refData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> recovery01(trade, marketData.scenario(i).creditRatesProvider(), refData));
  }

  // calculates recovery01 for one scenario
  CurrencyAmount expectedLoss(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider) {

    return tradePricer.expectedLoss(trade, ratesProvider);
  }

}
