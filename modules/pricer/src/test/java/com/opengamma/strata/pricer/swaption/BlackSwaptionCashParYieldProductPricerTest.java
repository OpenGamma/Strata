/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * Test {@link BlackSwaptionCashParYieldProductPricer}.
 */
public class BlackSwaptionCashParYieldProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = LocalDate.of(2012, 1, 10);
  // curve
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final DoubleArray DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray DSC_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  private static final CurveName DSC_NAME = CurveName.of("EUR Dsc");
  private static final CurveMetadata META_DSC = Curves.zeroRates(DSC_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve DSC_CURVE =
      InterpolatedNodalCurve.of(META_DSC, DSC_TIME, DSC_RATE, INTERPOLATOR);
  private static final DoubleArray FWD6_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray FWD6_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  private static final CurveName FWD6_NAME = CurveName.of("EUR EURIBOR 6M");
  private static final CurveMetadata META_FWD6 = Curves.zeroRates(FWD6_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve FWD6_CURVE =
      InterpolatedNodalCurve.of(META_FWD6, FWD6_TIME, FWD6_RATE, INTERPOLATOR);
  private static final ImmutableRatesProvider RATE_PROVIDER = ImmutableRatesProvider.builder(VAL_DATE)
      .discountCurve(EUR, DSC_CURVE)
      .iborIndexCurve(EUR_EURIBOR_6M, FWD6_CURVE)
      .build();
  // surface
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray EXPIRY = DoubleArray.of(0.5, 0.5, 1.0, 1.0, 5.0, 5.0);
  private static final DoubleArray TENOR = DoubleArray.of(2, 10, 2, 10, 2, 10);
  private static final DoubleArray VOL = DoubleArray.of(0.35, 0.30, 0.34, 0.25, 0.25, 0.20);
  private static final FixedIborSwapConvention SWAP_CONVENTION = FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
  private static final SurfaceMetadata METADATA = Surfaces.blackVolatilityByExpiryTenor("Black Vol", ACT_ACT_ISDA);
  private static final Surface SURFACE = InterpolatedNodalSurface.of(METADATA, EXPIRY, TENOR, VOL, INTERPOLATOR_2D);
  private static final BlackSwaptionExpiryTenorVolatilities VOLS =
      BlackSwaptionExpiryTenorVolatilities.of(SWAP_CONVENTION, VAL_DATE.atStartOfDay(ZoneOffset.UTC), SURFACE);
  // underlying swap and swaption
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CALENDAR);
  private static final LocalDate MATURITY = BDA_MF.adjust(VAL_DATE.plusMonths(26), REF_DATA);
  private static final LocalDate SETTLE = BDA_MF.adjust(CALENDAR.resolve(REF_DATA).shift(MATURITY, 2), REF_DATA);
  private static final double NOTIONAL = 123456789.0;
  private static final LocalDate END = SETTLE.plusYears(5);
  private static final double RATE = 0.02;
  private static final PeriodicSchedule PERIOD_FIXED = PeriodicSchedule.builder()
      .startDate(SETTLE)
      .endDate(END)
      .frequency(P12M)
      .businessDayAdjustment(BDA_MF)
      .stubConvention(StubConvention.SHORT_FINAL)
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
  private static final ResolvedSwapLeg RFIXED_LEG_REC = FIXED_LEG_REC.resolve(REF_DATA);
  private static final CashSwaptionSettlement PAR_YIELD =
      CashSwaptionSettlement.of(SETTLE, CashSwaptionSettlementMethod.PAR_YIELD);
  private static final ResolvedSwaption SWAPTION_REC_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
      .expiryTime(LocalTime.NOON)
      .expiryZone(ZoneOffset.UTC)
      .swaptionSettlement(PAR_YIELD)
      .longShort(LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
      .expiryTime(LocalTime.NOON)
      .expiryZone(ZoneOffset.UTC)
      .swaptionSettlement(PAR_YIELD)
      .longShort(SHORT)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
      .expiryTime(LocalTime.NOON)
      .expiryZone(ZoneOffset.UTC)
      .swaptionSettlement(PAR_YIELD)
      .longShort(LONG)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
      .expiryTime(LocalTime.NOON)
      .expiryZone(ZoneOffset.UTC)
      .swaptionSettlement(PAR_YIELD)
      .longShort(SHORT)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  // providers used for specific tests
  private static final ImmutableRatesProvider RATES_PROVIDER_AT_MATURITY = ImmutableRatesProvider.builder(MATURITY)
      .discountCurve(EUR, DSC_CURVE)
      .iborIndexCurve(EUR_EURIBOR_6M, FWD6_CURVE)
      .build();
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER_MATURITY =
      ImmutableRatesProvider.builder(MATURITY.plusDays(1))
          .discountCurve(EUR, DSC_CURVE)
          .iborIndexCurve(EUR_EURIBOR_6M, FWD6_CURVE)
          .build();
  private static final BlackSwaptionExpiryTenorVolatilities VOLS_AT_MATURITY =
      BlackSwaptionExpiryTenorVolatilities.of(SWAP_CONVENTION, MATURITY.atStartOfDay(ZoneOffset.UTC), SURFACE);
  private static final BlackSwaptionExpiryTenorVolatilities VOLS_AFTER_MATURITY =
      BlackSwaptionExpiryTenorVolatilities.of(SWAP_CONVENTION, MATURITY.plusDays(1).atStartOfDay(ZoneOffset.UTC), SURFACE);
  // test parameters
  private static final double TOL = 1.0e-12;
  private static final double FD_EPS = 1.0e-7;
  // pricer
  private static final BlackSwaptionCashParYieldProductPricer PRICER = BlackSwaptionCashParYieldProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  @Test
  public void test_presentValue() {
    CurrencyAmount computedRec = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPay = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    double tenor = VOLS.tenor(SETTLE, END);
    double volatility = SURFACE.zValue(expiry, tenor);
    double settle = ACT_ACT_ISDA.relativeYearFraction(VAL_DATE, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    double expectedRec = df * annuityCash * BlackFormulaRepository.price(forward, RATE, expiry, volatility, false);
    double expectedPay = -df * annuityCash * BlackFormulaRepository.price(forward, RATE, expiry, volatility, true);
    assertThat(computedRec.getCurrency()).isEqualTo(EUR);
    assertThat(computedRec.getAmount()).isCloseTo(expectedRec, offset(NOTIONAL * TOL));
    assertThat(computedPay.getCurrency()).isEqualTo(EUR);
    assertThat(computedPay.getAmount()).isCloseTo(expectedPay, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValue_atMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATES_PROVIDER_AT_MATURITY);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double settle = ACT_ACT_ISDA.relativeYearFraction(MATURITY, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    assertThat(computedRec.getAmount()).isCloseTo(df * annuityCash * (RATE - forward), offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValue_afterMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(computedRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValue_parity() {
    CurrencyAmount pvRecLong = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvRecShort = PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPayLong = PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvPayShort = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvRecLong.getAmount()).isCloseTo(-pvRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvPayLong.getAmount()).isCloseTo(-pvPayShort.getAmount(), offset(NOTIONAL * TOL));
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(EUR, SETTLE);
    double expected = discount * annuityCash * (forward - RATE);
    assertThat(pvPayLong.getAmount() - pvRecLong.getAmount()).isCloseTo(expected, offset(NOTIONAL * TOL));
    assertThat(pvPayShort.getAmount() - pvRecShort.getAmount()).isCloseTo(-expected, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_physicalSettlement() {
    Swaption swaption = Swaption
        .builder()
        .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
        .expiryTime(LocalTime.NOON)
        .expiryZone(ZoneOffset.UTC)
        .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
        .longShort(LONG)
        .underlying(SWAP_PAY)
        .build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.impliedVolatility(swaption.resolve(REF_DATA), RATE_PROVIDER, VOLS));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueDelta() {
    CurrencyAmount computedRec = PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPay = PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    double tenor = VOLS.tenor(SETTLE, END);
    double volatility = SURFACE.zValue(expiry, tenor);
    double settle = ACT_ACT_ISDA.relativeYearFraction(VAL_DATE, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    double expectedRec = df * annuityCash * BlackFormulaRepository.delta(forward, RATE, expiry, volatility, false);
    double expectedPay = -df * annuityCash * BlackFormulaRepository.delta(forward, RATE, expiry, volatility, true);
    assertThat(computedRec.getCurrency()).isEqualTo(EUR);
    assertThat(computedRec.getAmount()).isCloseTo(expectedRec, offset(NOTIONAL * TOL));
    assertThat(computedPay.getCurrency()).isEqualTo(EUR);
    assertThat(computedPay.getAmount()).isCloseTo(expectedPay, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueDelta_atMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValueDelta(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATES_PROVIDER_AT_MATURITY);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double settle = ACT_ACT_ISDA.relativeYearFraction(MATURITY, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    assertThat(computedRec.getAmount()).isCloseTo(-df * annuityCash, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueDelta_afterMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValueDelta(SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(computedRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueDelta_parity() {
    CurrencyAmount pvDeltaRecLong = PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvDeltaRecShort = PRICER.presentValueDelta(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvDeltaPayLong = PRICER.presentValueDelta(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvDeltaPayShort = PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvDeltaRecLong.getAmount()).isCloseTo(-pvDeltaRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvDeltaPayLong.getAmount()).isCloseTo(-pvDeltaPayShort.getAmount(), offset(NOTIONAL * TOL));
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(EUR, SETTLE);
    double expected = discount * annuityCash;
    assertThat(pvDeltaPayLong.getAmount() - pvDeltaRecLong.getAmount()).isCloseTo(expected, offset(NOTIONAL * TOL));
    assertThat(pvDeltaPayShort.getAmount() - pvDeltaRecShort.getAmount()).isCloseTo(-expected, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueGamma() {
    CurrencyAmount computedRec = PRICER.presentValueGamma(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPay = PRICER.presentValueGamma(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    double tenor = VOLS.tenor(SETTLE, END);
    double volatility = SURFACE.zValue(expiry, tenor);
    double settle = ACT_ACT_ISDA.relativeYearFraction(VAL_DATE, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    double expectedRec = df * annuityCash * BlackFormulaRepository.gamma(forward, RATE, expiry, volatility);
    double expectedPay = -df * annuityCash * BlackFormulaRepository.gamma(forward, RATE, expiry, volatility);
    assertThat(computedRec.getCurrency()).isEqualTo(EUR);
    assertThat(computedRec.getAmount()).isCloseTo(expectedRec, offset(NOTIONAL * TOL));
    assertThat(computedPay.getCurrency()).isEqualTo(EUR);
    assertThat(computedPay.getAmount()).isCloseTo(expectedPay, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueGamma_atMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValueGamma(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValueGamma(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertThat(computedRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueGamma_afterMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValueGamma(SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValueGamma(SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(computedRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueGamma_parity() {
    CurrencyAmount pvGammaRecLong = PRICER.presentValueGamma(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvGammaRecShort = PRICER.presentValueGamma(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvGammaPayLong = PRICER.presentValueGamma(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvGammaPayShort = PRICER.presentValueGamma(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvGammaRecLong.getAmount()).isCloseTo(-pvGammaRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvGammaPayLong.getAmount()).isCloseTo(-pvGammaPayShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvGammaPayLong.getAmount()).isCloseTo(pvGammaRecLong.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvGammaPayShort.getAmount()).isCloseTo(pvGammaRecShort.getAmount(), offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueTheta() {
    CurrencyAmount computedRec = PRICER.presentValueTheta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount computedPay = PRICER.presentValueTheta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    double tenor = VOLS.tenor(SETTLE, END);
    double volatility = SURFACE.zValue(expiry, tenor);
    double settle = ACT_ACT_ISDA.relativeYearFraction(VAL_DATE, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    double expectedRec = df * annuityCash * BlackFormulaRepository.driftlessTheta(forward, RATE, expiry, volatility);
    double expectedPay = -df * annuityCash * BlackFormulaRepository.driftlessTheta(forward, RATE, expiry, volatility);
    assertThat(computedRec.getCurrency()).isEqualTo(EUR);
    assertThat(computedRec.getAmount()).isCloseTo(expectedRec, offset(NOTIONAL * TOL));
    assertThat(computedPay.getCurrency()).isEqualTo(EUR);
    assertThat(computedPay.getAmount()).isCloseTo(expectedPay, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueTheta_atMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValueTheta(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValueTheta(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertThat(computedRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueTheta_afterMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValueTheta(SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValueTheta(SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(computedRec.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueTheta_parity() {
    CurrencyAmount pvThetaRecLong = PRICER.presentValueTheta(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvThetaRecShort = PRICER.presentValueTheta(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    CurrencyAmount pvThetaPayLong = PRICER.presentValueTheta(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    CurrencyAmount pvThetaPayShort = PRICER.presentValueTheta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvThetaRecLong.getAmount()).isCloseTo(-pvThetaRecShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvThetaPayLong.getAmount()).isCloseTo(-pvThetaPayShort.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvThetaPayLong.getAmount()).isCloseTo(pvThetaRecLong.getAmount(), offset(NOTIONAL * TOL));
    assertThat(pvThetaPayShort.getAmount()).isCloseTo(pvThetaRecShort.getAmount(), offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS));
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(EUR).getAmount()).isCloseTo(expectedRec.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(EUR).getAmount()).isCloseTo(expectedPay.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_currencyExposure_atMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY));
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(EUR).getAmount()).isCloseTo(expectedRec.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(EUR).getAmount()).isCloseTo(expectedPay.getAmount(EUR).getAmount(), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_currencyExposure_afterMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(computedRec.size()).isEqualTo(1);
    assertThat(computedRec.getAmount(EUR).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(computedPay.size()).isEqualTo(1);
    assertThat(computedPay.getAmount(EUR).getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_impliedVolatility() {
    double computedRec = PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    double computedPay = PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    double tenor = VOLS.tenor(SETTLE, END);
    double expected = SURFACE.zValue(expiry, tenor);
    assertThat(computedRec).isEqualTo(expected);
    assertThat(computedPay).isEqualTo(expected);
  }

  @Test
  public void test_impliedVolatility_atMaturity() {
    double computedRec =
        PRICER.impliedVolatility(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double computedPay =
        PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    double expiry = 0d;
    double tenor = VOLS.tenor(SETTLE, END);
    double expected = SURFACE.zValue(expiry, tenor);
    assertThat(computedRec).isEqualTo(expected);
    assertThat(computedPay).isEqualTo(expected);
  }

  @Test
  public void test_impliedVolatility_afterMaturity() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.impliedVolatility(
            SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.impliedVolatility(
            SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivityRatesStickyStrike() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedRec = RATE_PROVIDER.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), VOLS));
    assertThat(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d)).isTrue();
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay.build());
    CurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_PAY_SHORT, (p), VOLS));
    assertThat(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 100d)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityRatesStickyStrike_atMaturity() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    CurrencyParameterSensitivities computedRec =
        RATES_PROVIDER_AT_MATURITY.parameterSensitivity(pointRec.build());
    CurrencyParameterSensitivities expectedRec = FD_CAL.sensitivity(
        RATES_PROVIDER_AT_MATURITY, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), VOLS_AT_MATURITY));
    assertThat(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d)).isTrue();
    PointSensitivities pointPay = PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT,
        RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
  }

  @Test
  public void test_presentValueSensitivityRatesStickyStrike_afterMaturity() {
    PointSensitivities pointRec = PRICER.presentValueSensitivityRatesStickyStrike(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointRec.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
    PointSensitivities pointPay = PRICER.presentValueSensitivityRatesStickyStrike(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertThat(Math.abs(sensi.getSensitivity())).isEqualTo(0d);
    }
  }

  @Test
  public void test_presentValueSensitivityRatesStickyStrike_parity() {
    CurrencyParameterSensitivities pvSensiRecLong = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiRecShort = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiPayLong = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS).build());
    CurrencyParameterSensitivities pvSensiPayShort = RATE_PROVIDER.parameterSensitivity(
        PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS).build());
    assertThat(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();
    assertThat(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL)).isTrue();

    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    PointSensitivityBuilder forwardSensi = SWAP_PRICER.parRateSensitivity(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double annuityCashDeriv = SWAP_PRICER.getLegPricer()
        .annuityCashDerivative(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward).getDerivative(0);
    double discount = RATE_PROVIDER.discountFactor(EUR, SETTLE);
    PointSensitivityBuilder discountSensi = RATE_PROVIDER.discountFactors(EUR).zeroRatePointSensitivity(SETTLE);
    PointSensitivities expecedPoint = discountSensi.multipliedBy(annuityCash * (forward - RATE)).combinedWith(
        forwardSensi.multipliedBy(discount * annuityCash + discount * annuityCashDeriv * (forward - RATE))).build();
    CurrencyParameterSensitivities expected = RATE_PROVIDER.parameterSensitivity(expecedPoint);
    assertThat(expected.equalWithTolerance(pvSensiPayLong.combinedWith(pvSensiRecLong.multipliedBy(-1d)),
        NOTIONAL * TOL)).isTrue();
    assertThat(expected.equalWithTolerance(pvSensiRecShort.combinedWith(pvSensiPayShort.multipliedBy(-1d)),
        NOTIONAL * TOL)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivityBlackVolatility() {
    SwaptionSensitivity sensiRec =
        PRICER.presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    SwaptionSensitivity sensiPay =
        PRICER.presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    double forward = SWAP_PRICER.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(RFIXED_LEG_REC, forward);
    double expiry = VOLS.relativeTime(SWAPTION_REC_LONG.getExpiry());
    double tenor = VOLS.tenor(SETTLE, END);
    double volatility = SURFACE.zValue(expiry, tenor);
    double settle = ACT_ACT_ISDA.relativeYearFraction(VAL_DATE, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    double expectedRec = df * annuityCash * BlackFormulaRepository.vega(forward, RATE, expiry, volatility);
    double expectedPay = -df * annuityCash * BlackFormulaRepository.vega(forward, RATE, expiry, volatility);
    assertThat(sensiRec.getCurrency()).isEqualTo(EUR);
    assertThat(sensiRec.getSensitivity()).isCloseTo(expectedRec, offset(NOTIONAL * TOL));
    assertThat(sensiRec.getVolatilitiesName()).isEqualTo(VOLS.getName());
    assertThat(sensiRec.getExpiry()).isEqualTo(expiry);
    assertThat(sensiRec.getTenor()).isEqualTo(5.0);
    assertThat(sensiRec.getStrike()).isEqualTo(RATE);
    assertThat(sensiRec.getForward()).isCloseTo(forward, offset(TOL));
    assertThat(sensiPay.getCurrency()).isEqualTo(EUR);
    assertThat(sensiPay.getSensitivity()).isCloseTo(expectedPay, offset(NOTIONAL * TOL));
    assertThat(sensiRec.getVolatilitiesName()).isEqualTo(VOLS.getName());
    assertThat(sensiPay.getExpiry()).isEqualTo(expiry);
    assertThat(sensiPay.getTenor()).isEqualTo(5.0);
    assertThat(sensiPay.getStrike()).isEqualTo(RATE);
    assertThat(sensiPay.getForward()).isCloseTo(forward, offset(TOL));
  }

  @Test
  public void test_presentValueSensitivityBlackVolatility_atMaturity() {
    SwaptionSensitivity sensiRec = PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertThat(sensiRec.getSensitivity()).isCloseTo(0d, offset(NOTIONAL * TOL));
    SwaptionSensitivity sensiPay = PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOLS_AT_MATURITY);
    assertThat(sensiPay.getSensitivity()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueSensitivityBlackVolatility_afterMaturity() {
    SwaptionSensitivity sensiRec = PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(sensiRec.getSensitivity()).isCloseTo(0d, offset(NOTIONAL * TOL));
    SwaptionSensitivity sensiPay = PRICER.presentValueSensitivityModelParamsVolatility(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOLS_AFTER_MATURITY);
    assertThat(sensiPay.getSensitivity()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_presentValueSensitivityBlackVolatility_parity() {
    SwaptionSensitivity pvSensiRecLong =
        PRICER.presentValueSensitivityModelParamsVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    SwaptionSensitivity pvSensiRecShort =
        PRICER.presentValueSensitivityModelParamsVolatility(SWAPTION_REC_SHORT, RATE_PROVIDER, VOLS);
    SwaptionSensitivity pvSensiPayLong =
        PRICER.presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_LONG, RATE_PROVIDER, VOLS);
    SwaptionSensitivity pvSensiPayShort =
        PRICER.presentValueSensitivityModelParamsVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOLS);
    assertThat(pvSensiRecLong.getSensitivity()).isCloseTo(-pvSensiRecShort.getSensitivity(), offset(NOTIONAL * TOL));
    assertThat(pvSensiPayLong.getSensitivity()).isCloseTo(-pvSensiPayShort.getSensitivity(), offset(NOTIONAL * TOL));
    assertThat(pvSensiRecLong.getSensitivity()).isCloseTo(pvSensiPayLong.getSensitivity(), offset(NOTIONAL * TOL));
    assertThat(pvSensiPayShort.getSensitivity()).isCloseTo(pvSensiPayShort.getSensitivity(), offset(NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void pvRegression() {
    CurrencyAmount pv = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    assertThat(pv.getAmount()).isCloseTo(3823688.253812721, offset(NOTIONAL * TOL)); // 2.x
  }

  @Test
  public void pvCurveSensiRegression() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivityRatesStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOLS);
    CurrencyParameterSensitivities computed = RATE_PROVIDER.parameterSensitivity(point.build());
    computed.getSensitivity(DSC_NAME, EUR).getSensitivity();
    DoubleArray dscSensi = DoubleArray.of(
        0.0, 0.0, 0.0, -7143525.908886078, -1749520.4110068753, -719115.4683096837); // 2.x
    DoubleArray fwdSensi = DoubleArray.of(
        0d, 0d, 0d, 1.7943318714062232E8, -3.4987983718159467E8, -2.6516758066404995E8); // 2.x
    CurrencyParameterSensitivity dsc = DSC_CURVE.createParameterSensitivity(EUR, dscSensi);
    CurrencyParameterSensitivity fwd = FWD6_CURVE.createParameterSensitivity(EUR, fwdSensi);
    CurrencyParameterSensitivities expected = CurrencyParameterSensitivities.of(ImmutableList.of(dsc, fwd));
    assertThat(computed.equalWithTolerance(expected, NOTIONAL * TOL)).isTrue();
  }

}
