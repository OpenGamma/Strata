/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap.e2e;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.pricer.datasets.StandardDataSets;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.FxResetCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test end to end for cross currency swaps.
 */
@Test
public class SwapCrossCurrencyEnd2EndTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ImmutableReferenceData.of(CalendarUSD.NYC, CalendarUSD.NYC_CALENDAR));
  private static final IborIndex EUR_EURIBOR_3M = IborIndices.EUR_EURIBOR_3M;
  private static final IborIndex USD_LIBOR_3M = IborIndices.USD_LIBOR_3M;
  private static final FxIndex EUR_USD_WM = FxIndices.EUR_USD_WM;
  private static final double NOTIONAL_USD = 120_000_000;
  private static final double NOTIONAL_EUR = 100_000_000;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CalendarUSD.NYC);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, CalendarUSD.NYC);

  // tolerance
  private static final double TOLERANCE_PV = 1.0E-4;

  //-----------------------------------------------------------------------
  // XCcy swap with exchange of notional
  public void test_XCcyEur3MSpreadVsUSD3M() {
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2016, 1, 24))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .finalExchange(true)
            .initialExchange(true)
            .amount(ValueSchedule.of(NOTIONAL_EUR))
            .currency(EUR)
            .build())
        .calculation(IborRateCalculation.builder()
            .index(EUR_EURIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .spread(ValueSchedule.of(0.0020))
            .build())
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2016, 1, 24))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .finalExchange(true)
            .initialExchange(true)
            .amount(ValueSchedule.of(NOTIONAL_USD))
            .currency(USD)
            .build())
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    ResolvedSwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build()
        .resolve(REF_DATA);

    double pvUsdExpected = 431944.6868;
    double pvEurExpected = -731021.1778;

    DiscountingSwapTradePricer pricer = swapPricer();
    MultiCurrencyAmount pv = pricer.presentValue(trade, provider());
    assertEquals(pv.getAmount(USD).getAmount(), pvUsdExpected, TOLERANCE_PV);
    assertEquals(pv.getAmount(EUR).getAmount(), pvEurExpected, TOLERANCE_PV);
  }

  // XCcy swap with exchange of notional and FX Reset on the USD leg
  public void test_XCcyEur3MSpreadVsUSD3MFxReset() {
    //Test all possible combinations of exchange flags
    boolean[] allBoolean = {true, false};
    for (boolean initialExchange : allBoolean) {
      for (boolean intermediateExchange : allBoolean) {
        for (boolean finalExchange : allBoolean) {
          //Skip the case where all exchanges are false, this is tested separately
          if (initialExchange || intermediateExchange || finalExchange) {
            test_XCcyEurUSDFxReset(initialExchange, intermediateExchange, finalExchange);
          }
        }
      }
    }
  }

  // XCcy swap with exchange of notional and FX Reset on the USD leg
  public void test_XCcyFixedInitialNotional() {

    DiscountingSwapTradePricer pricer = swapPricer();

    //Create an MTM swap with initial notional override
    double notional = 1_000_000d;
    ResolvedSwapTrade fixedNotionalMtmTrade = getMtmTrade(true, true, true, notional).resolve(REF_DATA);
    ExplainMap explainMap = pricer.explainPresentValue(fixedNotionalMtmTrade, provider());

    CurrencyAmount fixedNotional = CurrencyAmount.of(Currency.USD, notional);
    ExplainMap fixedNotionalMtmLeg = explainMap.get(ExplainKey.LEGS).get().get(1);

    List<ExplainMap> events = fixedNotionalMtmLeg.get(ExplainKey.PAYMENT_EVENTS).get();

    //First two payment events should use fixed initial notional
    ExplainMap firstPaymentEvent = events.get(0);
    assertFixedNotionalPaymentEvent(firstPaymentEvent, fixedNotional.negated());
    ExplainMap secondPaymentEvent = events.get(1);
    assertFixedNotionalPaymentEvent(secondPaymentEvent, fixedNotional);

    //First coupon also uses fixed notional
    ExplainMap firstCoupon = fixedNotionalMtmLeg.get(ExplainKey.PAYMENT_PERIODS).get().get(0);
    assertEquals(firstCoupon.get(ExplainKey.TRADE_NOTIONAL), Optional.of(fixedNotional));
    assertEquals(firstCoupon.get(ExplainKey.NOTIONAL), Optional.of(fixedNotional));

    //Sum of all pv amounts which are impacted by overriding the first period with a  fixed notional
    CurrencyAmount firstPaymentPv = firstPaymentEvent.get(ExplainKey.PRESENT_VALUE).get();
    CurrencyAmount secondPaymentPv = secondPaymentEvent.get(ExplainKey.PRESENT_VALUE).get();
    CurrencyAmount firstCouponPv = firstCoupon.get(ExplainKey.PRESENT_VALUE).get();
    CurrencyAmount fixedNotionalImpactedEventsPv = firstPaymentPv.plus(secondPaymentPv).plus(firstCouponPv);

    //----------------------------------------------------------------------------------------------------------

    //Build identical trade but with no fixed notional
    ResolvedSwapTrade noFixedNotionalMtmTrade = getMtmTrade(true, true, true, null).resolve(REF_DATA);
    ExplainMap noFixedNotionalMtmLeg =
        pricer.explainPresentValue(noFixedNotionalMtmTrade, provider()).get(ExplainKey.LEGS).get().get(1);

    //Sum the pvs for the same combination of payments and events that are impacted by fixed notional in first trade
    CurrencyAmount noFixedNotionalEventsPv =
        noFixedNotionalMtmLeg.get(ExplainKey.PAYMENT_EVENTS).get().subList(0, 2).stream()
            .map(payment -> payment.get(ExplainKey.PRESENT_VALUE).get())
            .reduce(CurrencyAmount.zero(Currency.USD), CurrencyAmount::plus);

    CurrencyAmount noFixedNotionalCouponsPv =
        noFixedNotionalMtmLeg.get(ExplainKey.PAYMENT_PERIODS).get().get(0).get(ExplainKey.PRESENT_VALUE).get();
    
    CurrencyAmount noFixedNotionalImpactedEventsPv = noFixedNotionalCouponsPv.plus(noFixedNotionalEventsPv);

    //----------------------------------------------------------------------------------------------------------

    //PV difference of the events impacted by fixing notional
    CurrencyAmount paymentsPvDifference = fixedNotionalImpactedEventsPv.minus(noFixedNotionalImpactedEventsPv);

    //Calculate PV of the full trades
    MultiCurrencyAmount fixedNotionalLegPv = pricer.presentValue(fixedNotionalMtmTrade, provider());
    MultiCurrencyAmount noFixedNotionalLegPv = pricer.presentValue(noFixedNotionalMtmTrade, provider());

    //EUR PV should not have changed
    assertEquals(
        fixedNotionalLegPv.getAmount(Currency.EUR).getAmount(),
        noFixedNotionalLegPv.getAmount(Currency.EUR).getAmount(),
        TOLERANCE_PV);

    //Difference in USD PV should be equal the difference in PV of the three events impacted by the initial notional
    //All else should remain equal
    CurrencyAmount tradePvDifference =
        fixedNotionalLegPv.getAmount(Currency.USD).minus(noFixedNotionalLegPv.getAmount(Currency.USD));
    assertEquals(tradePvDifference.getAmount(), paymentsPvDifference.getAmount(), TOLERANCE_PV);
  }

  private void assertFixedNotionalPaymentEvent(ExplainMap paymentEvent, CurrencyAmount expectedNotional) {

    assertEquals(paymentEvent.get(ExplainKey.TRADE_NOTIONAL), Optional.of(expectedNotional));
    assertEquals(paymentEvent.get(ExplainKey.FORECAST_VALUE), Optional.of(expectedNotional));
    double firstDiscountFactor = paymentEvent.get(ExplainKey.DISCOUNT_FACTOR).get();

    //Fixed notional, so PV is notional * DCF
    CurrencyAmount expectedPv = expectedNotional.multipliedBy(firstDiscountFactor);
    assertEquals(paymentEvent.get(ExplainKey.PRESENT_VALUE), Optional.of(expectedPv));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "FxResetCalculation index EUR/USD-WM was specified but schedule does not include any notional exchanges")
  public void test_FxResetWithNoExchanges() {
    //specifying an FX reset with no exchanges throws an exception
    test_XCcyEurUSDFxReset(false, false, false);
  }

  private void test_XCcyEurUSDFxReset(boolean initialExchange, boolean intermediateExchange, boolean finalExchange) {

    ResolvedSwapTrade trade = getMtmTrade(initialExchange, intermediateExchange, finalExchange, null).resolve(REF_DATA);

    DiscountingSwapTradePricer pricer = swapPricer();
    MultiCurrencyAmount pv = pricer.presentValue(trade, provider());

    //Coupons are always included, so base is the total coupon pvs
    double pvUsdExpected = 1447799.5318;
    double pvEurExpected = -1020648.6461;
    int usdExpectedPaymentEvents = 0;
    int eurExpectedPaymentEvents = 0;

    //Add PV amounts of included exchanges to arrive at total expected pv
    if (initialExchange) {
      pvUsdExpected += -143998710.0091;
      pvEurExpected += 99999104.1730;
      ++usdExpectedPaymentEvents;
      ++eurExpectedPaymentEvents;
    }

    if (intermediateExchange) {
      pvUsdExpected += -344525.1458;
      usdExpectedPaymentEvents += 14;
    }

    if (finalExchange) {
      pvUsdExpected += 143414059.1395;
      pvEurExpected += -99709476.7047;
      ++usdExpectedPaymentEvents;
      ++eurExpectedPaymentEvents;
    }

    assertEquals(pv.getAmount(USD).getAmount(), pvUsdExpected, TOLERANCE_PV);
    assertEquals(pv.getAmount(EUR).getAmount(), pvEurExpected, TOLERANCE_PV);

    //Assert the payment event (exchange) count on each leg
    List<ExplainMap> legs = pricer.explainPresentValue(trade, provider()).get(ExplainKey.LEGS).get();
    assertThat(legs.get(0).get(ExplainKey.PAYMENT_EVENTS).orElse(ImmutableList.of())).hasSize(eurExpectedPaymentEvents);
    assertThat(legs.get(1).get(ExplainKey.PAYMENT_EVENTS).orElse(ImmutableList.of())).hasSize(usdExpectedPaymentEvents);
  }

  private SwapTrade getMtmTrade(
      boolean initialExchange,
      boolean intermediateExchange,
      boolean finalExchange,
      Double initialNotional) {

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2016, 1, 24))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .finalExchange(finalExchange)
            .initialExchange(initialExchange)
            .amount(ValueSchedule.of(NOTIONAL_EUR))
            .currency(EUR)
            .build())
        .calculation(IborRateCalculation.builder()
            .index(EUR_EURIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .spread(ValueSchedule.of(0.0020))
            .build())
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2016, 1, 24))
            .frequency(P3M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .finalExchange(finalExchange)
            .initialExchange(initialExchange)
            .intermediateExchange(intermediateExchange)
            .amount(ValueSchedule.of(NOTIONAL_USD))
            .currency(USD)
            .fxReset(FxResetCalculation.builder()
                .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
                .referenceCurrency(EUR)
                .index(EUR_USD_WM)
                .initialNotionalValue(initialNotional)
                .build())
            .build())
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    return SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg))
        .build();
  }

  //-------------------------------------------------------------------------
  // pricer
  private DiscountingSwapTradePricer swapPricer() {
    return DiscountingSwapTradePricer.DEFAULT;
  }

  // rates provider
  private static RatesProvider provider() {
    return StandardDataSets.providerUsdEurDscL3();
  }

}
