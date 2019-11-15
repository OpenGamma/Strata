/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.swap.KnownAmountSwapPaymentPeriod;

/**
 * Test {@link DiscountingKnownAmountPaymentPeriodPricer}
 */
public class DiscountingKnownAmountPaymentPeriodPricerTest {

  private static final DiscountingKnownAmountPaymentPeriodPricer PRICER = DiscountingKnownAmountPaymentPeriodPricer.DEFAULT;
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
  private static final LocalDate DATE_1 = LocalDate.of(2014, 1, 24);
  private static final LocalDate DATE_2 = LocalDate.of(2014, 4, 25);
  private static final LocalDate DATE_2U = LocalDate.of(2014, 4, 24);
  private static final double AMOUNT_1000 = 1000d;
  private static final CurrencyAmount AMOUNT_GBP1000 = CurrencyAmount.of(GBP, 1000);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2014, 4, 26);
  private static final double DISCOUNT_FACTOR = 0.976d;
  private static final double TOLERANCE_PV = 1E-7;

  private static final Payment PAYMENT = Payment.of(AMOUNT_GBP1000, PAYMENT_DATE);
  private static final Payment PAYMENT_PAST = Payment.of(AMOUNT_GBP1000, VAL_DATE.minusDays(1));

  private static final KnownAmountSwapPaymentPeriod PERIOD = KnownAmountSwapPaymentPeriod.builder()
      .payment(PAYMENT)
      .startDate(DATE_1)
      .endDate(DATE_2)
      .unadjustedEndDate(DATE_2U)
      .build();
  private static final KnownAmountSwapPaymentPeriod PERIOD_PAST = KnownAmountSwapPaymentPeriod.builder()
      .payment(PAYMENT_PAST)
      .startDate(DATE_1)
      .endDate(DATE_2)
      .build();

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.DOUBLE_QUADRATIC;
  private static final Curve DISCOUNT_CURVE_GBP;
  static {
    DoubleArray timeGbp = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0);
    DoubleArray rateGbp = DoubleArray.of(0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210);
    DISCOUNT_CURVE_GBP = InterpolatedNodalCurve.of(
        Curves.zeroRates("GBP-Discount", ACT_ACT_ISDA), timeGbp, rateGbp, INTERPOLATOR);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double pvExpected = AMOUNT_1000 * DISCOUNT_FACTOR;
    double pvComputed = PRICER.presentValue(PERIOD, prov);
    assertThat(pvComputed).isCloseTo(pvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_presentValue_inPast() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double pvComputed = PRICER.presentValue(PERIOD_PAST, prov);
    assertThat(pvComputed).isCloseTo(0, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValue() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double fvExpected = AMOUNT_1000;
    double fvComputed = PRICER.forecastValue(PERIOD, prov);
    assertThat(fvComputed).isCloseTo(fvExpected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_forecastValue_inPast() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    double fvComputed = PRICER.forecastValue(PERIOD_PAST, prov);
    assertThat(fvComputed).isCloseTo(0, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    PointSensitivities point = PRICER.presentValueSensitivity(PERIOD, prov).build();
    double relativeYearFraction = DAY_COUNT.relativeYearFraction(VAL_DATE, PAYMENT_DATE);
    double expected = -DISCOUNT_FACTOR * relativeYearFraction * AMOUNT_1000;
    ZeroRateSensitivity actual = (ZeroRateSensitivity) point.getSensitivities().get(0);
    assertThat(actual.getCurrency()).isEqualTo(GBP);
    assertThat(actual.getCurveCurrency()).isEqualTo(GBP);
    assertThat(actual.getYearFraction()).isEqualTo(relativeYearFraction);
    assertThat(actual.getSensitivity()).isCloseTo(expected, offset(AMOUNT_1000 * TOLERANCE_PV));
  }

  @Test
  public void test_presentValueSensitivity_inPast() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    PointSensitivities computed = PRICER.presentValueSensitivity(PERIOD_PAST, prov)
        .build();
    assertThat(computed).isEqualTo(PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValueSensitivity() {
    SimpleRatesProvider prov = createProvider(VAL_DATE);

    assertThat(PRICER.forecastValueSensitivity(PERIOD, prov)).isEqualTo(PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_accruedInterest() {
    LocalDate valDate = PERIOD.getStartDate().plusDays(7);
    SimpleRatesProvider prov = createProvider(valDate);

    double expected = AMOUNT_1000 * (7d / (7 + 28 + 31 + 25));
    double computed = PRICER.accruedInterest(PERIOD, prov);
    assertThat(computed).isCloseTo(expected, offset(TOLERANCE_PV));
  }

  @Test
  public void test_accruedInterest_valDateBeforePeriod() {
    SimpleRatesProvider prov = createProvider(PERIOD.getStartDate());

    double computed = PRICER.accruedInterest(PERIOD, prov);
    assertThat(computed).isCloseTo(0, offset(TOLERANCE_PV));
  }

  @Test
  public void test_accruedInterest_valDateAfterPeriod() {
    SimpleRatesProvider prov = createProvider(PERIOD.getEndDate().plusDays(1));

    double computed = PRICER.accruedInterest(PERIOD, prov);
    assertThat(computed).isCloseTo(0, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_explainPresentValue() {
    RatesProvider prov = createProvider(VAL_DATE);

    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValue(PERIOD, prov, builder);
    ExplainMap explain = builder.build();

    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("KnownAmountPaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PERIOD.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(PERIOD.getCurrency());
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isCloseTo(DISCOUNT_FACTOR, offset(TOLERANCE_PV));

    int daysBetween = (int) DAYS.between(DATE_1, DATE_2);
    assertThat(explain.get(ExplainKey.START_DATE).get()).isEqualTo(PERIOD.getStartDate());
    assertThat(explain.get(ExplainKey.UNADJUSTED_START_DATE).get()).isEqualTo(PERIOD.getUnadjustedStartDate());
    assertThat(explain.get(ExplainKey.END_DATE).get()).isEqualTo(PERIOD.getEndDate());
    assertThat(explain.get(ExplainKey.UNADJUSTED_END_DATE).get()).isEqualTo(PERIOD.getUnadjustedEndDate());
    assertThat(explain.get(ExplainKey.DAYS).get()).isEqualTo((Integer) daysBetween);

    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(PERIOD.getCurrency());
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(AMOUNT_1000, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(PERIOD.getCurrency());
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(AMOUNT_1000 * DISCOUNT_FACTOR, offset(TOLERANCE_PV));
  }

  @Test
  public void test_explainPresentValue_inPast() {
    RatesProvider prov = createProvider(VAL_DATE);

    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValue(PERIOD_PAST, prov, builder);
    ExplainMap explain = builder.build();

    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("KnownAmountPaymentPeriod");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PERIOD_PAST.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(PERIOD_PAST.getCurrency());

    int daysBetween = (int) DAYS.between(DATE_1, DATE_2);
    assertThat(explain.get(ExplainKey.START_DATE).get()).isEqualTo(PERIOD_PAST.getStartDate());
    assertThat(explain.get(ExplainKey.UNADJUSTED_START_DATE).get()).isEqualTo(PERIOD_PAST.getUnadjustedStartDate());
    assertThat(explain.get(ExplainKey.END_DATE).get()).isEqualTo(PERIOD_PAST.getEndDate());
    assertThat(explain.get(ExplainKey.UNADJUSTED_END_DATE).get()).isEqualTo(PERIOD_PAST.getUnadjustedEndDate());
    assertThat(explain.get(ExplainKey.DAYS).get()).isEqualTo((Integer) daysBetween);

    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(PERIOD_PAST.getCurrency());
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(0, offset(TOLERANCE_PV));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(PERIOD_PAST.getCurrency());
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(0 * DISCOUNT_FACTOR, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .build();
    MultiCurrencyAmount computed = PRICER.currencyExposure(PERIOD, prov);
    PointSensitivities point = PRICER.presentValueSensitivity(PERIOD, prov).build();
    MultiCurrencyAmount expected = prov.currencyExposure(point)
        .plus(CurrencyAmount.of(GBP, PRICER.presentValue(PERIOD, prov)));
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_currentCash_zero() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .build();
    double computed = PRICER.currentCash(PERIOD, prov);
    assertThat(computed).isEqualTo(0d);
  }

  @Test
  public void test_currentCash_onPayment() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(PERIOD.getPaymentDate())
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .build();
    double computed = PRICER.currentCash(PERIOD, prov);
    assertThat(computed).isEqualTo(AMOUNT_1000);
  }

  //-------------------------------------------------------------------------
  // creates a simple provider
  private SimpleRatesProvider createProvider(LocalDate valDate) {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", DAY_COUNT), DISCOUNT_FACTOR);
    DiscountFactors df = SimpleDiscountFactors.of(GBP, valDate, curve);
    SimpleRatesProvider prov = new SimpleRatesProvider(valDate);
    prov.setDayCount(DAY_COUNT);
    prov.setDiscountFactors(df);
    return prov;
  }

}
