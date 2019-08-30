/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.CompoundedRateType.PERIODIC;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.product.bond.FixedCouponBondPaymentPeriod;

/**
 * Test {@link DiscountingFixedCouponBondPaymentPeriodPricer}.
 */
public class DiscountingFixedCouponBondPaymentPeriodPricerTest {

  // issuer curves
  private static final LocalDate VAL_DATE = date(2015, 1, 28);
  private static final LocalDate VAL_DATE_AFTER = date(2015, 8, 28);
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final CurveMetadata METADATA = Curves.zeroRates(NAME, ACT_365F);
  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(0.1, 0.18), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS = ZeroRateDiscountFactors.of(GBP, VAL_DATE, CURVE);
  private static final DiscountFactors DSC_FACTORS_AFTER = ZeroRateDiscountFactors.of(GBP, VAL_DATE_AFTER, CURVE);
  private static final LegalEntityGroup GROUP = LegalEntityGroup.of("ISSUER1");
  private static final IssuerCurveDiscountFactors ISSUER_CURVE = IssuerCurveDiscountFactors.of(DSC_FACTORS, GROUP);
  private static final IssuerCurveDiscountFactors ISSUER_CURVE_AFTER =
      IssuerCurveDiscountFactors.of(DSC_FACTORS_AFTER, GROUP);
  // coupon payment
  private static final LocalDate START = LocalDate.of(2015, 2, 2);
  private static final LocalDate END = LocalDate.of(2015, 8, 2);
  private static final LocalDate START_ADJUSTED = LocalDate.of(2015, 2, 2);
  private static final LocalDate END_ADJUSTED = LocalDate.of(2015, 8, 3);
  private static final double FIXED_RATE = 0.025;
  private static final double NOTIONAL = 1.0e7;
  private static final double YEAR_FRACTION = 0.51;
  private static final FixedCouponBondPaymentPeriod PAYMENT_PERIOD = FixedCouponBondPaymentPeriod.builder()
      .currency(USD)
      .startDate(START_ADJUSTED)
      .unadjustedStartDate(START)
      .endDate(END_ADJUSTED)
      .unadjustedEndDate(END)
      .notional(NOTIONAL)
      .fixedRate(FIXED_RATE)
      .yearFraction(YEAR_FRACTION)
      .build();
  /// z-spread
  private static final double Z_SPREAD = 0.02;
  private static final int PERIOD_PER_YEAR = 4;

  private static final DiscountingFixedCouponBondPaymentPeriodPricer PRICER =
      DiscountingFixedCouponBondPaymentPeriodPricer.DEFAULT;
  private static final double TOL = 1.0e-12;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    double computed = PRICER.presentValue(PAYMENT_PERIOD, ISSUER_CURVE);
    double expected = FIXED_RATE * NOTIONAL * YEAR_FRACTION * DSC_FACTORS.discountFactor(END_ADJUSTED);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_presentValueWithSpread() {
    double computed = PRICER.presentValueWithSpread(
        PAYMENT_PERIOD, ISSUER_CURVE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double expected = FIXED_RATE * NOTIONAL * YEAR_FRACTION *
        DSC_FACTORS.discountFactorWithSpread(END_ADJUSTED, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_forecastValue() {
    double computed = PRICER.forecastValue(PAYMENT_PERIOD, ISSUER_CURVE);
    double expected = FIXED_RATE * NOTIONAL * YEAR_FRACTION;
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_presentValue_past() {
    double computed = PRICER.presentValue(PAYMENT_PERIOD, ISSUER_CURVE_AFTER);
    assertThat(computed).isEqualTo(0d);
  }

  @Test
  public void test_presentValueWithSpread_past() {
    double computed = PRICER.presentValueWithSpread(
        PAYMENT_PERIOD, ISSUER_CURVE_AFTER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed).isEqualTo(0d);
  }

  @Test
  public void test_forecastValue_past() {
    double computed = PRICER.forecastValue(PAYMENT_PERIOD, ISSUER_CURVE_AFTER);
    assertThat(computed).isEqualTo(0d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder computed = PRICER.presentValueSensitivity(PAYMENT_PERIOD, ISSUER_CURVE);
    PointSensitivityBuilder expected = IssuerCurveZeroRateSensitivity.of(
        DSC_FACTORS.zeroRatePointSensitivity(END_ADJUSTED).multipliedBy(FIXED_RATE * NOTIONAL * YEAR_FRACTION), GROUP);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_presentValueSensitivityWithSpread() {
    PointSensitivityBuilder computed = PRICER.presentValueSensitivityWithSpread(
        PAYMENT_PERIOD, ISSUER_CURVE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    PointSensitivityBuilder expected = IssuerCurveZeroRateSensitivity.of(
        DSC_FACTORS.zeroRatePointSensitivityWithSpread(END_ADJUSTED, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)
            .multipliedBy(FIXED_RATE * NOTIONAL * YEAR_FRACTION), GROUP);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_forecastValueSensitivity() {
    PointSensitivityBuilder computed = PRICER.forecastValueSensitivity(PAYMENT_PERIOD, ISSUER_CURVE);
    assertThat(computed).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_presentValueSensitivity_past() {
    PointSensitivityBuilder computed = PRICER.presentValueSensitivity(PAYMENT_PERIOD, ISSUER_CURVE_AFTER);
    assertThat(computed).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_presentValueSensitivityWithSpread_past() {
    PointSensitivityBuilder computed = PRICER.presentValueSensitivityWithSpread(
        PAYMENT_PERIOD, ISSUER_CURVE_AFTER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertThat(computed).isEqualTo(PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_explainPresentValue() {
    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValue(PAYMENT_PERIOD, ISSUER_CURVE, builder);
    ExplainMap explain = builder.build();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("FixedCouponBondPaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_PERIOD.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(PAYMENT_PERIOD.getCurrency());
    assertThat(explain.get(ExplainKey.START_DATE).get()).isEqualTo(START_ADJUSTED);
    assertThat(explain.get(ExplainKey.UNADJUSTED_START_DATE).get()).isEqualTo(START);
    assertThat(explain.get(ExplainKey.END_DATE).get()).isEqualTo(END_ADJUSTED);
    assertThat(explain.get(ExplainKey.UNADJUSTED_END_DATE).get()).isEqualTo(END);
    assertThat(explain.get(ExplainKey.DAYS).get().intValue()).isEqualTo((int) DAYS.between(START_ADJUSTED, END_ADJUSTED));
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isEqualTo(DSC_FACTORS.discountFactor(END_ADJUSTED));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(FIXED_RATE * NOTIONAL * YEAR_FRACTION, offset(NOTIONAL * TOL));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(FIXED_RATE * NOTIONAL * YEAR_FRACTION * DSC_FACTORS.discountFactor(END_ADJUSTED), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_explainPresentValue_past() {
    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValue(PAYMENT_PERIOD, ISSUER_CURVE_AFTER, builder);
    ExplainMap explain = builder.build();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("FixedCouponBondPaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_PERIOD.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(PAYMENT_PERIOD.getCurrency());
    assertThat(explain.get(ExplainKey.START_DATE).get()).isEqualTo(START_ADJUSTED);
    assertThat(explain.get(ExplainKey.UNADJUSTED_START_DATE).get()).isEqualTo(START);
    assertThat(explain.get(ExplainKey.END_DATE).get()).isEqualTo(END_ADJUSTED);
    assertThat(explain.get(ExplainKey.UNADJUSTED_END_DATE).get()).isEqualTo(END);
    assertThat(explain.get(ExplainKey.DAYS).get().intValue()).isEqualTo((int) DAYS.between(START_ADJUSTED, END_ADJUSTED));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

  @Test
  public void test_explainPresentValueWithSpread() {
    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValueWithSpread(
        PAYMENT_PERIOD, ISSUER_CURVE, builder, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    ExplainMap explain = builder.build();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("FixedCouponBondPaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_PERIOD.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(PAYMENT_PERIOD.getCurrency());
    assertThat(explain.get(ExplainKey.START_DATE).get()).isEqualTo(START_ADJUSTED);
    assertThat(explain.get(ExplainKey.UNADJUSTED_START_DATE).get()).isEqualTo(START);
    assertThat(explain.get(ExplainKey.END_DATE).get()).isEqualTo(END_ADJUSTED);
    assertThat(explain.get(ExplainKey.UNADJUSTED_END_DATE).get()).isEqualTo(END);
    assertThat(explain.get(ExplainKey.DAYS).get().intValue()).isEqualTo((int) DAYS.between(START_ADJUSTED, END_ADJUSTED));
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isEqualTo(DSC_FACTORS.discountFactorWithSpread(END_ADJUSTED, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(FIXED_RATE * NOTIONAL * YEAR_FRACTION, offset(NOTIONAL * TOL));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(FIXED_RATE * NOTIONAL * YEAR_FRACTION *
        DSC_FACTORS.discountFactorWithSpread(END_ADJUSTED, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR), offset(NOTIONAL * TOL));
  }

  @Test
  public void test_explainPresentValueWithSpread_past() {
    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValueWithSpread(
        PAYMENT_PERIOD, ISSUER_CURVE_AFTER, builder, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    ExplainMap explain = builder.build();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("FixedCouponBondPaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_PERIOD.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(PAYMENT_PERIOD.getCurrency());
    assertThat(explain.get(ExplainKey.START_DATE).get()).isEqualTo(START_ADJUSTED);
    assertThat(explain.get(ExplainKey.UNADJUSTED_START_DATE).get()).isEqualTo(START);
    assertThat(explain.get(ExplainKey.END_DATE).get()).isEqualTo(END_ADJUSTED);
    assertThat(explain.get(ExplainKey.UNADJUSTED_END_DATE).get()).isEqualTo(END);
    assertThat(explain.get(ExplainKey.DAYS).get().intValue()).isEqualTo((int) DAYS.between(START_ADJUSTED, END_ADJUSTED));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(0d, offset(NOTIONAL * TOL));
  }

}
