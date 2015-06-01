/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_EXPANDED_SWAP_LEG_PAY;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_EXPANDED_SWAP_LEG_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_EXPANDED_SWAP_LEG_REC_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_OBSERVATION;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD_REC_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.INFLATION_FIXED_SWAP_LEG_PAY_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.INFLATION_MONTHLY_SWAP_LEG_REC_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_PAY_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_PAY_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_CROSS_CURRENCY;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_INFLATION;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_TRADE;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_TRADE_CROSS_CURRENCY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.finance.rate.swap.CompoundingMethod;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.ForwardPriceIndexValues;
import com.opengamma.strata.market.value.PriceIndexValues;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.PriceIndexProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Tests {@link DiscountingSwapProductPricer}.
 */
@Test
public class DiscountingSwapProductPricerTest {

  private static final RatesProvider MOCK_PROV = new MockRatesProvider(date(2014, 1, 22));

  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;

  private static final ImmutableRatesProvider RATES_GBP = RatesProviderDataSets.MULTI_GBP;
  private static final ImmutableRatesProvider RATES_GBP_USD = RatesProviderDataSets.MULTI_GBP_USD;
  private static final double FD_SHIFT = 1.0E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FINITE_DIFFERENCE_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(FD_SHIFT);

  private static final double TOLERANCE_RATE = 1.0e-12;
  private static final double TOLERANCE_RATE_DELTA = 1.0E-6;

  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;
  private static final LocalDate VAL_DATE = date(2014, 7, 8);
  private static final YearMonth VAL_MONTH = YearMonth.of(2014, 7);
  private static final double CONSTANT_INDEX = 242d;
  private static final double START_INDEX = 218d;
  private static final PriceIndexValues PRICE_CURVE = ForwardPriceIndexValues.of(
      GB_RPI,
      VAL_MONTH,
      LocalDateDoubleTimeSeries.of(date(2014, 3, 31), START_INDEX),
      InterpolatedNodalCurve.of(
          "GB_RPI_CURVE_FLAT",
          new double[] {1, 1000},
          new double[] {CONSTANT_INDEX, CONSTANT_INDEX},
          INTERPOLATOR));

  //-------------------------------------------------------------------------
  public void test_legPricer() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    assertEquals(pricerSwap.getLegPricer(), pricerLeg);
  }

  //-------------------------------------------------------------------------
  public void test_parRate_singleCurrency() {
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.discountFactor(GBP, FIXED_RATE_PAYMENT_PERIOD_PAY_GBP.getPaymentDate()))
        .thenReturn(0.99d);
    when(mockProv.getValuationDate()).thenReturn(date(2014, 1, 22));
    when(mockProv.fxRate(GBP, GBP)).thenReturn(1.0);
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    double fwdRate = 0.01;
    double pvCpnIbor = 0.99 * fwdRate * 0.25 * 1_000_000;
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, mockProv))
        .thenReturn(pvCpnIbor);
    double pvCpnFixed = -0.99 * 0.0123d * 0.25 * 1_000_000;
    when(mockPeriod.presentValue(FIXED_RATE_PAYMENT_PERIOD_PAY_GBP, mockProv))
        .thenReturn(pvCpnFixed);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    double pvNotional = 980_000d;
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, mockProv))
        .thenReturn(pvNotional);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_PAY_GBP, mockProv))
        .thenReturn(-pvNotional);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ExpandedSwap expanded = SWAP.expand();
    double pvbp = pricerLeg.pvbp(FIXED_EXPANDED_SWAP_LEG_PAY, mockProv);
    double parRateExpected1 = -(pvCpnIbor + -pvNotional + pvNotional) / pvbp;
    double parRateExpected2 = fwdRate;
    double parRateComputed = pricerSwap.parRate(expanded, mockProv);
    assertEquals(parRateComputed, parRateExpected1, TOLERANCE_RATE);
    assertEquals(parRateComputed, parRateExpected2, TOLERANCE_RATE);
  }

  public void test_parRate_crossCurrency() {
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.discountFactor(USD, FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getPaymentDate()))
        .thenReturn(0.99d);
    when(mockProv.getValuationDate()).thenReturn(date(2014, 1, 22));
    when(mockProv.fxRate(GBP, GBP)).thenReturn(1.0);
    when(mockProv.fxRate(USD, USD)).thenReturn(1.0);
    double fxGbpUsd = 1.51d;
    when(mockProv.fxRate(GBP, USD)).thenReturn(fxGbpUsd);
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    double fwdRate = 0.01;
    double pvCpnIborGbp = 0.99 * fwdRate * 0.25 * 1_000_000;
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, mockProv))
        .thenReturn(pvCpnIborGbp);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    double pvNotionalGbp = 980_000d;
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, mockProv))
        .thenReturn(pvNotionalGbp);
    double pvNotionalUsd = -fxGbpUsd * 981_000d;
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_PAY_USD, mockProv))
        .thenReturn(pvNotionalUsd);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    double pvbp = pricerLeg.pvbp(FIXED_EXPANDED_SWAP_LEG_PAY_USD, mockProv);
    double parRateExpected = -((pvCpnIborGbp + pvNotionalGbp) * fxGbpUsd + pvNotionalUsd) / pvbp;
    double parRateComputed = pricerSwap.parRate(expanded, mockProv);
    assertEquals(parRateComputed, parRateExpected, TOLERANCE_RATE);
  }

  public void test_parRate_bothLegFloating() {
    Swap swap = Swap.builder()
        .legs(IBOR_EXPANDED_SWAP_LEG_REC_GBP, IBOR_EXPANDED_SWAP_LEG_REC_GBP)
        .build();
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    assertThrowsIllegalArg(() -> pricerSwap.parRate(swap, MOCK_PROV));
  }

  public void test_parRate_inflation() {
    DiscountingSwapLegPricer pricerLeg = DiscountingSwapLegPricer.DEFAULT;
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(GB_RPI, PRICE_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexValues(map).build();
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .discountCurves(RATES_GBP.getDiscountCurves())
        .dayCount(ACT_ACT_ISDA)
        .build();
    double parRateComputed = pricerSwap.parRate(SWAP_INFLATION, prov);
    RateCalculationSwapLeg fixedLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2014, 6, 9))
            .endDate(date(2019, 6, 9))
            .frequency(Frequency.P12M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.ofYears(5))
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
            .compoundingMethod(CompoundingMethod.STRAIGHT)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL))
        .calculation(FixedRateCalculation.builder()
            .rate(ValueSchedule.of(parRateComputed))
            .dayCount(DayCounts.ONE_ONE) // year fraction is always 1.
            .build())
        .build();
    Swap swapWithParRate = Swap.builder().legs(INFLATION_MONTHLY_SWAP_LEG_REC_GBP, fixedLeg).build();
    double pvWithParRate = pricerSwap.presentValue(swapWithParRate, prov).getAmount(GBP).getAmount();
    assertEquals(pvWithParRate, 0.0d, NOTIONAL * TOLERANCE_RATE);
  }

  public void test_parRate_inflation_periodic() {
    RateCalculationSwapLeg fixedLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2014, 6, 9))
            .endDate(date(2019, 6, 9))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL))
        .calculation(FixedRateCalculation.builder()
            .rate(ValueSchedule.of(0.04))
            .dayCount(DayCounts.ACT_365F)
            .build())
        .build();
    Swap swap = Swap.builder().legs(INFLATION_MONTHLY_SWAP_LEG_REC_GBP, fixedLeg).build();
    DiscountingSwapLegPricer pricerLeg = DiscountingSwapLegPricer.DEFAULT;
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(GB_RPI, PRICE_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexValues(map).build();
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .discountCurves(RATES_GBP.getDiscountCurves())
        .dayCount(ACT_ACT_ISDA)
        .build();
    double parRateComputed = pricerSwap.parRate(swap, prov);
    RateCalculationSwapLeg fixedLegWithParRate = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2014, 6, 9))
            .endDate(date(2019, 6, 9))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL))
        .calculation(FixedRateCalculation.builder()
            .rate(ValueSchedule.of(parRateComputed))
            .dayCount(DayCounts.ACT_365F)
            .build())
        .build();
    Swap swapWithParRate = Swap.builder().legs(INFLATION_MONTHLY_SWAP_LEG_REC_GBP, fixedLegWithParRate).build();
    double pvWithParRate = pricerSwap.presentValue(swapWithParRate, prov).getAmount(GBP).getAmount();
    assertEquals(pvWithParRate, 0.0d, NOTIONAL * TOLERANCE_RATE);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_singleCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(FIXED_RATE_PAYMENT_PERIOD_PAY_GBP, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_REC_GBP, MOCK_PROV))
        .thenReturn(35d);
    when(mockEvent.presentValue(NOTIONAL_EXCHANGE_PAY_GBP, MOCK_PROV))
        .thenReturn(-30d);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ExpandedSwap expanded = SWAP.expand();
    assertEquals(pricerSwap.presentValue(expanded, MOCK_PROV), MultiCurrencyAmount.of(GBP, 505d));

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.presentValue(SWAP_TRADE, MOCK_PROV),
        pricerSwap.presentValue(expanded, MOCK_PROV));
  }

  public void test_presentValue_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(pricerSwap.presentValue(expanded, MOCK_PROV), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.presentValue(SWAP_TRADE_CROSS_CURRENCY, MOCK_PROV),
        pricerSwap.presentValue(expanded, MOCK_PROV));
  }

  public void test_presentValue_withCurrency_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    CurrencyAmount expected = CurrencyAmount.of(USD, 1000d * MockRatesProvider.RATE - 500d);
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(pricerSwap.presentValue(expanded, USD, MOCK_PROV), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.presentValue(SWAP_TRADE_CROSS_CURRENCY, USD, MOCK_PROV),
        pricerSwap.presentValue(expanded, USD, MOCK_PROV));
  }

  public void test_presentValue_inflation() {
    DiscountingSwapLegPricer pricerLeg = DiscountingSwapLegPricer.DEFAULT;
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(GB_RPI, PRICE_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexValues(map).build();
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .discountCurves(RATES_GBP.getDiscountCurves())
        .dayCount(ACT_ACT_ISDA)
        .build();
    LocalDate paymentDate = SWAP_INFLATION.getLegs().get(0).expand().getPaymentPeriods().get(0).getPaymentDate();
    double fixedRate = ((FixedRateCalculation) INFLATION_FIXED_SWAP_LEG_PAY_GBP.getCalculation())
        .getRate().getInitialValue();
    MultiCurrencyAmount pvComputed = pricerSwap.presentValue(SWAP_INFLATION, prov);
    double pvExpected = (-(CONSTANT_INDEX / START_INDEX - 1.0) + Math.pow(1 + fixedRate, 5) - 1.0)
        * NOTIONAL * prov.discountFactor(GBP, paymentDate);
    assertTrue(pvComputed.getCurrencies().size() == 1);
    assertEquals(pvComputed.getAmount(GBP).getAmount(), pvExpected, NOTIONAL * TOLERANCE_RATE);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_singleCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(FIXED_RATE_PAYMENT_PERIOD_PAY_GBP, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ExpandedSwap expanded = SWAP.expand();
    assertEquals(pricerSwap.futureValue(expanded, MOCK_PROV), MultiCurrencyAmount.of(GBP, 500d));

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.futureValue(SWAP_TRADE, MOCK_PROV),
        pricerSwap.futureValue(expanded, MOCK_PROV));
  }

  public void test_futureValue_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, MOCK_PROV))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(pricerSwap.futureValue(expanded, MOCK_PROV), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.futureValue(SWAP_TRADE_CROSS_CURRENCY, MOCK_PROV),
        pricerSwap.futureValue(expanded, MOCK_PROV));
  }

  public void test_futureValue_inflation() {
    DiscountingSwapLegPricer pricerLeg = DiscountingSwapLegPricer.DEFAULT;
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(GB_RPI, PRICE_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexValues(map).build();
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .discountCurves(RATES_GBP.getDiscountCurves())
        .dayCount(ACT_ACT_ISDA)
        .build();
    MultiCurrencyAmount fvComputed = pricerSwap.futureValue(SWAP_INFLATION, prov);
    double fixedRate = ((FixedRateCalculation) INFLATION_FIXED_SWAP_LEG_PAY_GBP.getCalculation())
        .getRate().getInitialValue();
    double fvExpected = (-(CONSTANT_INDEX / START_INDEX - 1.0) + Math.pow(1.0 + fixedRate, 5) - 1.0) * NOTIONAL;
    assertTrue(fvComputed.getCurrencies().size() == 1);
    assertEquals(fvComputed.getAmount(GBP).getAmount(), fvExpected, NOTIONAL * TOLERANCE_RATE);
  }

  //-------------------------------------------------------------------------
  public void test_parRateSensitivity_singleCurrency() {
    ExpandedSwap expanded = SWAP.expand();
    PointSensitivities point = PRICER_SWAP.parRateSensitivity(expanded, RATES_GBP).build();
    CurveParameterSensitivities prAd = RATES_GBP.parameterSensitivity(point);
    CurveParameterSensitivities prFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(
        RATES_GBP, p -> CurrencyAmount.of(GBP, PRICER_SWAP.parRate(expanded, p)));
    assertTrue(prAd.equalWithTolerance(prFd, TOLERANCE_RATE_DELTA));
  }

  public void test_parRateSensitivity_crossCurrency() {
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    PointSensitivities point = PRICER_SWAP.parRateSensitivity(expanded, RATES_GBP_USD).build();
    CurveParameterSensitivities prAd = RATES_GBP_USD.parameterSensitivity(point);
    CurveParameterSensitivities prFd = FINITE_DIFFERENCE_CALCULATOR.sensitivity(
        RATES_GBP_USD, p -> CurrencyAmount.of(USD, PRICER_SWAP.parRate(expanded, p)));
    assertTrue(prAd.equalWithTolerance(prFd, TOLERANCE_RATE_DELTA));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    // ibor leg
    IborRateSensitivity fwdSense =
        IborRateSensitivity.of(GBP_LIBOR_3M, GBP, IBOR_RATE_OBSERVATION.getFixingDate(), 140.0);
    ZeroRateSensitivity dscSense =
        ZeroRateSensitivity.of(GBP, IBOR_RATE_PAYMENT_PERIOD_REC_GBP.getPaymentDate(), -162.0);
    PointSensitivityBuilder sensiFloating = fwdSense.combinedWith(dscSense);
    // fixed leg
    PointSensitivityBuilder sensiFixed =
        ZeroRateSensitivity.of(GBP, IBOR_RATE_PAYMENT_PERIOD_REC_GBP.getPaymentDate(), 152.0);
    // events
    Currency ccy = IBOR_EXPANDED_SWAP_LEG_REC_GBP.getCurrency();
    LocalDate paymentDateEvent = NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate();
    PointSensitivityBuilder sensiEvent = ZeroRateSensitivity.of(ccy, paymentDateEvent, -134.0);
    PointSensitivities expected = sensiFloating.build()
        .combinedWith(sensiEvent.build())
        .combinedWith(sensiFixed.build())
        .combinedWith(sensiEvent.build());

    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockPeriod.presentValueSensitivity(IBOR_EXPANDED_SWAP_LEG_REC_GBP.getPaymentPeriods().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiFloating.build().toMutable());
    when(mockPeriod.presentValueSensitivity(FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentPeriods().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiFixed.build().toMutable());
    when(mockEvent.presentValueSensitivity(IBOR_EXPANDED_SWAP_LEG_REC_GBP.getPaymentEvents().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    when(mockEvent.presentValueSensitivity(FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentEvents().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    PointSensitivities res = pricerSwap.presentValueSensitivity(SWAP, MOCK_PROV).build();

    assertTrue(res.equalWithTolerance(expected, TOLERANCE_RATE));

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.presentValueSensitivity(SWAP_TRADE, MOCK_PROV),
        pricerSwap.presentValueSensitivity(SWAP, MOCK_PROV).build());
  }

  public void test_presentValueSensitivity_inflation() {
    DiscountingSwapLegPricer pricerLeg = DiscountingSwapLegPricer.DEFAULT;
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(GB_RPI, PRICE_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexValues(map).build();
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .discountCurves(RATES_GBP.getDiscountCurves())
        .dayCount(ACT_ACT_ISDA)
        .build();
    PointSensitivityBuilder pvSensiComputed = pricerSwap.presentValueSensitivity(SWAP_INFLATION, prov);
    PointSensitivityBuilder pvSensiInflationLeg =
        pricerLeg.presentValueSensitivity(INFLATION_MONTHLY_SWAP_LEG_REC_GBP, prov);
    PointSensitivityBuilder pvSensiFixedLeg = pricerLeg.presentValueSensitivity(INFLATION_FIXED_SWAP_LEG_PAY_GBP, prov);
    PointSensitivityBuilder pvSensiExpected = pvSensiFixedLeg.combinedWith(pvSensiInflationLeg);
    assertTrue(pvSensiComputed.build().normalized()
        .equalWithTolerance(pvSensiExpected.build().normalized(), TOLERANCE_RATE * NOTIONAL));
  }

  //-------------------------------------------------------------------------
  public void test_futureValueSensitivity() {
    // ibor leg
    PointSensitivityBuilder sensiFloating =
        IborRateSensitivity.of(GBP_LIBOR_3M, GBP, IBOR_RATE_OBSERVATION.getFixingDate(), 140.0);
    // fixed leg
    PointSensitivityBuilder sensiFixed = PointSensitivityBuilder.none();
    // events
    PointSensitivityBuilder sensiEvent = PointSensitivityBuilder.none();
    PointSensitivities expected = sensiFloating.build();

    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockPeriod.futureValueSensitivity(IBOR_EXPANDED_SWAP_LEG_REC_GBP.getPaymentPeriods().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiFloating.build().toMutable());
    when(mockPeriod.futureValueSensitivity(FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentPeriods().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiFixed.build().toMutable());
    when(mockEvent.futureValueSensitivity(IBOR_EXPANDED_SWAP_LEG_REC_GBP.getPaymentEvents().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    when(mockEvent.futureValueSensitivity(FIXED_EXPANDED_SWAP_LEG_PAY.getPaymentEvents().get(0), MOCK_PROV))
        .thenAnswer(t -> sensiEvent.build().toMutable());
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    PointSensitivities res = pricerSwap.futureValueSensitivity(SWAP, MOCK_PROV).build();

    assertTrue(res.equalWithTolerance(expected, TOLERANCE_RATE));

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(
        pricerTrade.futureValueSensitivity(SWAP_TRADE, MOCK_PROV),
        pricerSwap.futureValueSensitivity(SWAP, MOCK_PROV).build());
  }

  public void test_futureValueSensitivity_inflation() {
    DiscountingSwapLegPricer pricerLeg = DiscountingSwapLegPricer.DEFAULT;
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(GB_RPI, PRICE_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexValues(map).build();
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .discountCurves(RATES_GBP.getDiscountCurves())
        .dayCount(ACT_ACT_ISDA)
        .build();
    PointSensitivityBuilder fvSensiComputed = pricerSwap.futureValueSensitivity(SWAP_INFLATION, prov);
    PointSensitivityBuilder fvSensiInflationLeg =
        pricerLeg.futureValueSensitivity(INFLATION_MONTHLY_SWAP_LEG_REC_GBP, prov);
    PointSensitivityBuilder fvSensiFixedLeg = pricerLeg.futureValueSensitivity(INFLATION_FIXED_SWAP_LEG_PAY_GBP, prov);
    PointSensitivityBuilder fvSensiExpected = fvSensiFixedLeg.combinedWith(fvSensiInflationLeg);
    assertTrue(fvSensiComputed.build().normalized()
        .equalWithTolerance(fvSensiExpected.build().normalized(), TOLERANCE_RATE * NOTIONAL));
  }

  //-------------------------------------------------------------------------
  public void test_cashFlows() {
    RatesProvider mockProv = mock(RatesProvider.class);
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    double df1 = 0.98;
    double df2 = 0.93;
    double fvGBP = 1000d;
    double fvUSD = -500d;
    when(mockPeriod.futureValue(IBOR_RATE_PAYMENT_PERIOD_REC_GBP, mockProv)).thenReturn(fvGBP);
    when(mockPeriod.futureValue(FIXED_RATE_PAYMENT_PERIOD_PAY_USD, mockProv)).thenReturn(fvUSD);
    when(mockProv.getValuationDate()).thenReturn(LocalDate.of(2014, 7, 1));
    when(mockProv.discountFactor(IBOR_RATE_PAYMENT_PERIOD_REC_GBP.getCurrency(),
        IBOR_RATE_PAYMENT_PERIOD_REC_GBP.getPaymentDate())).thenReturn(df1);
    when(mockProv.discountFactor(FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getCurrency(),
        FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getPaymentDate())).thenReturn(df2);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer pricerLeg = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    DiscountingSwapProductPricer pricerSwap = new DiscountingSwapProductPricer(pricerLeg);
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();

    CashFlows computed = pricerSwap.cashFlows(expanded, mockProv);
    CashFlow flowGBP = CashFlow.of(IBOR_RATE_PAYMENT_PERIOD_REC_GBP.getPaymentDate(), GBP, fvGBP, df1);
    CashFlow flowUSD = CashFlow.of(FIXED_RATE_PAYMENT_PERIOD_PAY_USD.getPaymentDate(), USD, fvUSD, df2);
    CashFlows expected = CashFlows.of(ImmutableList.of(flowGBP, flowUSD));
    assertEquals(computed, expected);

    // test via SwapTrade
    DiscountingSwapTradePricer pricerTrade = new DiscountingSwapTradePricer(pricerSwap);
    assertEquals(pricerTrade.cashFlows(SWAP_TRADE, MOCK_PROV), pricerSwap.cashFlows(expanded, MOCK_PROV));
  }

}
