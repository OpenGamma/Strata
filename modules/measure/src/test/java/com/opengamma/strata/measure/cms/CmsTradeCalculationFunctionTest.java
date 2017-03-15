/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.cms;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.swaption.SwaptionMarketDataLookup;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsLegPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsPeriodPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsProductPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionSabrRateVolatilityDataSet;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId;
import com.opengamma.strata.product.cms.Cms;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.CmsTrade;
import com.opengamma.strata.product.cms.ResolvedCmsTrade;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link CmsTradeCalculationFunction}.
 */
@Test
public class CmsTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final SwapIndex SWAP_INDEX = SwapIndices.EUR_EURIBOR_1100_5Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2020, 10, 21);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ_EUR =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE_EUR =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.NONE, RollConventions.NONE);
  private static final List<ValueStep> NOTIONAL_STEPS = new ArrayList<ValueStep>();
  private static final double NOTIONAL_VALUE_0 = 100_000_000;
  private static final double NOTIONAL_VALUE_1 = 1.1e6;
  private static final double NOTIONAL_VALUE_2 = 0.9e6;
  private static final double NOTIONAL_VALUE_3 = 1.2e6;
  static {
    NOTIONAL_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(NOTIONAL_VALUE_1)));
    NOTIONAL_STEPS.add(ValueStep.of(2, ValueAdjustment.ofReplace(NOTIONAL_VALUE_2)));
    NOTIONAL_STEPS.add(ValueStep.of(3, ValueAdjustment.ofReplace(NOTIONAL_VALUE_3)));
  }
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE_0, NOTIONAL_STEPS);
  private static final Cms PRODUCT = Cms.of(CmsLeg.builder()
      .index(SWAP_INDEX)
      .notional(NOTIONAL)
      .payReceive(PAY)
      .paymentSchedule(SCHEDULE_EUR)
      .build());
  public static final CmsTrade TRADE = CmsTrade.builder()
      .product(PRODUCT)
      .build();
  public static final ResolvedCmsTrade RTRADE = TRADE.resolve(REF_DATA);

  private static final Currency CURRENCY = PRODUCT.getCmsLeg().getCurrency();
  public static final IborIndex INDEX = (IborIndex) PRODUCT.allRateIndices().iterator().next();
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  public static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  private static final SwaptionVolatilitiesId SWAPTION_ID = SwaptionVolatilitiesId.of("SABRVols");
  public static final SwaptionMarketDataLookup SWAPTION_LOOKUP = SwaptionMarketDataLookup.of(INDEX, SWAPTION_ID);
  private static final double CUT_OFF_STRIKE = 0.10;
  private static final double MU = 2.50;
  public static final CmsSabrExtrapolationParams CMS_MODEL = CmsSabrExtrapolationParams.of(CUT_OFF_STRIKE, MU);
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP, SWAPTION_LOOKUP, CMS_MODEL);
  private static final LocalDate VAL_DATE = START.plusMonths(1);
  public static final SabrSwaptionVolatilities VOLS = SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VAL_DATE, false);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    CmsTradeCalculationFunction function = new CmsTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(DISCOUNT_CURVE_ID, FORWARD_CURVE_ID, SWAPTION_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    CmsTradeCalculationFunction function = new CmsTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    SabrExtrapolationReplicationCmsTradePricer pricer = new SabrExtrapolationReplicationCmsTradePricer(
        new SabrExtrapolationReplicationCmsProductPricer(
            new SabrExtrapolationReplicationCmsLegPricer(
                SabrExtrapolationReplicationCmsPeriodPricer.of(CUT_OFF_STRIKE, MU))));
    ResolvedCmsTrade resolved = TRADE.resolve(REF_DATA);
    MultiCurrencyAmount expectedPv = pricer.presentValue(resolved, provider, VOLS);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE, Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(TRADE.resolve(REF_DATA)));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(date(2015, 10, 19), 0.013);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DISCOUNT_CURVE_ID, SwaptionSabrRateVolatilityDataSet.CURVE_DSC_EUR,
            FORWARD_CURVE_ID, SwaptionSabrRateVolatilityDataSet.CURVE_FWD_EUR,
            SWAPTION_ID, VOLS),
        ImmutableMap.of(
            IndexQuoteId.of(SWAP_INDEX), ts));
    return md;
  }

}
