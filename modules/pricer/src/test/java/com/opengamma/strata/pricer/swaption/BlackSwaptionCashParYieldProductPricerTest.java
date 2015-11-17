/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.LongShort.SHORT;
import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swaption.CashSettlement;
import com.opengamma.strata.product.swaption.CashSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSettlement;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * Test {@link BlackSwaptionCashParYieldProductPricer}.
 */
@Test
public class BlackSwaptionCashParYieldProductPricerTest {
  private static final LocalDate VALUATION = LocalDate.of(2012, 1, 10);
  // curve
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final DoubleArray DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray DSC_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  private static final CurveName DSC_NAME = CurveName.of("EUR Dsc");
  private static final CurveMetadata META_DSC = Curves.zeroRates(DSC_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve DSC_CURVE = InterpolatedNodalCurve.of(META_DSC, DSC_TIME, DSC_RATE, INTERPOLATOR); 
  private static final DoubleArray FWD6_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray FWD6_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  private static final CurveName FWD6_NAME = CurveName.of("EUR EURIBOR 6M");
  private static final CurveMetadata META_FWD6 = Curves.zeroRates(FWD6_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve FWD6_CURVE = InterpolatedNodalCurve.of(META_FWD6, FWD6_TIME, FWD6_RATE, INTERPOLATOR); 
  private static final ImmutableRatesProvider RATE_PROVIDER = ImmutableRatesProvider.builder()
      .discountCurves(ImmutableMap.of(EUR, DSC_CURVE))
      .indexCurves(ImmutableMap.of(EUR_EURIBOR_6M, FWD6_CURVE))
      .valuationDate(VALUATION)
      .build();
  // surface
  private static final Interpolator1D LINEAR_FLAT =
      CombinedInterpolatorExtrapolator.of(CurveInterpolators.LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final DoubleArray EXPIRY = DoubleArray.of(0.5, 1.0, 5.0, 0.5, 1.0, 5.0);
  private static final DoubleArray TENOR = DoubleArray.of(2, 2, 2, 10, 10, 10);
  private static final DoubleArray VOL = DoubleArray.of(0.35, 0.34, 0.25, 0.30, 0.25, 0.20);
  private static final SurfaceMetadata METADATA = DefaultSurfaceMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.YEAR_FRACTION)
      .zValueType(ValueType.VOLATILITY)
      .surfaceName(SurfaceName.of("Black Vol"))
      .build();
  private static final NodalSurface SURFACE = InterpolatedNodalSurface.of(METADATA, EXPIRY, TENOR, VOL, INTERPOLATOR_2D);
  private static final FixedIborSwapConvention SWAP_CONVENTION = FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
  private static final BlackVolatilityExpiryTenorSwaptionProvider VOL_PROVIDER =
      BlackVolatilityExpiryTenorSwaptionProvider.of(SURFACE, SWAP_CONVENTION, ACT_ACT_ISDA, VALUATION);
  // underlying swap and swaption
  private static final HolidayCalendar CALENDAR = HolidayCalendars.SAT_SUN;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CALENDAR);
  private static final LocalDate MATURITY = BDA_MF.adjust(VALUATION.plusMonths(26));
  private static final LocalDate SETTLE = BDA_MF.adjust(CALENDAR.shift(MATURITY, 2));
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
  private static final Swap SWAP_PAY = Swap.of(FIXED_LEG_PAY, IBOR_LEG_REC);
  private static final CashSettlement PAR_YIELD = CashSettlement.builder()
      .cashSettlementMethod(CashSettlementMethod.PAR_YIELD)
      .settlementDate(SETTLE)
      .build();
  private static final Swaption SWAPTION_REC_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
      .expiryTime(LocalTime.NOON)
      .expiryZone(ZoneOffset.UTC)
      .swaptionSettlement(PAR_YIELD)
      .longShort(LONG)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_REC_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
      .expiryTime(LocalTime.NOON)
      .expiryZone(ZoneOffset.UTC)
      .swaptionSettlement(PAR_YIELD)
      .longShort(SHORT)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_PAY_LONG = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
      .expiryTime(LocalTime.NOON)
      .expiryZone(ZoneOffset.UTC)
      .swaptionSettlement(PAR_YIELD)
      .longShort(LONG)
      .underlying(SWAP_PAY)
      .build();
  private static final Swaption SWAPTION_PAY_SHORT = Swaption
      .builder()
      .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
      .expiryTime(LocalTime.NOON)
      .expiryZone(ZoneOffset.UTC)
      .swaptionSettlement(PAR_YIELD)
      .longShort(SHORT)
      .underlying(SWAP_PAY)
      .build();
  // providers used for specific tests
  private static final ImmutableRatesProvider RATES_PROVIDER_AT_MATURITY = ImmutableRatesProvider.builder()
      .discountCurves(ImmutableMap.of(EUR, DSC_CURVE))
      .indexCurves(ImmutableMap.of(EUR_EURIBOR_6M, FWD6_CURVE))
      .valuationDate(MATURITY)
      .build();
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER_MATURITY = ImmutableRatesProvider.builder()
      .discountCurves(ImmutableMap.of(EUR, DSC_CURVE))
      .indexCurves(ImmutableMap.of(EUR_EURIBOR_6M, FWD6_CURVE))
      .valuationDate(MATURITY.plusDays(1))
      .build();
  private static final BlackVolatilityExpiryTenorSwaptionProvider VOL_PROVIDER_AT_MATURITY =
      BlackVolatilityExpiryTenorSwaptionProvider.of(SURFACE, SWAP_CONVENTION, ACT_ACT_ISDA, MATURITY);
  private static final BlackVolatilityExpiryTenorSwaptionProvider VOL_PROVIDER_AFTER_MATURITY =
      BlackVolatilityExpiryTenorSwaptionProvider.of(SURFACE, SWAP_CONVENTION, ACT_ACT_ISDA, MATURITY.plusDays(1));
  // test parameters
  private static final double TOL = 1.0e-12;
  private static final double FD_EPS = 1.0e-7;
  // pricer
  private static final BlackSwaptionCashParYieldProductPricer PRICER = BlackSwaptionCashParYieldProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  public void test_presentValue() {
    CurrencyAmount computedRec = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPay = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double forward = SWAP_PRICER.parRate(SWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(FIXED_LEG_REC, forward);
    double expiry = VOL_PROVIDER.relativeTime(SWAPTION_REC_LONG.getExpiryDateTime());
    double tenor = VOL_PROVIDER.tenor(SETTLE, END);
    double volatility = SURFACE.zValue(expiry, tenor);
    double settle = ACT_ACT_ISDA.relativeYearFraction(VALUATION, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    double expectedRec = df * annuityCash * BlackFormulaRepository.price(forward, RATE, expiry, volatility, false);
    double expectedPay = -df * annuityCash * BlackFormulaRepository.price(forward, RATE, expiry, volatility, true);
    assertEquals(computedRec.getCurrency(), EUR);
    assertEquals(computedRec.getAmount(), expectedRec, NOTIONAL * TOL);
    assertEquals(computedPay.getCurrency(), EUR);
    assertEquals(computedPay.getAmount(), expectedPay, NOTIONAL * TOL);
  }

  public void test_presentValue_atMaturity() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    double forward = SWAP_PRICER.parRate(SWAP_REC, RATES_PROVIDER_AT_MATURITY);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(FIXED_LEG_REC, forward);
    double settle = ACT_ACT_ISDA.relativeYearFraction(MATURITY, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    assertEquals(computedRec.getAmount(), df * annuityCash * (RATE - forward), NOTIONAL * TOL);
    assertEquals(computedPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_afterExpiry() {
    CurrencyAmount computedRec =
        PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    CurrencyAmount computedPay =
        PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(computedRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_parity() {
    CurrencyAmount pvRecLong = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvRecShort = PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPayLong = PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPayShort = PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvRecLong.getAmount(), -pvRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvPayLong.getAmount(), -pvPayShort.getAmount(), NOTIONAL * TOL);
    double forward = SWAP_PRICER.parRate(SWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(SWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(EUR, SETTLE);
    double expected = discount * annuityCash * (forward - RATE);
    assertEquals(pvPayLong.getAmount() - pvRecLong.getAmount(), expected, NOTIONAL * TOL);
    assertEquals(pvPayShort.getAmount() - pvRecShort.getAmount(), -expected, NOTIONAL * TOL);
  }

  public void test_physicalSettlement() {
    Swaption swaption = Swaption
        .builder()
        .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
        .expiryTime(LocalTime.NOON)
        .expiryZone(ZoneOffset.UTC)
        .swaptionSettlement(PhysicalSettlement.DEFAULT)
        .longShort(LONG)
        .underlying(SWAP_PAY)
        .build();
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(swaption, RATE_PROVIDER, VOL_PROVIDER));
  }

  public void test_noFixedLeg() {
    Swap swap = Swap.of(IBOR_LEG_REC, IBOR_LEG_PAY);
    Swaption swaption = Swaption
        .builder()
        .expiryDate(AdjustableDate.of(MATURITY, BDA_MF))
        .expiryTime(LocalTime.NOON)
        .expiryZone(ZoneOffset.UTC)
        .swaptionSettlement(PAR_YIELD)
        .longShort(LONG)
        .underlying(swap)
        .build();
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(swaption, RATE_PROVIDER, VOL_PROVIDER));
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(EUR).getAmount(), expectedRec.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(EUR).getAmount(), expectedPay.getAmount(EUR).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_atMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER.presentValue(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(EUR).getAmount(), expectedRec.getAmount(EUR).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER.presentValue(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(EUR).getAmount(), expectedPay.getAmount(EUR).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_afterMaturity() {
    MultiCurrencyAmount computedRec = PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    MultiCurrencyAmount computedPay = PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(EUR).getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(EUR).getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_impliedVolatility() {
    double computedRec = PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    double computedPay = PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double expiry = VOL_PROVIDER.relativeTime(SWAPTION_REC_LONG.getExpiryDateTime());
    double tenor = VOL_PROVIDER.tenor(SETTLE, END);
    double expected = SURFACE.zValue(expiry, tenor);
    assertEquals(computedRec, expected);
    assertEquals(computedPay, expected);
  }

  public void test_impliedVolatility_atMaturity() {
    double computedRec =
        PRICER.impliedVolatility(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    double computedPay =
        PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    double expiry = 0d;
    double tenor = VOL_PROVIDER.tenor(SETTLE, END);
    double expected = SURFACE.zValue(expiry, tenor);
    assertEquals(computedRec, expected);
    assertEquals(computedPay, expected);
  }

  public void test_impliedVolatility_afterMaturity() {
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY));
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityStickyStrike() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities computedRec = RATE_PROVIDER.curveParameterSensitivity(pointRec.build());
    CurveCurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), VOL_PROVIDER));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d));
    PointSensitivityBuilder pointPay =
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities computedPay = RATE_PROVIDER.curveParameterSensitivity(pointPay.build());
    CurveCurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> PRICER.presentValue(SWAPTION_PAY_SHORT, (p), VOL_PROVIDER));
    assertTrue(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 100d));
  }

  public void test_presentValueSensitivityStickyStrike_atMaturity() {
    PointSensitivityBuilder pointRec =
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    CurveCurrencyParameterSensitivities computedRec =
        RATES_PROVIDER_AT_MATURITY.curveParameterSensitivity(pointRec.build());
    CurveCurrencyParameterSensitivities expectedRec = FD_CAL.sensitivity(
        RATES_PROVIDER_AT_MATURITY, (p) -> PRICER.presentValue(SWAPTION_REC_LONG, (p), VOL_PROVIDER_AT_MATURITY));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d));
    PointSensitivities pointPay = PRICER.presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT,
        RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
  }

  public void test_presentValueSensitivityStickyStrike_afterMaturity() {
    PointSensitivities pointRec = PRICER.presentValueSensitivityStickyStrike(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointRec.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
    PointSensitivities pointPay = PRICER.presentValueSensitivityStickyStrike(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
  }

  public void test_presentValueSensitivityStickyStrike_parity() {
    CurveCurrencyParameterSensitivities pvSensiRecLong = RATE_PROVIDER.curveParameterSensitivity(
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER).build());
    CurveCurrencyParameterSensitivities pvSensiRecShort = RATE_PROVIDER.curveParameterSensitivity(
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER).build());
    CurveCurrencyParameterSensitivities pvSensiPayLong = RATE_PROVIDER.curveParameterSensitivity(
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER).build());
    CurveCurrencyParameterSensitivities pvSensiPayShort = RATE_PROVIDER.curveParameterSensitivity(
        PRICER.presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER).build());
    assertTrue(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL));
    assertTrue(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL));

    double forward = SWAP_PRICER.parRate(SWAP_REC, RATE_PROVIDER);
    PointSensitivityBuilder forwardSensi = SWAP_PRICER.parRateSensitivity(SWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(SWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double annuityCashDeriv = SWAP_PRICER.getLegPricer()
        .annuityCashDerivative(SWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(EUR, SETTLE);
    PointSensitivityBuilder discountSensi = RATE_PROVIDER.discountFactors(EUR).zeroRatePointSensitivity(SETTLE);
    PointSensitivities expecedPoint = discountSensi.multipliedBy(annuityCash * (forward - RATE)).combinedWith(
        forwardSensi.multipliedBy(discount * annuityCash + discount * annuityCashDeriv * (forward - RATE))).build();
    CurveCurrencyParameterSensitivities expected = RATE_PROVIDER.curveParameterSensitivity(expecedPoint);
    assertTrue(expected.equalWithTolerance(pvSensiPayLong.combinedWith(pvSensiRecLong.multipliedBy(-1d)),
        NOTIONAL * TOL));
    assertTrue(expected.equalWithTolerance(pvSensiRecShort.combinedWith(pvSensiPayShort.multipliedBy(-1d)),
        NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityBlackVolatility() {
    SwaptionSensitivity sensiRec =
        PRICER.presentValueSensitivityBlackVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity sensiPay =
        PRICER.presentValueSensitivityBlackVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double forward = SWAP_PRICER.parRate(SWAP_REC, RATE_PROVIDER);
    double annuityCash = SWAP_PRICER.getLegPricer().annuityCash(FIXED_LEG_REC, forward);
    double expiry = VOL_PROVIDER.relativeTime(SWAPTION_REC_LONG.getExpiryDateTime());
    double tenor = VOL_PROVIDER.tenor(SETTLE, END);
    double volatility = SURFACE.zValue(expiry, tenor);
    double settle = ACT_ACT_ISDA.relativeYearFraction(VALUATION, SETTLE);
    double df = Math.exp(-DSC_CURVE.yValue(settle) * settle);
    double expectedRec = df * annuityCash * BlackFormulaRepository.vega(forward, RATE, expiry, volatility);
    double expectedPay = -df * annuityCash * BlackFormulaRepository.vega(forward, RATE, expiry, volatility);
    assertEquals(sensiRec.getCurrency(), EUR);
    assertEquals(sensiRec.getSensitivity(), expectedRec, NOTIONAL * TOL);
    assertEquals(sensiRec.getConvention(), SWAP_CONVENTION);
    assertEquals(sensiRec.getExpiry(), SWAPTION_REC_LONG.getExpiryDateTime());
    assertEquals(sensiRec.getTenor(), 5.0);
    assertEquals(sensiRec.getStrike(), RATE);
    assertEquals(sensiRec.getForward(), forward, TOL);
    assertEquals(sensiPay.getCurrency(), EUR);
    assertEquals(sensiPay.getSensitivity(), expectedPay, NOTIONAL * TOL);
    assertEquals(sensiRec.getConvention(), SWAP_CONVENTION);
    assertEquals(sensiPay.getExpiry(), SWAPTION_REC_LONG.getExpiryDateTime());
    assertEquals(sensiPay.getTenor(), 5.0);
    assertEquals(sensiPay.getStrike(), RATE);
    assertEquals(sensiPay.getForward(), forward, TOL);
  }

  public void test_presentValueSensitivityBlackVolatility_atMaturity() {
    SwaptionSensitivity sensiRec = PRICER.presentValueSensitivityBlackVolatility(
        SWAPTION_REC_LONG, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    assertEquals(sensiRec.getSensitivity(), 0d, NOTIONAL * TOL);
    SwaptionSensitivity sensiPay = PRICER.presentValueSensitivityBlackVolatility(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    assertEquals(sensiPay.getSensitivity(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueSensitivityBlackVolatility_afterMaturity() {
    SwaptionSensitivity sensiRec = PRICER.presentValueSensitivityBlackVolatility(
        SWAPTION_REC_LONG, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(sensiRec.getSensitivity(), 0d, NOTIONAL * TOL);
    SwaptionSensitivity sensiPay = PRICER.presentValueSensitivityBlackVolatility(
        SWAPTION_PAY_SHORT, RATES_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(sensiPay.getSensitivity(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueSensitivityBlackVolatility_parity() {
    SwaptionSensitivity pvSensiRecLong =
        PRICER.presentValueSensitivityBlackVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity pvSensiRecShort =
        PRICER.presentValueSensitivityBlackVolatility(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity pvSensiPayLong =
        PRICER.presentValueSensitivityBlackVolatility(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity pvSensiPayShort =
        PRICER.presentValueSensitivityBlackVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvSensiRecLong.getSensitivity(), -pvSensiRecShort.getSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayLong.getSensitivity(), -pvSensiPayShort.getSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiRecLong.getSensitivity(), pvSensiPayLong.getSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayShort.getSensitivity(), pvSensiPayShort.getSensitivity(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void pvRegression() {
    CurrencyAmount pv = PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pv.getAmount(), 3823688.253812721, NOTIONAL * TOL); // 2.x
  }

  public void pvCurveSensiRegression() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities computed = RATE_PROVIDER.curveParameterSensitivity(point.build());
    computed.getSensitivity(DSC_NAME, EUR).getSensitivity();
    DoubleArray dscSensi = DoubleArray.of(
        0.0, 0.0, 0.0, -7143525.908886078, -1749520.4110068753, -719115.4683096837); // 2.x
    DoubleArray fwdSensi = DoubleArray.of(
        0d, 0d, 0d, 1.7943318714062232E8, -3.4987983718159467E8, -2.6516758066404995E8); // 2.x
    CurveCurrencyParameterSensitivity dsc = CurveCurrencyParameterSensitivity.of(META_DSC, EUR, dscSensi);
    CurveCurrencyParameterSensitivity fwd = CurveCurrencyParameterSensitivity.of(META_FWD6, EUR, fwdSensi);
    CurveCurrencyParameterSensitivities expected = CurveCurrencyParameterSensitivities.of(ImmutableList.of(dsc, fwd));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * TOL));
  }

}
