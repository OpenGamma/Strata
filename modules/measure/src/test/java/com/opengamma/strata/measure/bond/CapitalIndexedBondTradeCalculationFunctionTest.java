/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ICMA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.bond.CapitalIndexedBondYieldConvention.US_IL_REAL;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.bond.CapitalIndexedBondCurveDataSet;
import com.opengamma.strata.pricer.bond.DiscountingCapitalIndexedBondTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityGroup;
import com.opengamma.strata.pricer.bond.RepoGroup;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.bond.CapitalIndexedBond;
import com.opengamma.strata.product.bond.CapitalIndexedBondTrade;
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBondTrade;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;

/**
 * Test {@link CapitalIndexedBondTradeCalculationFunction}.
 */
@Test
public class CapitalIndexedBondTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // detachment date (for nonzero ex-coupon days) < valuation date < payment date
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 7, 13);

  private static final DaysAdjustment SETTLE_OFFSET = DaysAdjustment.ofBusinessDays(3, USNY);
  private static final Currency CURRENCY = USD;
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final PeriodicSchedule SCHEDULE =
      PeriodicSchedule.of(date(2006, 1, 15), date(2016, 1, 15), P6M, BUSINESS_ADJUST, StubConvention.NONE, false);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Ticker", "BOND1");
  private static final StandardId ISSUER_ID = CapitalIndexedBondCurveDataSet.ISSUER_ID;
  private static final CapitalIndexedBond PRODUCT = CapitalIndexedBond.builder()
      .securityId(SECURITY_ID)
      .notional(10_000_000d)
      .currency(CURRENCY)
      .dayCount(ACT_ACT_ICMA)
      .rateCalculation(InflationRateCalculation.builder()
          .gearing(ValueSchedule.of(0.01))
          .index(US_CPI_U)
          .lag(Period.ofMonths(3))
          .indexCalculationMethod(PriceIndexCalculationMethod.INTERPOLATED)
          .firstIndexValue(198.47742)
          .build())
      .legalEntityId(ISSUER_ID)
      .yieldConvention(US_IL_REAL)
      .settlementDateOffset(SETTLE_OFFSET)
      .accrualSchedule(SCHEDULE)
      .build();

  private static final long QUANTITY = 100L;
  private static final LocalDate SETTLEMENT_STANDARD = SETTLE_OFFSET.adjust(VAL_DATE, REF_DATA);
  private static final TradeInfo TRADE_INFO_STANDARD = TradeInfo.builder().settlementDate(SETTLEMENT_STANDARD).build();
  private static final double TRADE_PRICE = 1.0203;
  private static final CapitalIndexedBondTrade TRADE = CapitalIndexedBondTrade.builder()
      .info(TRADE_INFO_STANDARD)
      .product(PRODUCT)
      .quantity(QUANTITY)
      .price(TRADE_PRICE)
      .build();
  public static final ResolvedCapitalIndexedBondTrade RTRADE = TRADE.resolve(REF_DATA);

  private static final RepoGroup REPO_GROUP = CapitalIndexedBondCurveDataSet.GROUP_REPO;
  private static final LegalEntityGroup ISSUER_GROUP = CapitalIndexedBondCurveDataSet.GROUP_ISSUER;
  private static final CurveId INF_CURVE_ID = CurveId.of("Default", "Inflation");
  private static final CurveId REPO_CURVE_ID = CurveId.of("Default", "Repo");
  private static final CurveId ISSUER_CURVE_ID = CurveId.of("Default", "Issuer");
  public static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(),
      ImmutableMap.of(US_CPI_U, INF_CURVE_ID));
  public static final LegalEntityDiscountingMarketDataLookup LED_LOOKUP = LegalEntityDiscountingMarketDataLookup.of(
      ImmutableMap.of(ISSUER_ID, REPO_GROUP),
      ImmutableMap.of(Pair.of(REPO_GROUP, CURRENCY), REPO_CURVE_ID),
      ImmutableMap.of(ISSUER_ID, ISSUER_GROUP),
      ImmutableMap.of(Pair.of(ISSUER_GROUP, CURRENCY), ISSUER_CURVE_ID));
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP, LED_LOOKUP);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    CapitalIndexedBondTradeCalculationFunction function = new CapitalIndexedBondTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(ImmutableSet.of(INF_CURVE_ID, REPO_CURVE_ID, ISSUER_CURVE_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(US_CPI_U)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    CapitalIndexedBondTradeCalculationFunction function = new CapitalIndexedBondTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider ratesProvider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    LegalEntityDiscountingProvider ledProvider = LED_LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingCapitalIndexedBondTradePricer pricer = DiscountingCapitalIndexedBondTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, ratesProvider, ledProvider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, ratesProvider, ledProvider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, ratesProvider);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(RTRADE));
  }

  public void test_pv01() {
    CapitalIndexedBondTradeCalculationFunction function = new CapitalIndexedBondTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider ratesProvider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    LegalEntityDiscountingProvider ledProvider = LED_LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingCapitalIndexedBondTradePricer pricer = DiscountingCapitalIndexedBondTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, ratesProvider, ledProvider);
    CurrencyParameterSensitivities pvParamSens = ledProvider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PV01_CALIBRATED_SUM,
        Measures.PV01_CALIBRATED_BUCKETED);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01_CALIBRATED_SUM, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal))))
        .containsEntry(
            Measures.PV01_CALIBRATED_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed))));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    return new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            INF_CURVE_ID, CapitalIndexedBondCurveDataSet.CPI_CURVE,
            REPO_CURVE_ID, CapitalIndexedBondCurveDataSet.REPO_CURVE,
            ISSUER_CURVE_ID, CapitalIndexedBondCurveDataSet.ISSUER_CURVE),
        ImmutableMap.of(IndexQuoteId.of(US_CPI_U), CapitalIndexedBondCurveDataSet.getTimeSeries(VAL_DATE)));
  }

}
