/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.e2e;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.interestrate.datasets.StandardDataSetsMulticurveEUR;
import com.opengamma.analytics.financial.interestrate.datasets.StandardDataSetsMulticurveUSD;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.swap.FxResetCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.pricer.ImmutableRatesProvider;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.impl.Legacy;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapTradePricer;

/**
 * Test end to end for cross currency swaps.
 */
@Test
public class SwapCrossCurrencyEnd2EndTest {

  private static final IborIndex EUR_EURIBOR_3M = IborIndices.EUR_EURIBOR_3M;
  private static final IborIndex USD_LIBOR_3M = IborIndices.USD_LIBOR_3M;
  private static final FxIndex WM_EUR_USD = FxIndices.WM_EUR_USD;
  private static final double NOTIONAL_USD = 120_000_000;
  private static final double NOTIONAL_EUR = 100_000_000;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CalendarUSD.NYC);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, CalendarUSD.NYC);

  // curve providers
  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_USD_PAIR =
      StandardDataSetsMulticurveUSD.getCurvesUSDOisL3();
  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_EUR_PAIR =
      StandardDataSetsMulticurveEUR.getCurvesEurOisE3();
  private static final FxMatrix FX_MATRIX =
      FxMatrix.builder().addRate(Currency.EUR, Currency.USD, 1.20).build();
  private static final com.opengamma.analytics.financial.instrument.index.IborIndex EUREURIBOR3M =
      MULTICURVE_EUR_PAIR.getFirst().getIndexesIbor().iterator().next();
  private static final MulticurveProviderDiscount MULTICURVE = MULTICURVE_USD_PAIR.getFirst();
  static {
    MULTICURVE.setCurve(Currency.EUR,
        MULTICURVE_EUR_PAIR.getFirst().getCurve(Currency.EUR));
    MULTICURVE.setCurve(EUREURIBOR3M, MULTICURVE_EUR_PAIR.getFirst().getCurve(EUREURIBOR3M));
    MULTICURVE.setForexMatrix(FX_MATRIX);
  }
  private static final CurveBuildingBlockBundle BLOCK = MULTICURVE_USD_PAIR.getSecond();
  static {
    BLOCK.addAll(MULTICURVE_EUR_PAIR.getSecond());
  }

  // tolerance
  private static final double TOLERANCE_PV = 1.0E-4;

  //-----------------------------------------------------------------------
  // XCcy swap with exchange of notional
  public void test_XCcyEur3MSpreadVsUSD3M() {
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2016, 1, 24))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .finalExchange(true)
            .initialExchange(true)
            .amount(ValueSchedule.of(NOTIONAL_EUR))
            .currency(EUR)
            .build())
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(EUR_EURIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .spread(ValueSchedule.of(0.0020))
            .build())
        .build();

    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2016, 1, 24))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .finalExchange(true)
            .initialExchange(true)
            .amount(ValueSchedule.of(NOTIONAL_USD))
            .currency(USD)
            .build())
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build();

    double pvUsdExpected = 431944.6868;
    double pvEurExpected = -731021.1778;

    DiscountingSwapTradePricer pricer = swapPricer();
    MultiCurrencyAmount pv = pricer.presentValue(trade, provider());
    assertEquals(pv.getAmount(USD).getAmount(), pvUsdExpected, TOLERANCE_PV);
    assertEquals(pv.getAmount(EUR).getAmount(), pvEurExpected, TOLERANCE_PV);
  }

  // XCcy swap with exchange of notional and FX Reset on the USD leg
  public void test_XCcyEur3MSpreadVsUSD3MFxReset() {

    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2016, 1, 24))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .finalExchange(true)
            .initialExchange(true)
            .amount(ValueSchedule.of(NOTIONAL_EUR))
            .currency(EUR)
            .build())
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(EUR_EURIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .spread(ValueSchedule.of(0.0020))
            .build())
        .build();

    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2016, 1, 24))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .finalExchange(true)
            .initialExchange(true)
            .amount(ValueSchedule.of(NOTIONAL_USD))
            .currency(USD)
            .fxReset(FxResetCalculation.builder()
                .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
                .referenceCurrency(EUR)
                .index(WM_EUR_USD)
                .build())
            .build())
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build();

    double pvUsdExpected = 518623.5163;
    double pvEurExpected = -731021.1778;

    DiscountingSwapTradePricer pricer = swapPricer();
    MultiCurrencyAmount pv = pricer.presentValue(trade, provider());
    assertEquals(pv.getAmount(USD).getAmount(), pvUsdExpected, TOLERANCE_PV);
    assertEquals(pv.getAmount(EUR).getAmount(), pvEurExpected, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  // pricer
  private DiscountingSwapTradePricer swapPricer() {
    return DiscountingSwapTradePricer.DEFAULT;
  }

  private static final LocalDateDoubleTimeSeries TS_EMTPY = LocalDateDoubleTimeSeries.empty();

  // rates provider
  private static RatesProvider provider() {
    return ImmutableRatesProvider.builder()
        .valuationDate(LocalDate.of(2014, 1, 22))
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.copyOf(MULTICURVE.getDiscountingCurves()))
        .indexCurves(Legacy.indexCurves(MULTICURVE))
        .timeSeries(ImmutableMap.of(
            USD_LIBOR_3M, TS_EMTPY,
            EUR_EURIBOR_3M, TS_EMTPY,
            WM_EUR_USD, TS_EMTPY))
        .dayCount(ACT_ACT_ISDA)
        .build();
  }

}
