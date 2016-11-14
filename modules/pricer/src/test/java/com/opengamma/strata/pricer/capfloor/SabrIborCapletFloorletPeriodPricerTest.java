/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.model.SabrParameterType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.swap.DiscountingRatePaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;

/**
 * Test {@link SabrIborCapletFloorletPeriodPricer}.
 */
@Test
public class SabrIborCapletFloorletPeriodPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ZonedDateTime VALUATION = dateUtc(2008, 8, 18);
  private static final LocalDate FIXING = LocalDate.of(2011, 1, 3);
  private static final double NOTIONAL = 1000000; //1m
  private static final double STRIKE = 0.01;
  private static final IborRateComputation RATE_COMP = IborRateComputation.of(EUR_EURIBOR_3M, FIXING, REF_DATA);
  private static final IborCapletFloorletPeriod CAPLET_LONG = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .notional(NOTIONAL)
      .iborRate(RATE_COMP)
      .build();
  private static final IborCapletFloorletPeriod CAPLET_SHORT = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .notional(-NOTIONAL)
      .iborRate(RATE_COMP)
      .build();
  private static final IborCapletFloorletPeriod FLOORLET_LONG = IborCapletFloorletPeriod.builder()
      .floorlet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .notional(NOTIONAL)
      .iborRate(RATE_COMP)
      .build();
  private static final IborCapletFloorletPeriod FLOORLET_SHORT = IborCapletFloorletPeriod.builder()
      .floorlet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .notional(-NOTIONAL)
      .iborRate(RATE_COMP)
      .build();
  private static final RateAccrualPeriod IBOR_PERIOD = RateAccrualPeriod.builder()
      .startDate(CAPLET_LONG.getStartDate())
      .endDate(CAPLET_LONG.getEndDate())
      .yearFraction(CAPLET_LONG.getYearFraction())
      .rateComputation(RATE_COMP)
      .build();
  private static final RatePaymentPeriod IBOR_COUPON = RatePaymentPeriod.builder()
      .accrualPeriods(IBOR_PERIOD)
      .paymentDate(CAPLET_LONG.getPaymentDate())
      .dayCount(EUR_EURIBOR_3M.getDayCount())
      .notional(NOTIONAL)
      .currency(EUR)
      .build();
  private static final RateAccrualPeriod FIXED_PERIOD = RateAccrualPeriod.builder()
      .startDate(CAPLET_LONG.getStartDate())
      .endDate(CAPLET_LONG.getEndDate())
      .rateComputation(FixedRateComputation.of(STRIKE))
      .yearFraction(CAPLET_LONG.getYearFraction())
      .build();
  private static final RatePaymentPeriod FIXED_COUPON = RatePaymentPeriod.builder()
      .accrualPeriods(FIXED_PERIOD)
      .paymentDate(CAPLET_LONG.getPaymentDate())
      .dayCount(EUR_EURIBOR_3M.getDayCount())
      .notional(NOTIONAL)
      .currency(EUR)
      .build();
  private static final RateAccrualPeriod FIXED_PERIOD_UNIT = RateAccrualPeriod.builder()
      .startDate(CAPLET_LONG.getStartDate())
      .endDate(CAPLET_LONG.getEndDate())
      .rateComputation(FixedRateComputation.of(1d))
      .yearFraction(CAPLET_LONG.getYearFraction())
      .build();
  private static final RatePaymentPeriod FIXED_COUPON_UNIT = RatePaymentPeriod.builder()
      .accrualPeriods(FIXED_PERIOD_UNIT)
      .paymentDate(CAPLET_LONG.getPaymentDate())
      .dayCount(EUR_EURIBOR_3M.getDayCount())
      .notional(NOTIONAL)
      .currency(EUR)
      .build();
  // valuation date before fixing date
  private static final ImmutableRatesProvider RATES = IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
      VALUATION.toLocalDate(), EUR_EURIBOR_3M, LocalDateDoubleTimeSeries.empty());
  private static final SabrParametersIborCapletFloorletVolatilities VOLS = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION, EUR_EURIBOR_3M);
  // valuation date equal to fixing date
  private static final double OBS_INDEX = 0.013;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.of(FIXING, OBS_INDEX);
  private static final ImmutableRatesProvider RATES_ON_FIX =
      IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(FIXING, EUR_EURIBOR_3M, TIME_SERIES);
  private static final SabrParametersIborCapletFloorletVolatilities VOLS_ON_FIX = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(FIXING.atStartOfDay(ZoneOffset.UTC), EUR_EURIBOR_3M);
  // valuation date after fixing date
  private static final ImmutableRatesProvider RATES_AFTER_FIX =
      IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(FIXING.plusWeeks(1), EUR_EURIBOR_3M, TIME_SERIES);
  private static final SabrParametersIborCapletFloorletVolatilities VOLS_AFTER_FIX = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(FIXING.plusWeeks(1).atStartOfDay(ZoneOffset.UTC), EUR_EURIBOR_3M);
  // valuation date after payment date
  private static final LocalDate DATE_AFTER_PAY = LocalDate.of(2011, 5, 2);
  private static final ImmutableRatesProvider RATES_AFTER_PAY =
      IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(DATE_AFTER_PAY, EUR_EURIBOR_3M, TIME_SERIES);
  private static final SabrParametersIborCapletFloorletVolatilities VOLS_AFTER_PAY = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(DATE_AFTER_PAY.plusWeeks(1).atStartOfDay(ZoneOffset.UTC), EUR_EURIBOR_3M);
  // Black vols
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS_BLACK = IborCapletFloorletDataSet
      .createBlackVolatilities(VALUATION, EUR_EURIBOR_3M);
  // constatnt shift 
  private static final double SHIFT = IborCapletFloorletSabrRateVolatilityDataSet.CONST_SHIFT;

  private static final double TOL = 1.0e-14;
  private static final double EPS_FD = 1.0e-6;
  private static final SabrIborCapletFloorletPeriodPricer PRICER = SabrIborCapletFloorletPeriodPricer.DEFAULT;
  private static final VolatilityIborCapletFloorletPeriodPricer PRICER_BASE = VolatilityIborCapletFloorletPeriodPricer.DEFAULT;
  private static final DiscountingRatePaymentPeriodPricer PRICER_COUPON = DiscountingRatePaymentPeriodPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  //-------------------------------------------------------------------------
  public void test_presentValue_formula() {
    CurrencyAmount computedCaplet = PRICER.presentValue(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount computedFloorlet = PRICER.presentValue(FLOORLET_SHORT, RATES, VOLS);
    double forward = RATES.iborIndexRates(EUR_EURIBOR_3M).rate(RATE_COMP.getObservation());
    double expiry = VOLS.relativeTime(CAPLET_LONG.getFixingDateTime());
    double volatility = VOLS.volatility(expiry, STRIKE, forward);
    double df = RATES.discountFactor(EUR, CAPLET_LONG.getPaymentDate());
    double expectedCaplet = NOTIONAL * df * CAPLET_LONG.getYearFraction() * BlackFormulaRepository.price(
        forward + SHIFT, STRIKE + SHIFT, expiry, volatility, CALL.isCall());
    double expectedFloorlet = -NOTIONAL * df * CAPLET_LONG.getYearFraction() * BlackFormulaRepository.price(
        forward + SHIFT, STRIKE + SHIFT, expiry, volatility, PUT.isCall());
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), expectedCaplet, NOTIONAL * TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), expectedFloorlet, NOTIONAL * TOL);
    // consistency with shifted Black
    ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities vols = ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(
        EUR_EURIBOR_3M, VALUATION, ConstantSurface.of("constVol", volatility)
            .withMetadata(Surfaces.blackVolatilityByExpiryStrike("costVol", DayCounts.ACT_ACT_ISDA)),
        IborCapletFloorletSabrRateVolatilityDataSet.CURVE_CONST_SHIFT);
    CurrencyAmount computedCapletBlack = PRICER_BASE.presentValue(CAPLET_LONG, RATES, vols);
    CurrencyAmount computedFloorletBlack = PRICER_BASE.presentValue(FLOORLET_SHORT, RATES, vols);
    assertEquals(computedCaplet.getAmount(), computedCapletBlack.getAmount(), NOTIONAL * TOL);
    assertEquals(computedFloorlet.getAmount(), computedFloorletBlack.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValue_parity() {
    double capletLong = PRICER.presentValue(CAPLET_LONG, RATES, VOLS).getAmount();
    double capletShort = PRICER.presentValue(CAPLET_SHORT, RATES, VOLS).getAmount();
    double floorletLong = PRICER.presentValue(FLOORLET_LONG, RATES, VOLS).getAmount();
    double floorletShort = PRICER.presentValue(FLOORLET_SHORT, RATES, VOLS).getAmount();
    double iborCoupon = PRICER_COUPON.presentValue(IBOR_COUPON, RATES);
    double fixedCoupon = PRICER_COUPON.presentValue(FIXED_COUPON, RATES);
    assertEquals(capletLong, -capletShort, NOTIONAL * TOL);
    assertEquals(floorletLong, -floorletShort, NOTIONAL * TOL);
    assertEquals(capletLong - floorletLong, iborCoupon - fixedCoupon, NOTIONAL * TOL);
    assertEquals(capletShort - floorletShort, -iborCoupon + fixedCoupon, NOTIONAL * TOL);
  }

  public void test_presentValue_onFix() {
    CurrencyAmount computedCaplet = PRICER.presentValue(CAPLET_LONG, RATES_ON_FIX, VOLS_ON_FIX);
    CurrencyAmount computedFloorlet = PRICER.presentValue(FLOORLET_SHORT, RATES_ON_FIX, VOLS_ON_FIX);
    double expectedCaplet = PRICER_COUPON.presentValue(FIXED_COUPON_UNIT, RATES_ON_FIX) * (OBS_INDEX - STRIKE);
    double expectedFloorlet = 0d;
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), expectedCaplet, NOTIONAL * TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), expectedFloorlet, NOTIONAL * TOL);
  }

  public void test_presentValue_afterFix() {
    CurrencyAmount computedCaplet = PRICER.presentValue(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    CurrencyAmount computedFloorlet = PRICER.presentValue(FLOORLET_SHORT, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    double payoff = (OBS_INDEX - STRIKE) * PRICER_COUPON.presentValue(FIXED_COUPON_UNIT, RATES_AFTER_FIX);
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), payoff, NOTIONAL * TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_afterPay() {
    CurrencyAmount computedCaplet = PRICER.presentValue(CAPLET_LONG, RATES_AFTER_PAY, VOLS_AFTER_PAY);
    CurrencyAmount computedFloorlet = PRICER.presentValue(FLOORLET_SHORT, RATES_AFTER_PAY, VOLS_AFTER_PAY);
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_impliedVolatility() {
    double computed = PRICER.impliedVolatility(CAPLET_LONG, RATES, VOLS);
    double expiry = VOLS.relativeTime(CAPLET_LONG.getFixingDateTime());
    double forward = RATES.iborIndexRates(EUR_EURIBOR_3M).rate(RATE_COMP.getObservation());
    double expected = VOLS.volatility(expiry, STRIKE, forward);
    assertEquals(computed, expected, TOL);
  }

  public void test_impliedVolatility_onFix() {
    double computed = PRICER.impliedVolatility(CAPLET_LONG, RATES_ON_FIX, VOLS_ON_FIX);
    double forward = RATES_ON_FIX.iborIndexRates(EUR_EURIBOR_3M).rate(RATE_COMP.getObservation());
    double expected = VOLS.volatility(0d, STRIKE, forward);
    assertEquals(computed, expected, TOL);
  }

  public void test_impliedVolatility_afterFix() {
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueDelta_formula() {
    CurrencyAmount computedCaplet = PRICER.presentValueDelta(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount computedFloorlet = PRICER.presentValueDelta(FLOORLET_SHORT, RATES, VOLS);
    double forward = RATES.iborIndexRates(EUR_EURIBOR_3M).rate(RATE_COMP.getObservation());
    double expiry = VOLS.relativeTime(CAPLET_LONG.getFixingDateTime());
    double volatility = VOLS.volatility(expiry, STRIKE, forward);
    double df = RATES.discountFactor(EUR, CAPLET_LONG.getPaymentDate());
    double expectedCaplet = NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.delta(forward + SHIFT, STRIKE + SHIFT, expiry, volatility, CALL.isCall());
    double expectedFloorlet = -NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.delta(forward + SHIFT, STRIKE + SHIFT, expiry, volatility, PUT.isCall());
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), expectedCaplet, NOTIONAL * TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), expectedFloorlet, NOTIONAL * TOL);
  }

  public void test_presentValueDelta_parity() {
    double capletLong = PRICER.presentValueDelta(CAPLET_LONG, RATES, VOLS).getAmount();
    double capletShort = PRICER.presentValueDelta(CAPLET_SHORT, RATES, VOLS).getAmount();
    double floorletLong = PRICER.presentValueDelta(FLOORLET_LONG, RATES, VOLS).getAmount();
    double floorletShort = PRICER.presentValueDelta(FLOORLET_SHORT, RATES, VOLS).getAmount();
    double unitCoupon = PRICER_COUPON.presentValue(FIXED_COUPON_UNIT, RATES);
    assertEquals(capletLong, -capletShort, NOTIONAL * TOL);
    assertEquals(floorletLong, -floorletShort, NOTIONAL * TOL);
    assertEquals(capletLong - floorletLong, unitCoupon, NOTIONAL * TOL);
    assertEquals(capletShort - floorletShort, -unitCoupon, NOTIONAL * TOL);
  }

  public void test_presentValueDelta_onFix() {
    CurrencyAmount computedCaplet = PRICER.presentValueDelta(CAPLET_LONG, RATES_ON_FIX, VOLS_ON_FIX);
    CurrencyAmount computedFloorlet = PRICER.presentValueDelta(FLOORLET_SHORT, RATES_ON_FIX, VOLS_ON_FIX);
    double expectedCaplet = PRICER_COUPON.presentValue(FIXED_COUPON_UNIT, RATES_ON_FIX);
    double expectedFloorlet = 0d;
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), expectedCaplet, TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), expectedFloorlet, TOL);
  }

  public void test_presentValueDelta_afterFix() {
    CurrencyAmount computedCaplet = PRICER.presentValueDelta(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    CurrencyAmount computedFloorlet = PRICER.presentValueDelta(FLOORLET_SHORT, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), 0d, TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueGamma_formula() {
    CurrencyAmount computedCaplet = PRICER.presentValueGamma(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount computedFloorlet = PRICER.presentValueGamma(FLOORLET_SHORT, RATES, VOLS);
    double forward = RATES.iborIndexRates(EUR_EURIBOR_3M).rate(RATE_COMP.getObservation());
    double expiry = VOLS.relativeTime(CAPLET_LONG.getFixingDateTime());
    double volatility = VOLS.volatility(expiry, STRIKE, forward);
    double df = RATES.discountFactor(EUR, CAPLET_LONG.getPaymentDate());
    double expectedCaplet = NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.gamma(forward + SHIFT, STRIKE + SHIFT, expiry, volatility);
    double expectedFloorlet = -NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.gamma(forward + SHIFT, STRIKE + SHIFT, expiry, volatility);
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), expectedCaplet, NOTIONAL * TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), expectedFloorlet, NOTIONAL * TOL);
  }

  public void test_presentValueGamma_onFix() {
    CurrencyAmount computedCaplet = PRICER.presentValueGamma(CAPLET_LONG, RATES_ON_FIX, VOLS_ON_FIX);
    CurrencyAmount computedFloorlet = PRICER.presentValueGamma(FLOORLET_SHORT, RATES_ON_FIX, VOLS_ON_FIX);
    double expectedCaplet = 0d;
    double expectedFloorlet = 0d;
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), expectedCaplet, TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), expectedFloorlet, TOL);
  }

  public void test_presentValueGamma_afterFix() {
    CurrencyAmount computedCaplet = PRICER.presentValueGamma(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    CurrencyAmount computedFloorlet = PRICER.presentValueGamma(FLOORLET_SHORT, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), 0d, TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueTheta_formula() {
    CurrencyAmount computedCaplet = PRICER.presentValueTheta(CAPLET_LONG, RATES, VOLS);
    CurrencyAmount computedFloorlet = PRICER.presentValueTheta(FLOORLET_SHORT, RATES, VOLS);
    double forward = RATES.iborIndexRates(EUR_EURIBOR_3M).rate(RATE_COMP.getObservation());
    double expiry = VOLS.relativeTime(CAPLET_LONG.getFixingDateTime());
    double volatility = VOLS.volatility(expiry, STRIKE, forward);
    double df = RATES.discountFactor(EUR, CAPLET_LONG.getPaymentDate());
    double expectedCaplet = NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.driftlessTheta(forward + SHIFT, STRIKE + SHIFT, expiry, volatility);
    double expectedFloorlet = -NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.driftlessTheta(forward + SHIFT, STRIKE + SHIFT, expiry, volatility);
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), expectedCaplet, NOTIONAL * TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), expectedFloorlet, NOTIONAL * TOL);
  }

  public void test_presentValueTheta_parity() {
    double capletLong = PRICER.presentValueTheta(CAPLET_LONG, RATES, VOLS).getAmount();
    double capletShort = PRICER.presentValueTheta(CAPLET_SHORT, RATES, VOLS).getAmount();
    double floorletLong = PRICER.presentValueTheta(FLOORLET_LONG, RATES, VOLS).getAmount();
    double floorletShort = PRICER.presentValueTheta(FLOORLET_SHORT, RATES, VOLS).getAmount();
    assertEquals(capletLong, -capletShort, NOTIONAL * TOL);
    assertEquals(floorletLong, -floorletShort, NOTIONAL * TOL);
    assertEquals(capletLong, floorletLong, NOTIONAL * TOL);
    assertEquals(capletShort, floorletShort, NOTIONAL * TOL);
  }

  public void test_presentValueTheta_onFix() {
    CurrencyAmount computedCaplet = PRICER.presentValueTheta(CAPLET_LONG, RATES_ON_FIX, VOLS_ON_FIX);
    CurrencyAmount computedFloorlet = PRICER.presentValueTheta(FLOORLET_SHORT, RATES_ON_FIX, VOLS_ON_FIX);
    double expectedCaplet = 0d;
    double expectedFloorlet = 0d;
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), expectedCaplet, TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), expectedFloorlet, TOL);
  }

  public void test_presentValueTheta_afterFix() {
    CurrencyAmount computedCaplet = PRICER.presentValueTheta(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    CurrencyAmount computedFloorlet = PRICER.presentValueTheta(FLOORLET_SHORT, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    assertEquals(computedCaplet.getCurrency(), EUR);
    assertEquals(computedCaplet.getAmount(), 0d, TOL);
    assertEquals(computedFloorlet.getCurrency(), EUR);
    assertEquals(computedFloorlet.getAmount(), 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder pointCaplet = PRICER.presentValueSensitivityRatesStickyModel(CAPLET_LONG, RATES, VOLS);
    CurrencyParameterSensitivities computedCaplet = RATES.parameterSensitivity(pointCaplet.build());
    PointSensitivityBuilder pointFloorlet = PRICER.presentValueSensitivityRatesStickyModel(FLOORLET_SHORT, RATES, VOLS);
    CurrencyParameterSensitivities computedFloorlet = RATES.parameterSensitivity(pointFloorlet.build());
    CurrencyParameterSensitivities expectedCaplet =
        FD_CAL.sensitivity(RATES, p -> PRICER_BASE.presentValue(CAPLET_LONG, p, VOLS));
    CurrencyParameterSensitivities expectedFloorlet =
        FD_CAL.sensitivity(RATES, p -> PRICER_BASE.presentValue(FLOORLET_SHORT, p, VOLS));
    assertTrue(computedCaplet.equalWithTolerance(expectedCaplet, EPS_FD * NOTIONAL * 50d));
    assertTrue(computedFloorlet.equalWithTolerance(expectedFloorlet, EPS_FD * NOTIONAL * 50d));
    // consistency with shifted Black
    PointSensitivityBuilder pointCapletBase = PRICER.presentValueSensitivityRates(CAPLET_LONG, RATES, VOLS);
    PointSensitivityBuilder pointFloorletBase = PRICER.presentValueSensitivityRates(FLOORLET_SHORT, RATES, VOLS);
    double forward = RATES.iborIndexRates(EUR_EURIBOR_3M).rate(RATE_COMP.getObservation());
    double expiry = VOLS.relativeTime(CAPLET_LONG.getFixingDateTime());
    double volatility = VOLS.volatility(expiry, STRIKE, forward);
    ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities vols = ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(
        EUR_EURIBOR_3M, VALUATION, ConstantSurface.of("constVol", volatility)
            .withMetadata(Surfaces.blackVolatilityByExpiryStrike("costVol", DayCounts.ACT_ACT_ISDA)),
        IborCapletFloorletSabrRateVolatilityDataSet.CURVE_CONST_SHIFT);
    PointSensitivityBuilder pointCapletExp = PRICER_BASE.presentValueSensitivityRates(CAPLET_LONG, RATES, vols);
    PointSensitivityBuilder pointFloorletExp = PRICER_BASE.presentValueSensitivityRates(FLOORLET_SHORT, RATES, vols);
    assertEquals(pointCapletBase, pointCapletExp);
    assertEquals(pointFloorletBase, pointFloorletExp);
  }

  public void test_presentValueSensitivity_onFix() {
    PointSensitivityBuilder pointCaplet = PRICER.presentValueSensitivityRatesStickyModel(CAPLET_LONG, RATES_ON_FIX, VOLS_ON_FIX);
    CurrencyParameterSensitivities computedCaplet = RATES_ON_FIX.parameterSensitivity(pointCaplet.build());
    PointSensitivityBuilder pointFloorlet =
        PRICER.presentValueSensitivityRatesStickyModel(FLOORLET_SHORT, RATES_ON_FIX, VOLS_ON_FIX);
    CurrencyParameterSensitivities computedFloorlet =
        RATES_ON_FIX.parameterSensitivity(pointFloorlet.build());
    CurrencyParameterSensitivities expectedCaplet =
        FD_CAL.sensitivity(RATES_ON_FIX, p -> PRICER_BASE.presentValue(CAPLET_LONG, p, VOLS_ON_FIX));
    CurrencyParameterSensitivities expectedFloorlet =
        FD_CAL.sensitivity(RATES_ON_FIX, p -> PRICER_BASE.presentValue(FLOORLET_SHORT, p, VOLS_ON_FIX));
    assertTrue(computedCaplet.equalWithTolerance(expectedCaplet, EPS_FD * NOTIONAL));
    assertTrue(computedFloorlet.equalWithTolerance(expectedFloorlet, EPS_FD * NOTIONAL));
  }

  public void test_presentValueSensitivity_afterFix() {
    PointSensitivityBuilder pointCaplet =
        PRICER.presentValueSensitivityRatesStickyModel(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    CurrencyParameterSensitivities computedCaplet = RATES_AFTER_FIX.parameterSensitivity(pointCaplet.build());
    PointSensitivityBuilder pointFloorlet =
        PRICER.presentValueSensitivityRatesStickyModel(FLOORLET_SHORT, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    CurrencyParameterSensitivities computedFloorlet =
        RATES_AFTER_FIX.parameterSensitivity(pointFloorlet.build());
    CurrencyParameterSensitivities expectedCaplet =
        FD_CAL.sensitivity(RATES_AFTER_FIX, p -> PRICER_BASE.presentValue(CAPLET_LONG, p, VOLS_AFTER_FIX));
    CurrencyParameterSensitivities expectedFloorlet =
        FD_CAL.sensitivity(RATES_AFTER_FIX, p -> PRICER_BASE.presentValue(FLOORLET_SHORT, p, VOLS_AFTER_FIX));
    assertTrue(computedCaplet.equalWithTolerance(expectedCaplet, EPS_FD * NOTIONAL));
    assertTrue(computedFloorlet.equalWithTolerance(expectedFloorlet, EPS_FD * NOTIONAL));
  }

  public void test_presentValueSensitivity_afterPay() {
    PointSensitivityBuilder computedCaplet =
        PRICER.presentValueSensitivityRatesStickyModel(CAPLET_LONG, RATES_AFTER_PAY, VOLS_AFTER_PAY);
    PointSensitivityBuilder computedFloorlet =
        PRICER.presentValueSensitivityRatesStickyModel(FLOORLET_SHORT, RATES_AFTER_PAY, VOLS_AFTER_PAY);
    assertEquals(computedCaplet, PointSensitivityBuilder.none());
    assertEquals(computedFloorlet, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityVolatility() {
    PointSensitivities pointCaplet = PRICER.presentValueSensitivityModelParamsSabr(CAPLET_LONG, RATES, VOLS).build();
    PointSensitivities pointFloorlet = PRICER.presentValueSensitivityModelParamsSabr(FLOORLET_SHORT, RATES, VOLS).build();
    double forward = RATES.iborIndexRates(EUR_EURIBOR_3M).rate(RATE_COMP.getObservation());
    double expiry = VOLS.relativeTime(CAPLET_LONG.getFixingDateTime());
    ValueDerivatives volSensi = VOLS.getParameters().volatilityAdjoint(expiry, STRIKE, forward);
    double df = RATES.discountFactor(EUR, CAPLET_LONG.getPaymentDate());
    double vegaCaplet = NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.vega(forward + SHIFT, STRIKE + SHIFT, expiry, volSensi.getValue());
    double vegaFloorlet = -NOTIONAL * df * CAPLET_LONG.getYearFraction() *
        BlackFormulaRepository.vega(forward + SHIFT, STRIKE + SHIFT, expiry, volSensi.getValue());
    assertSensitivity(pointCaplet, SabrParameterType.ALPHA, vegaCaplet * volSensi.getDerivative(2), TOL);
    assertSensitivity(pointCaplet, SabrParameterType.BETA, vegaCaplet * volSensi.getDerivative(3), TOL);
    assertSensitivity(pointCaplet, SabrParameterType.RHO, vegaCaplet * volSensi.getDerivative(4), TOL);
    assertSensitivity(pointCaplet, SabrParameterType.NU, vegaCaplet * volSensi.getDerivative(5), TOL);
    assertSensitivity(pointFloorlet, SabrParameterType.ALPHA, vegaFloorlet * volSensi.getDerivative(2), TOL);
    assertSensitivity(pointFloorlet, SabrParameterType.BETA, vegaFloorlet * volSensi.getDerivative(3), TOL);
    assertSensitivity(pointFloorlet, SabrParameterType.RHO, vegaFloorlet * volSensi.getDerivative(4), TOL);
    assertSensitivity(pointFloorlet, SabrParameterType.NU, vegaFloorlet * volSensi.getDerivative(5), TOL);
    PointSensitivities pointCapletVol = PRICER.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES, VOLS).build();
    // vol sensitivity in base class
    PointSensitivities pointFloorletVol =
        PRICER.presentValueSensitivityModelParamsVolatility(FLOORLET_SHORT, RATES, VOLS).build();
    IborCapletFloorletSensitivity pointCapletVolExp =
        IborCapletFloorletSensitivity.of(VOLS.getName(), expiry, STRIKE, forward, EUR, vegaCaplet);
    IborCapletFloorletSensitivity pointFloorletVolExp =
        IborCapletFloorletSensitivity.of(VOLS.getName(), expiry, STRIKE, forward, EUR, vegaFloorlet);
    assertEquals(pointCapletVol.getSensitivities().get(0), pointCapletVolExp);
    assertEquals(pointFloorletVol.getSensitivities().get(0), pointFloorletVolExp);
  }

  private void assertSensitivity(PointSensitivities points, SabrParameterType type, double expected, double tol) {
    for (PointSensitivity point : points.getSensitivities()) {
      IborCapletFloorletSabrSensitivity sens = (IborCapletFloorletSabrSensitivity) point;
      assertEquals(sens.getCurrency(), EUR);
      assertEquals(sens.getVolatilitiesName(), VOLS.getName());
      if (sens.getSensitivityType() == type) {
        assertEquals(sens.getSensitivity(), expected, NOTIONAL * tol);
        return;
      }
    }
    fail("Did not find sensitivity: " + type + " in " + points);
  }

  public void test_presentValueSensitivityVolatility_onFix() {
    PointSensitivityBuilder computedCaplet =
        PRICER.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES_ON_FIX, VOLS_ON_FIX);
    PointSensitivityBuilder computedFloorlet =
        PRICER.presentValueSensitivityModelParamsVolatility(FLOORLET_SHORT, RATES_ON_FIX, VOLS_ON_FIX);
    assertEquals(computedCaplet, PointSensitivityBuilder.none());
    assertEquals(computedFloorlet, PointSensitivityBuilder.none());

  }

  public void test_presentValueSensitivityVolatility_afterFix() {
    PointSensitivityBuilder computedCaplet =
        PRICER.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    PointSensitivityBuilder computedFloorlet =
        PRICER.presentValueSensitivityModelParamsVolatility(FLOORLET_SHORT, RATES_AFTER_FIX, VOLS_AFTER_FIX);
    assertEquals(computedCaplet, PointSensitivityBuilder.none());
    assertEquals(computedFloorlet, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_fail_Black() {
    assertThrowsIllegalArg(() -> PRICER.presentValue(CAPLET_LONG, RATES, VOLS_BLACK));
    assertThrowsIllegalArg(() -> PRICER.impliedVolatility(CAPLET_LONG, RATES, VOLS_BLACK));
    assertThrowsIllegalArg(() -> PRICER.presentValueDelta(CAPLET_LONG, RATES, VOLS_BLACK));
    assertThrowsIllegalArg(() -> PRICER.presentValueGamma(CAPLET_LONG, RATES, VOLS_BLACK));
    assertThrowsIllegalArg(() -> PRICER.presentValueTheta(CAPLET_LONG, RATES, VOLS_BLACK));
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityRates(CAPLET_LONG, RATES, VOLS_BLACK));
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityModelParamsVolatility(CAPLET_LONG, RATES, VOLS_BLACK));
  }

}
