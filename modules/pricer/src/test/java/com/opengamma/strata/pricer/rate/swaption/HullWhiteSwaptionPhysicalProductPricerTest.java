/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.LongShort.SHORT;
import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.pricer.impl.rate.model.HullWhiteOneFactorPiecewiseConstantParameters;
import com.opengamma.strata.pricer.impl.rate.swap.CashFlowEquivalentCalculator;
import com.opengamma.strata.pricer.index.HullWhiteIborFutureDataSet;
import com.opengamma.strata.pricer.index.HullWhiteOneFactorPiecewiseConstantParametersProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swap.PaymentPeriodPricer;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.KnownAmountPaymentPeriod;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentPeriod;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swaption.CashSettlement;
import com.opengamma.strata.product.swaption.CashSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSettlement;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * Test {@link HullWhiteSwaptionPhysicalProductPricer}. 
 */
@Test
public class HullWhiteSwaptionPhysicalProductPricerTest {

  private static final ZonedDateTime MATURITY = dateUtc(2016, 7, 7);
  private static final HolidayCalendar CALENDAR = HolidayCalendars.SAT_SUN;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CALENDAR);
  private static final LocalDate SETTLE = BDA_MF.adjust(CALENDAR.shift(MATURITY.toLocalDate(), 2));
  private static final double NOTIONAL = 100000000; //100m
  private static final int TENOR_YEAR = 5;
  private static final LocalDate END = SETTLE.plusYears(TENOR_YEAR);
  private static final double RATE = 0.0175;
  private static final PeriodicSchedule PERIOD_FIXED = PeriodicSchedule.builder()
      .startDate(SETTLE)
      .endDate(END)
      .frequency(P12M)
      .businessDayAdjustment(BDA_MF)
      .stubConvention(StubConvention.SHORT_FINAL)
      .rollConvention(RollConventions.EOM)
      .build();
  private static final PaymentSchedule PAYMENT_FIXED = PaymentSchedule.builder()
      .paymentFrequency(P12M)
      .paymentDateOffset(DaysAdjustment.NONE)
      .build();
  private static final FixedRateCalculation RATE_FIXED = FixedRateCalculation.builder()
      .dayCount(THIRTY_U_360)
      .rate(ValueSchedule.of(RATE))
      .build();
  private static final PeriodicSchedule PERIOD_IBOR = PeriodicSchedule.builder()
      .startDate(SETTLE)
      .endDate(END)
      .frequency(P6M)
      .businessDayAdjustment(BDA_MF)
      .stubConvention(StubConvention.SHORT_FINAL)
      .rollConvention(RollConventions.EOM)
      .build();
  private static final PaymentSchedule PAYMENT_IBOR = PaymentSchedule.builder()
      .paymentFrequency(P6M)
      .paymentDateOffset(DaysAdjustment.NONE)
      .build();
  private static final IborRateCalculation RATE_IBOR = IborRateCalculation.builder()
      .index(EUR_EURIBOR_6M)
      .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CALENDAR, BDA_MF))
      .build();
  private static final SwapLeg FIXED_LEG_REC = RateCalculationSwapLeg.builder()
      .payReceive(RECEIVE)
      .accrualSchedule(PERIOD_FIXED)
      .paymentSchedule(PAYMENT_FIXED)
      .notionalSchedule(NotionalSchedule.of(EUR, NOTIONAL))
      .calculation(RATE_FIXED)
      .build();
  private static final SwapLeg FIXED_LEG_PAY = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(PERIOD_FIXED)
      .paymentSchedule(PAYMENT_FIXED)
      .notionalSchedule(NotionalSchedule.of(EUR, NOTIONAL))
      .calculation(RATE_FIXED)
      .build();
  private static final SwapLeg IBOR_LEG_REC = RateCalculationSwapLeg.builder()
      .payReceive(RECEIVE)
      .accrualSchedule(PERIOD_IBOR)
      .paymentSchedule(PAYMENT_IBOR)
      .notionalSchedule(NotionalSchedule.of(EUR, NOTIONAL))
      .calculation(RATE_IBOR)
      .build();
  private static final SwapLeg IBOR_LEG_PAY = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(PERIOD_IBOR)
      .paymentSchedule(PAYMENT_IBOR)
      .notionalSchedule(NotionalSchedule.of(EUR, NOTIONAL))
      .calculation(RATE_IBOR)
      .build();
  private static final Swap SWAP_REC = Swap.of(FIXED_LEG_REC, IBOR_LEG_PAY);
  private static final Swap SWAP_PAY = Swap.of(FIXED_LEG_PAY, IBOR_LEG_REC);
  private static final CashSettlement PAR_YIELD = CashSettlement.builder()
      .cashSettlementMethod(CashSettlementMethod.PAR_YIELD)
      .settlementDate(SETTLE)
      .build();
  private static final Swaption SWAPTION_REC_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PhysicalSettlement.DEFAULT)
      .longShort(LONG)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_REC_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PhysicalSettlement.DEFAULT)
      .longShort(SHORT)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_PAY_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PhysicalSettlement.DEFAULT)
      .longShort(LONG)
      .underlying(SWAP_PAY)
      .build();
  private static final Swaption SWAPTION_PAY_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PhysicalSettlement.DEFAULT)
      .longShort(SHORT)
      .underlying(SWAP_PAY)
      .build();
  private static final Swaption SWAPTION_CASH = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate()))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PAR_YIELD)
      .underlying(SWAP_REC)
      .build();

  private static final LocalDate VALUATION = LocalDate.of(2011, 7, 7);
  private static final HullWhiteOneFactorPiecewiseConstantParametersProvider HW_PROVIDER =
      HullWhiteIborFutureDataSet.createHullWhiteProvider(VALUATION);
  private static final HullWhiteOneFactorPiecewiseConstantParametersProvider HW_PROVIDER_AT_MATURITY =
      HullWhiteIborFutureDataSet.createHullWhiteProvider(MATURITY.toLocalDate());
  private static final HullWhiteOneFactorPiecewiseConstantParametersProvider HW_PROVIDER_AFTER_MATURITY =
      HullWhiteIborFutureDataSet.createHullWhiteProvider(MATURITY.toLocalDate().plusDays(1));
  private static final ImmutableRatesProvider RATE_PROVIDER = HullWhiteIborFutureDataSet.createRatesProvider(VALUATION);
  private static final ImmutableRatesProvider RATES_PROVIDER_AT_MATURITY = HullWhiteIborFutureDataSet
      .createRatesProvider(MATURITY.toLocalDate());
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER_MATURITY = HullWhiteIborFutureDataSet
      .createRatesProvider(MATURITY.toLocalDate().plusDays(1));

  private static final double TOL = 1.0e-12;
  private static final double FD_TOL = 1.0e-7;
  private static final HullWhiteSwaptionPhysicalProductPricer PRICER = HullWhiteSwaptionPhysicalProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_TOL);
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);

  //-------------------------------------------------------------------------
  public void validate_physical_settlement() {
    assertThrowsIllegalArg(() -> PRICER.presentValue(SWAPTION_CASH, RATE_PROVIDER, HW_PROVIDER));
  }

  public void test_noFixedLeg() {
    Swap swap = Swap.of(IBOR_LEG_REC, IBOR_LEG_PAY);
    Swaption swaption = Swaption
        .builder()
        .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
        .expiryTime(LocalTime.NOON)
        .expiryZone(ZoneOffset.UTC)
        .swaptionSettlement(PAR_YIELD)
        .longShort(LONG)
        .underlying(swap)
        .build();
    assertThrowsIllegalArg(() -> PRICER.presentValue(swaption, RATE_PROVIDER, HW_PROVIDER));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount computedRec = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    CurrencyAmount computedPay = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    PaymentPeriodPricer<PaymentPeriod> paymentPeriodPricer = PaymentPeriodPricer.instance();
    ExpandedSwapLeg cashFlowEquiv = CashFlowEquivalentCalculator.cashFlowEquivalent(SWAP_REC);
    LocalDate expiryDate = MATURITY.toLocalDate();
    int nPayments = cashFlowEquiv.getPaymentPeriods().size();
    double[] alpha = new double[nPayments];
    double[] discountedCashFlow = new double[nPayments];
    for (int loopcf = 0; loopcf < nPayments; loopcf++) {
      PaymentPeriod payment = cashFlowEquiv.getPaymentPeriods().get(loopcf);
      LocalDate paymentDate = payment instanceof KnownAmountPaymentPeriod ? payment.getPaymentDate()
          : ((RatePaymentPeriod) payment).getAccrualPeriods().get(0).getStartDate();
      alpha[loopcf] = HW_PROVIDER.alpha(RATE_PROVIDER.getValuationDate(), expiryDate, expiryDate, paymentDate);
      discountedCashFlow[loopcf] = paymentPeriodPricer.presentValueCashFlowEquivalent(payment, RATE_PROVIDER);
    }
    double omegaPay = -1d;
    double kappa = HW_PROVIDER.getModel().kappa(DoubleArray.copyOf(discountedCashFlow), DoubleArray.copyOf(alpha));
    double expectedRec = 0.0;
    double expectedPay = 0.0;
    for (int loopcf = 0; loopcf < nPayments; loopcf++) {
      expectedRec += discountedCashFlow[loopcf] * NORMAL.getCDF((kappa + alpha[loopcf]));
      expectedPay += discountedCashFlow[loopcf] * NORMAL.getCDF(omegaPay * (kappa + alpha[loopcf]));
    }
    assertEquals(computedRec.getCurrency(), EUR);
    assertEquals(computedRec.getAmount(), expectedRec, NOTIONAL * TOL);
    assertEquals(computedPay.getCurrency(), EUR);
    assertEquals(computedPay.getAmount(), expectedPay, NOTIONAL * TOL);
  }

  public void test_presentValue_atMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    double swapPv = SWAP_PRICER.presentValue(SWAP_REC, RATES_PROVIDER_AT_MATURITY).getAmount(EUR).getAmount();
    assertEquals(computedRec.getAmount(), swapPv, NOTIONAL * TOL);
    assertEquals(computedPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_afterExpiry() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    assertEquals(computedRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_parity() {
    CurrencyAmount pvRecLong = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    CurrencyAmount pvRecShort = PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, HW_PROVIDER);
    CurrencyAmount pvPayLong = PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER);
    CurrencyAmount pvPayShort = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    assertEquals(pvRecLong.getAmount(), -pvRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvPayLong.getAmount(), -pvPayShort.getAmount(), NOTIONAL * TOL);
    double swapPv = SWAP_PRICER.presentValue(SWAP_PAY, RATE_PROVIDER).getAmount(EUR).getAmount();
    assertEquals(pvPayLong.getAmount() - pvRecLong.getAmount(), swapPv, NOTIONAL * TOL);
    assertEquals(pvPayShort.getAmount() - pvRecShort.getAmount(), -swapPv, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(EUR).getAmount(), expectedRec.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(EUR).getAmount(), expectedPay.getAmount(EUR).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_atMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(EUR).getAmount(), expectedRec.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(EUR).getAmount(), expectedPay.getAmount(EUR).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_afterMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(EUR).getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(EUR).getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    CurveCurrencyParameterSensitivities computedRec = RATE_PROVIDER.curveParameterSensitivity(pointRec.build());
    CurveCurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), HW_PROVIDER));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_TOL * 1000d));
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    CurveCurrencyParameterSensitivities computedPay = RATE_PROVIDER.curveParameterSensitivity(pointPay.build());
    CurveCurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_PAY_SHORT, (p), HW_PROVIDER));
    assertTrue(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_TOL * 1000d));
  }

  public void test_presentValueSensitivity_atMaturity() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    CurveCurrencyParameterSensitivities computedRec =
        RATES_PROVIDER_AT_MATURITY.curveParameterSensitivity(pointRec.build());
    CurveCurrencyParameterSensitivities expectedRec = FD_CAL.sensitivity(
        RATES_PROVIDER_AT_MATURITY, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), HW_PROVIDER_AT_MATURITY));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_TOL * 1000d));
    PointSensitivities pointPay = PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT,
        RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
  }

  public void test_presentValueSensitivity_afterMaturity() {
    PointSensitivities pointRec = PRICER.presentValueSensitivity(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointRec.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
    PointSensitivities pointPay = PRICER.presentValueSensitivity(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
  }

  public void test_presentValueSensitivity_parity() {
    CurveCurrencyParameterSensitivities pvSensiRecLong = RATE_PROVIDER.curveParameterSensitivity(
        PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER).build());
    CurveCurrencyParameterSensitivities pvSensiRecShort = RATE_PROVIDER.curveParameterSensitivity(
        PRICER.presentValueSensitivity(SWAPTION_REC_SHORT, RATE_PROVIDER, HW_PROVIDER).build());
    CurveCurrencyParameterSensitivities pvSensiPayLong = RATE_PROVIDER.curveParameterSensitivity(
        PRICER.presentValueSensitivity(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER).build());
    CurveCurrencyParameterSensitivities pvSensiPayShort = RATE_PROVIDER.curveParameterSensitivity(
        PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER).build());
    assertTrue(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL));
    assertTrue(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL));
    PointSensitivities expecedPoint = SWAP_PRICER.presentValueSensitivity(SWAP_PAY, RATE_PROVIDER).build();
    CurveCurrencyParameterSensitivities expected = RATE_PROVIDER.curveParameterSensitivity(expecedPoint);
    assertTrue(expected.equalWithTolerance(pvSensiPayLong.combinedWith(pvSensiRecLong.multipliedBy(-1d)), NOTIONAL * TOL));
    assertTrue(expected.equalWithTolerance(pvSensiRecShort.combinedWith(pvSensiPayShort.multipliedBy(-1d)), NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityHullWhiteParameter() {
    DoubleArray computedRec =
        PRICER.presentValueSensitivityHullWhiteParameter(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    DoubleArray computedPay =
        PRICER.presentValueSensitivityHullWhiteParameter(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    DoubleArray vols = HW_PROVIDER.getParameters().getVolatility();
    int size = vols.size();
    double[] expectedRec = new double[size];
    double[] expectedPay = new double[size];
    for (int i = 0; i < size; ++i) {
      double[] volsUp = vols.toArray();
      double[] volsDw = vols.toArray();
      volsUp[i] += FD_TOL;
      volsDw[i] -= FD_TOL;
      HullWhiteOneFactorPiecewiseConstantParameters paramsUp = HullWhiteOneFactorPiecewiseConstantParameters.of(
          HW_PROVIDER.getParameters().getMeanReversion(), DoubleArray.copyOf(volsUp), HW_PROVIDER.getParameters()
              .getVolatilityTime().subArray(1, size));
      HullWhiteOneFactorPiecewiseConstantParameters paramsDw = HullWhiteOneFactorPiecewiseConstantParameters.of(
          HW_PROVIDER.getParameters().getMeanReversion(), DoubleArray.copyOf(volsDw), HW_PROVIDER.getParameters()
              .getVolatilityTime().subArray(1, size));
      HullWhiteOneFactorPiecewiseConstantParametersProvider provUp = HullWhiteOneFactorPiecewiseConstantParametersProvider
          .of(paramsUp, HW_PROVIDER.getDayCount(), HW_PROVIDER.getValuationDateTime());
      HullWhiteOneFactorPiecewiseConstantParametersProvider provDw = HullWhiteOneFactorPiecewiseConstantParametersProvider
          .of(paramsDw, HW_PROVIDER.getDayCount(), HW_PROVIDER.getValuationDateTime());
      expectedRec[i] = 0.5 * (PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, provUp).getAmount() -
          PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, provDw).getAmount()) / FD_TOL;
      expectedPay[i] = 0.5 * (PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, provUp).getAmount() -
          PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, provDw).getAmount()) / FD_TOL;
    }
    assertTrue(DoubleArrayMath.fuzzyEquals(computedRec.toArray(), expectedRec, NOTIONAL * FD_TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(computedPay.toArray(), expectedPay, NOTIONAL * FD_TOL));
  }

  public void test_presentValueSensitivityHullWhiteParameter_atMaturity() {
    DoubleArray pvSensiRec = PRICER.presentValueSensitivityHullWhiteParameter(
        SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    assertTrue(pvSensiRec.equalZeroWithTolerance(NOTIONAL * TOL));
    DoubleArray pvSensiPay = PRICER.presentValueSensitivityHullWhiteParameter(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    assertTrue(pvSensiPay.equalZeroWithTolerance(NOTIONAL * TOL));
  }

  public void test_presentValueSensitivityHullWhiteParameter_afterMaturity() {
    DoubleArray pvSensiRec = PRICER.presentValueSensitivityHullWhiteParameter(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    assertTrue(pvSensiRec.equalZeroWithTolerance(NOTIONAL * TOL));
    DoubleArray pvSensiPay = PRICER.presentValueSensitivityHullWhiteParameter(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    assertTrue(pvSensiPay.equalZeroWithTolerance(NOTIONAL * TOL));
  }

  public void test_presentValueSensitivityHullWhiteParameter_parity() {
    DoubleArray pvSensiRecLong =
        PRICER.presentValueSensitivityHullWhiteParameter(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    DoubleArray pvSensiRecShort =
        PRICER.presentValueSensitivityHullWhiteParameter(SWAPTION_REC_SHORT, RATE_PROVIDER, HW_PROVIDER);
    DoubleArray pvSensiPayLong =
        PRICER.presentValueSensitivityHullWhiteParameter(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER);
    DoubleArray pvSensiPayShort =
        PRICER.presentValueSensitivityHullWhiteParameter(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    assertTrue(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL));
    assertTrue(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL));
    assertTrue(pvSensiPayLong.equalWithTolerance(pvSensiRecLong, NOTIONAL * TOL));
    assertTrue(pvSensiRecShort.equalWithTolerance(pvSensiPayShort, NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void regression_pv() {
    CurrencyAmount pv = PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER);
    assertEquals(pv.getAmount(), 4213670.335092038, NOTIONAL * TOL);
  }

  public void regression_curveSensitivity() {
    PointSensitivities point = PRICER.presentValueSensitivity(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER).build();
    CurveCurrencyParameterSensitivities computed = RATE_PROVIDER.curveParameterSensitivity(point);
    double[] dscExp = new double[] {0.0, 0.0, 0.0, 0.0, -1.4127023229222856E7, -1.744958350376594E7 };
    double[] fwdExp = new double[] {0.0, 0.0, 0.0, 0.0, -2.0295973516660026E8, 4.12336887967829E8 };
    assertTrue(DoubleArrayMath.fuzzyEquals(computed.getSensitivity(HullWhiteIborFutureDataSet.DSC_NAME, EUR)
        .getSensitivity().toArray(), dscExp, NOTIONAL * TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(computed.getSensitivity(HullWhiteIborFutureDataSet.FWD6_NAME, EUR)
        .getSensitivity().toArray(), fwdExp, NOTIONAL * TOL));
  }

  public void regression_hullWhiteSensitivity() {
    DoubleArray computed = PRICER.presentValueSensitivityHullWhiteParameter(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER);
    double[] expected = new double[] {
      2.9365484063149095E7, 3.262667329294093E7, 7.226220286364576E7, 2.4446925038968167E8, 120476.73820821749 };
    assertTrue(DoubleArrayMath.fuzzyEquals(computed.toArray(), expected, NOTIONAL * TOL));
  }
}
