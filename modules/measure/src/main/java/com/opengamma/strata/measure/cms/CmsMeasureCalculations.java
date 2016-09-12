/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.cms;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.measure.swaption.SwaptionScenarioMarketData;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsLegPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsPeriodPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsProductPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.product.cms.ResolvedCmsTrade;

/**
 * Multi-scenario measure calculations for CMS trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class CmsMeasureCalculations {

  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedCmsTrade}.
   */
  private final SabrExtrapolationReplicationCmsTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param cmsParams  the CMS parameters
   */
  CmsMeasureCalculations(CmsSabrExtrapolationParams cmsParams) {
    SabrExtrapolationReplicationCmsPeriodPricer periodPricer =
        SabrExtrapolationReplicationCmsPeriodPricer.of(cmsParams.getCutOffStrike(), cmsParams.getMu());
    SabrExtrapolationReplicationCmsLegPricer legPricer = new SabrExtrapolationReplicationCmsLegPricer(periodPricer);
    SabrExtrapolationReplicationCmsProductPricer productPricer = new SabrExtrapolationReplicationCmsProductPricer(legPricer);
    SabrExtrapolationReplicationCmsTradePricer tradePricer = new SabrExtrapolationReplicationCmsTradePricer(productPricer);
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer function for {@link ResolvedCmsTrade}
   */
  CmsMeasureCalculations(SabrExtrapolationReplicationCmsTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  MultiCurrencyScenarioArray presentValue(
      ResolvedCmsTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    IborIndex index = cmsLegIborIndex(trade);
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // present value for one scenario
  MultiCurrencyAmount presentValue(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    return tradePricer.presentValue(trade, ratesProvider, checkSabr(volatilities));
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedCmsTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    IborIndex index = cmsLegIborIndex(trade);
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedCmsTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    IborIndex index = cmsLegIborIndex(trade);
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedCmsTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    IborIndex index = cmsLegIborIndex(trade);
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedCmsTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    IborIndex index = cmsLegIborIndex(trade);
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  // point sensitivity
  private PointSensitivities pointSensitivity(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    return tradePricer.presentValueSensitivityRates(trade, ratesProvider, checkSabr(volatilities));
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedCmsTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    IborIndex index = cmsLegIborIndex(trade);
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currencyExposure(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    return tradePricer.currencyExposure(trade, ratesProvider, checkSabr(volatilities));
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  MultiCurrencyScenarioArray currentCash(
      ResolvedCmsTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    IborIndex index = cmsLegIborIndex(trade);
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currentCash(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // current cash for one scenario
  MultiCurrencyAmount currentCash(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    return tradePricer.currentCash(trade, ratesProvider, checkSabr(volatilities));
  }

  //-------------------------------------------------------------------------
  // returns the Ibor index or the CMS leg
  static IborIndex cmsLegIborIndex(ResolvedCmsTrade trade) {
    return trade.getProduct().getCmsLeg().getUnderlyingIndex();
  }

  // checks that the volatilities are for SABR
  private static SabrSwaptionVolatilities checkSabr(SwaptionVolatilities volatilities) {
    if (volatilities instanceof SabrSwaptionVolatilities) {
      return (SabrSwaptionVolatilities) volatilities;
    }
    throw new IllegalArgumentException(
        "Swaption volatiliies for pricing CMS must be for SABR model, but was: " + volatilities.getVolatilityType());
  }

}
