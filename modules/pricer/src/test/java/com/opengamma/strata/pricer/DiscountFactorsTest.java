/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;

/**
 * Test {@link DiscountFactors}.
 */
public class DiscountFactorsTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final InterpolatedNodalCurve CURVE_DF = InterpolatedNodalCurve.of(
      Curves.discountFactors(NAME, ACT_365F), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE_ZERO = InterpolatedNodalCurve.of(
      Curves.zeroRates(NAME, ACT_365F), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final CurveMetadata META_ZERO_PERIODIC = DefaultCurveMetadata.builder()
      .curveName(NAME)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .dayCount(ACT_365F)
      .addInfo(CurveInfoType.COMPOUNDING_PER_YEAR, 2)
      .build();
  private static final InterpolatedNodalCurve CURVE_ZERO_PERIODIC = InterpolatedNodalCurve.of(
      META_ZERO_PERIODIC, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE_PRICES = InterpolatedNodalCurve.of(
      Curves.prices(NAME), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_discountFactors() {
    DiscountFactors test = DiscountFactors.of(GBP, DATE_VAL, CURVE_DF);
    assertThat(test instanceof SimpleDiscountFactors).isTrue();
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
  }

  @Test
  public void test_of_zeroRate() {
    DiscountFactors test = DiscountFactors.of(GBP, DATE_VAL, CURVE_ZERO);
    assertThat(test instanceof ZeroRateDiscountFactors).isTrue();
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
  }

  @Test
  public void test_of_zeroRatePeriodic() {
    DiscountFactors test = DiscountFactors.of(GBP, DATE_VAL, CURVE_ZERO_PERIODIC);
    assertThat(test instanceof ZeroRatePeriodicDiscountFactors).isTrue();
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
  }

  @Test
  public void test_of_prices() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DiscountFactors.of(GBP, DATE_VAL, CURVE_PRICES));
  }

}
