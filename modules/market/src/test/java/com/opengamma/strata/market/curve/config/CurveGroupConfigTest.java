/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.config;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.rate.fra.FraTemplate;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.value.ValueType;

/**
 * Test {@link CurveGroupConfig}.
 */
@Test
public class CurveGroupConfigTest {

  private static final InterpolatedCurveConfig CURVE_CONFIG =
      InterpolatedCurveConfig.builder()
          .name(CurveName.of("Test"))
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(ACT_365F)
          .nodes(ImmutableList.of(
              FraCurveNode.of(
                  FraTemplate.of(Period.ofMonths(1), GBP_LIBOR_1M),
                  QuoteKey.of(StandardId.of("OG", "Ticker")))))
          .interpolator(Interpolator1DFactory.LINEAR_INSTANCE)
          .extrapolatorLeft(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
          .extrapolatorRight(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
          .build();
  private static final InterpolatedCurveConfig CURVE_CONFIG2 = CURVE_CONFIG.toBuilder()
      .name(CurveName.of("Test2"))
      .build();
  private static final CurveGroupEntry ENTRY1 = CurveGroupEntry.builder()
      .curveConfig(CURVE_CONFIG)
      .discountCurrencies(GBP)
      .build();
  private static final CurveGroupEntry ENTRY2 = CurveGroupEntry.builder()
      .curveConfig(CURVE_CONFIG2)
      .iborIndices(GBP_LIBOR_1M, GBP_LIBOR_3M)
      .build();
  private static final CurveGroupEntry ENTRY3 = CurveGroupEntry.builder()
      .curveConfig(CURVE_CONFIG)
      .discountCurrencies(GBP)
      .iborIndices(GBP_LIBOR_1M, GBP_LIBOR_3M)
      .build();

  public void test_builder1() {
    CurveGroupConfig test = CurveGroupConfig.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_CONFIG, GBP)
        .addForwardCurve(CURVE_CONFIG2, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertEquals(test.getName(), CurveGroupName.of("Test"));
    assertEquals(test.getEntries(), ImmutableList.of(ENTRY1, ENTRY2));
    assertEquals(test.getEntry(CurveName.of("Test")), Optional.of(ENTRY1));
    assertEquals(test.getEntry(CurveName.of("Test2")), Optional.of(ENTRY2));
    assertEquals(test.getEntry(CurveName.of("Rubbish")), Optional.empty());
  }

  public void test_builder2() {
    CurveGroupConfig test = CurveGroupConfig.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_CONFIG, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertEquals(test.getName(), CurveGroupName.of("Test"));
    assertEquals(test.getEntries(), ImmutableList.of(ENTRY3));
    assertEquals(test.getEntry(CurveName.of("Test")), Optional.of(ENTRY3));
    assertEquals(test.getEntry(CurveName.of("Test2")), Optional.empty());
    assertEquals(test.getEntry(CurveName.of("Rubbish")), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroupConfig test = CurveGroupConfig.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_CONFIG, GBP)
        .build();
    coverImmutableBean(test);
    CurveGroupConfig test2 = CurveGroupConfig.builder()
        .name(CurveGroupName.of("Test2"))
        .addForwardCurve(CURVE_CONFIG2, GBP_LIBOR_1M)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroupConfig test = CurveGroupConfig.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_CONFIG, GBP)
        .build();
    assertSerialization(test);
  }

}
