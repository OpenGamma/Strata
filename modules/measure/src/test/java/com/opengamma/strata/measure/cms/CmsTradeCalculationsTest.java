/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.cms;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.swaption.SwaptionMarketDataLookup;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsLegPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsPeriodPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsProductPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.product.cms.ResolvedCmsTrade;

/**
 * Test {@link CmsTradeCalculations}.
 */
@Test
public class CmsTradeCalculationsTest {

  private static final ResolvedCmsTrade RTRADE = CmsTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = CmsTradeCalculationFunctionTest.RATES_LOOKUP;
  private static final SwaptionMarketDataLookup SWAPTION_LOOKUP = CmsTradeCalculationFunctionTest.SWAPTION_LOOKUP;
  private static final CmsSabrExtrapolationParams CMS_MODEL = CmsTradeCalculationFunctionTest.CMS_MODEL;
  private static final SabrSwaptionVolatilities VOLS = CmsTradeCalculationFunctionTest.VOLS;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    ScenarioMarketData md = CmsTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    SabrExtrapolationReplicationCmsTradePricer pricer = new SabrExtrapolationReplicationCmsTradePricer(
        new SabrExtrapolationReplicationCmsProductPricer(
            new SabrExtrapolationReplicationCmsLegPricer(
                SabrExtrapolationReplicationCmsPeriodPricer.of(CMS_MODEL.getCutOffStrike(), CMS_MODEL.getMu()))));
    MultiCurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider, VOLS);

    CmsTradeCalculations calcs = CmsTradeCalculations.of(CMS_MODEL);
    assertEquals(
        calcs.presentValue(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        calcs.currencyExposure(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertEquals(
        calcs.currentCash(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  public void test_pv01() {
    ScenarioMarketData md = CmsTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    SabrExtrapolationReplicationCmsTradePricer pricer = new SabrExtrapolationReplicationCmsTradePricer(
        new SabrExtrapolationReplicationCmsProductPricer(
            new SabrExtrapolationReplicationCmsLegPricer(
                SabrExtrapolationReplicationCmsPeriodPricer.of(CMS_MODEL.getCutOffStrike(), CMS_MODEL.getMu()))));
    PointSensitivities pvPointSens = pricer.presentValueSensitivityRates(RTRADE, provider, VOLS);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    CmsTradeCalculations calcs = CmsTradeCalculations.of(CMS_MODEL);
    assertEquals(
        calcs.pv01RatesCalibratedSum(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        calcs.pv01RatesCalibratedBucketed(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
