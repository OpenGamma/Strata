/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.pricer.impl.rate.swap.CashFlowEquivalentCalculator;
import com.opengamma.strata.pricer.index.HullWhiteIborFutureDataSet;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParameters;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParametersProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swap.SwapPaymentEventPricer;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapPaymentEvent;
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * Test {@link HullWhiteSwaptionPhysicalProductPricer}. 
 */
public class HullWhiteSwaptionPhysicalProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ZonedDateTime MATURITY = dateUtc(2016, 7, 7);
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CALENDAR);
  private static final LocalDate SETTLE =
      BDA_MF.adjust(CALENDAR.resolve(REF_DATA).shift(MATURITY.toLocalDate(), 2), REF_DATA);
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
  private static final ResolvedSwap RSWAP_REC = SWAP_REC.resolve(REF_DATA);
  private static final Swap SWAP_PAY = Swap.of(FIXED_LEG_PAY, IBOR_LEG_REC);
  private static final ResolvedSwap RSWAP_PAY = SWAP_PAY.resolve(REF_DATA);
  private static final CashSwaptionSettlement PAR_YIELD =
      CashSwaptionSettlement.of(SETTLE, CashSwaptionSettlementMethod.PAR_YIELD);
  private static final ResolvedSwaption SWAPTION_REC_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
      .longShort(LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
      .longShort(SHORT)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
      .longShort(LONG)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate(), BDA_MF))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
      .longShort(SHORT)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_CASH = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY.toLocalDate()))
      .expiryTime(MATURITY.toLocalTime())
      .expiryZone(MATURITY.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PAR_YIELD)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);

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
  @Test
  public void validate_physical_settlement() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.presentValue(SWAPTION_CASH, RATE_PROVIDER, HW_PROVIDER));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    CurrencyAmount computedRec = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    CurrencyAmount computedPay = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    SwapPaymentEventPricer<SwapPaymentEvent> paymentEventPricer = SwapPaymentEventPricer.standard();
    ResolvedSwapLeg cashFlowEquiv = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(RSWAP_REC, RATE_PROVIDER);
    LocalDate expiryDate = MATURITY.toLocalDate();
    int nPayments = cashFlowEquiv.getPaymentEvents().size();
    double[] alpha = new double[nPayments];
    double[] discountedCashFlow = new double[nPayments];
    for (int loopcf = 0; loopcf < nPayments; loopcf++) {
      SwapPaymentEvent payment = cashFlowEquiv.getPaymentEvents().get(loopcf);
      alpha[loopcf] = HW_PROVIDER.alpha(RATE_PROVIDER.getValuationDate(), expiryDate, expiryDate, payment.getPaymentDate());
      discountedCashFlow[loopcf] = paymentEventPricer.presentValue(payment, RATE_PROVIDER);
    }
    double omegaPay = -1d;
    double kappa = HW_PROVIDER.getModel().kappa(DoubleArray.copyOf(discountedCashFlow), DoubleArray.copyOf(alpha));
    double expectedRec = 0.0;
    double expectedPay = 0.0;
    for (int loopcf = 0; loopcf < nPayments; loopcf++) {
      expectedRec += discountedCashFlow[loopcf] * NORMAL.getCDF((kappa + alpha[loopcf]));
      expectedPay += discountedCashFlow[loopcf] * NORMAL.getCDF(omegaPay * (kappa + alpha[loopcf]));
    }
    assertThat(computedRec.getCurrency()).isEqualTo(EUR);
    assertThat(computedRec.getAmount()).isCloseTo(expectedRec, offset(NOTIONAL * TOL));
    assertThat(computedPay.getCurrency()).isEqualTo(EUR);
    assertThat(computedPay.getAmount()).isCloseTo(expectedPay, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValue_atMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    double swapPv = SWAP_PRICER.presentValue(RSWAP_REC, RATES_PROVIDER_AT_MATURITY).getAmount(EUR).getAmount();
    assertThat(computedRec.getAmount()).isCloseTo(swapPv, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValue_afterExpiry() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    assertThat(computedRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValue_parity() {
    CurrencyAmount pvRecLong = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    CurrencyAmount pvRecShort = PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, HW_PROVIDER);
    CurrencyAmount pvPayLong = PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER);
    CurrencyAmount pvPayShort = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    assertThat(pvRecLong.getAmount()).isCloseTo(-pvRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvPayLong.getAmount()).isCloseTo(-pvPayShort.getAmount(), offset(NOTIONAL * TOL));
    double swapPv = SWAP_PRICER.presentValue(RSWAP_PAY, RATE_PROVIDER).getAmount(EUR).getAmount();
    assertThat(pvPayLong.getAmount() - pvRecLong.getAmount()).isCloseTo(swapPv, offset(NOTIONAL * TOL));
    assertThat(pvPayShort.getAmount() - pvRecShort.getAmount()).isCloseTo(-swapPv, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRates(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER));
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(EUR).getAmount()).isCloseTo(expectedRec.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRates(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(EUR).getAmount()).isCloseTo(expectedPay.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_currencyExposure_atMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRates(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY));
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(EUR).getAmount()).isCloseTo(expectedRec.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRates(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(EUR).getAmount()).isCloseTo(expectedPay.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_currencyExposure_afterMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(EUR).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(EUR).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRates(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    CurrencyParameterSensitivities computedRec = RATE_PROVIDER.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), HW_PROVIDER));
    assertThat(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_TOL * 1000d)).isTrue();
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRates(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay.build());
    CurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_PAY_SHORT, (p), HW_PROVIDER));
    assertThat(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_TOL * 1000d)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_atMaturity() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRates(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    CurrencyParameterSensitivities computedRec =
        RATES_PROVIDER_AT_MATURITY.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec = FD_CAL.sensitivity(
        RATES_PROVIDER_AT_MATURITY, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), HW_PROVIDER_AT_MATURITY));
    assertThat(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_TOL * 1000d)).isTrue();
    PointSensitivities pointPay = PRICER.presentValueSensitivityRates(SWAPTION_PAY_SHORT,
        RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
  }

  @Test
  public void test_presentValueSensitivity_afterMaturity() {
    PointSensitivities pointRec = PRICER.presentValueSensitivityRates(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointRec.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
    PointSensitivities pointPay = PRICER.presentValueSensitivityRates(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
  }

  @Test
  public void test_presentValueSensitivity_parity() {
    CurrencyParameterSensitivities pvSensiRecLong = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRates(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER).build());
    CurrencyParameterSensitivities pvSensiRecShort = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRates(SWAPTION_REC_SHORT, RATE_PROVIDER, HW_PROVIDER).build());
    CurrencyParameterSensitivities pvSensiPayLong = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRates(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER).build());
    CurrencyParameterSensitivities pvSensiPayShort = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRates(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER).build());
    assertThat(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();
    assertThat(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();
    PointSensitivities expectedPoint = SWAP_PRICER.presentValueSensitivity(RSWAP_PAY, RATE_PROVIDER).build();
    CurrencyParameterSensitivities expected = RATE_PROVIDER.parameterSensitivity(expectedPoint);
    assertThat(expected.equalWithTolerance(pvSensiPayLong.combinedWith(pvSensiRecLong.multipliedBy(-1d)), NOTIONAL * TOL)).isTrue();
    assertThat(expected.equalWithTolerance(pvSensiRecShort.combinedWith(pvSensiPayShort.multipliedBy(-1d)), NOTIONAL * TOL)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivityHullWhiteParameter() {
    DoubleArray computedRec =
        PRICER.presentValueSensitivityModelParamsHullWhite(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    DoubleArray computedPay =
        PRICER.presentValueSensitivityModelParamsHullWhite(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
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
    assertThat(DoubleArrayMath.fuzzyEquals(computedRec.toArray(), expectedRec, NOTIONAL * FD_TOL)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(computedPay.toArray(), expectedPay, NOTIONAL * FD_TOL)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityHullWhiteParameter_atMaturity() {
    DoubleArray pvSensiRec = PRICER.presentValueSensitivityModelParamsHullWhite(
        SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    assertThat(pvSensiRec.equalZeroWithTolerance(NOTIONAL * TOL)).isTrue();
    DoubleArray pvSensiPay = PRICER.presentValueSensitivityModelParamsHullWhite(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, HW_PROVIDER_AT_MATURITY);
    assertThat(pvSensiPay.equalZeroWithTolerance(NOTIONAL * TOL)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityHullWhiteParameter_afterMaturity() {
    DoubleArray pvSensiRec = PRICER.presentValueSensitivityModelParamsHullWhite(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    assertThat(pvSensiRec.equalZeroWithTolerance(NOTIONAL * TOL)).isTrue();
    DoubleArray pvSensiPay = PRICER.presentValueSensitivityModelParamsHullWhite(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, HW_PROVIDER_AFTER_MATURITY);
    assertThat(pvSensiPay.equalZeroWithTolerance(NOTIONAL * TOL)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityHullWhiteParameter_parity() {
    DoubleArray pvSensiRecLong =
        PRICER.presentValueSensitivityModelParamsHullWhite(SWAPTION_REC_LONG, RATE_PROVIDER, HW_PROVIDER);
    DoubleArray pvSensiRecShort =
        PRICER.presentValueSensitivityModelParamsHullWhite(SWAPTION_REC_SHORT, RATE_PROVIDER, HW_PROVIDER);
    DoubleArray pvSensiPayLong =
        PRICER.presentValueSensitivityModelParamsHullWhite(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER);
    DoubleArray pvSensiPayShort =
        PRICER.presentValueSensitivityModelParamsHullWhite(SWAPTION_PAY_SHORT, RATE_PROVIDER, HW_PROVIDER);
    assertThat(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();
    assertThat(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();
    assertThat(pvSensiPayLong.equalWithTolerance(pvSensiRecLong, NOTIONAL * TOL)).isTrue();
    assertThat(pvSensiRecShort.equalWithTolerance(pvSensiPayShort, NOTIONAL * TOL)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void regression_pv() {
    CurrencyAmount pv = PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER);
    assertThat(pv.getAmount()).isCloseTo(4213670.335092038, offset(NOTIONAL * TOL));
  }

  @Test
  public void regression_curveSensitivity() {
    PointSensitivities point = PRICER.presentValueSensitivityRates(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER).build();
    CurrencyParameterSensitivities computed = RATE_PROVIDER.parameterSensitivity(point);
    double[] dscExp = new double[] {0.0, 0.0, 0.0, 0.0, -1.4127023229222856E7, -1.744958350376594E7};
    double[] fwdExp = new double[] {0.0, 0.0, 0.0, 0.0, -2.0295973516660026E8, 4.12336887967829E8};
    assertThat(DoubleArrayMath.fuzzyEquals(computed.getSensitivity(HullWhiteIborFutureDataSet.DSC_NAME, EUR)
        .getSensitivity().toArray(), dscExp, NOTIONAL * TOL)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(computed.getSensitivity(HullWhiteIborFutureDataSet.FWD6_NAME, EUR)
        .getSensitivity().toArray(), fwdExp, NOTIONAL * TOL)).isTrue();
  }

  @Test
  public void regression_hullWhiteSensitivity() {
    DoubleArray computed = PRICER.presentValueSensitivityModelParamsHullWhite(SWAPTION_PAY_LONG, RATE_PROVIDER, HW_PROVIDER);
    double[] expected = new double[] {
        2.9365484063149095E7, 3.262667329294093E7, 7.226220286364576E7, 2.4446925038968167E8, 120476.73820821749};
    assertThat(DoubleArrayMath.fuzzyEquals(computed.toArray(), expected, NOTIONAL * TOL)).isTrue();
  }
}
