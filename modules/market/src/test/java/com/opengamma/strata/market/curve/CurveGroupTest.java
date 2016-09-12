/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.CHF_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_2M;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;

/**
 * Test {@link CurveGroup}.
 */
@Test
public class CurveGroupTest {

  private static final CurveGroupName NAME = CurveGroupName.of("TestGroup");
  private static final CurveGroupName NAME2 = CurveGroupName.of("TestGroup2");
  private static final CurveName DISCOUNT_NAME = CurveName.of("Discount");
  private static final CurveName IBOR_NAME = CurveName.of("Ibor");
  private static final CurveName OVERNIGHT_NAME = CurveName.of("Overnight");
  private static final Curve DISCOUNT_CURVE = ConstantCurve.of("Discount", 0.99);
  private static final Map<Currency, Curve> DISCOUNT_CURVES = ImmutableMap.of(GBP, DISCOUNT_CURVE);
  private static final Curve IBOR_CURVE = ConstantCurve.of("Ibor", 0.5);
  private static final Curve OVERNIGHT_CURVE = ConstantCurve.of("Overnight", 0.6);
  private static final Map<Index, Curve> IBOR_CURVES = ImmutableMap.of(GBP_LIBOR_3M, IBOR_CURVE);

  //-------------------------------------------------------------------------
  public void test_of() {
    CurveGroup test = CurveGroup.of(NAME, DISCOUNT_CURVES, IBOR_CURVES);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getDiscountCurves()).isEqualTo(DISCOUNT_CURVES);
    assertThat(test.getForwardCurves()).isEqualTo(IBOR_CURVES);
    assertThat(test.findCurve(DISCOUNT_NAME)).hasValue(DISCOUNT_CURVE);
    assertThat(test.findCurve(IBOR_NAME)).hasValue(IBOR_CURVE);
    assertThat(test.findCurve(OVERNIGHT_NAME)).isEmpty();
    assertThat(test.findDiscountCurve(GBP)).hasValue(DISCOUNT_CURVE);
    assertThat(test.findDiscountCurve(USD)).isEmpty();
    assertThat(test.findForwardCurve(GBP_LIBOR_3M)).hasValue(IBOR_CURVE);
    assertThat(test.findForwardCurve(CHF_LIBOR_3M)).isEmpty();
  }

  public void test_builder() {
    CurveGroup test = CurveGroup.builder()
        .name(NAME)
        .discountCurves(DISCOUNT_CURVES)
        .forwardCurves(IBOR_CURVES)
        .build();
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getDiscountCurves()).isEqualTo(DISCOUNT_CURVES);
    assertThat(test.getForwardCurves()).isEqualTo(IBOR_CURVES);
    assertThat(test.findDiscountCurve(GBP)).hasValue(DISCOUNT_CURVE);
    assertThat(test.findDiscountCurve(USD)).isEmpty();
    assertThat(test.findForwardCurve(GBP_LIBOR_3M)).hasValue(IBOR_CURVE);
    assertThat(test.findForwardCurve(CHF_LIBOR_3M)).isEmpty();
  }

  public void test_ofCurves() {
    CurveGroupDefinition definition = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("group"))
        .addCurve(DISCOUNT_NAME, GBP, GBP_LIBOR_1M)
        .addForwardCurve(IBOR_NAME, USD_LIBOR_1M, USD_LIBOR_2M)
        .addForwardCurve(OVERNIGHT_NAME, EUR_EONIA)
        .build();
    CurveGroup group = CurveGroup.ofCurves(definition, DISCOUNT_CURVE, OVERNIGHT_CURVE, IBOR_CURVE);
    assertThat(group.findDiscountCurve(GBP)).hasValue(DISCOUNT_CURVE);
    assertThat(group.findForwardCurve(USD_LIBOR_1M)).hasValue(IBOR_CURVE);
    assertThat(group.findForwardCurve(USD_LIBOR_2M)).hasValue(IBOR_CURVE);
    assertThat(group.findForwardCurve(EUR_EONIA)).hasValue(OVERNIGHT_CURVE);
  }

  public void test_ofCurves_duplicateCurveName() {
    CurveGroupDefinition definition = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("group"))
        .addForwardCurve(IBOR_NAME, USD_LIBOR_1M, USD_LIBOR_2M)
        .build();
    CurveGroup group = CurveGroup.ofCurves(definition, IBOR_CURVE, IBOR_CURVE);
    assertThat(group.findForwardCurve(USD_LIBOR_1M)).hasValue(IBOR_CURVE);
    assertThat(group.findForwardCurve(USD_LIBOR_2M)).hasValue(IBOR_CURVE);
  }

  public void stream() {
    CurveGroup test = CurveGroup.of(NAME, DISCOUNT_CURVES, IBOR_CURVES);
    List<Curve> expected = ImmutableList.<Curve>builder()
        .addAll(DISCOUNT_CURVES.values())
        .addAll(IBOR_CURVES.values())
        .build();
    assertThat(test.stream().collect(toList())).containsOnlyElementsOf(expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroup test = CurveGroup.of(NAME, DISCOUNT_CURVES, IBOR_CURVES);
    coverImmutableBean(test);
    CurveGroup test2 = CurveGroup.of(NAME2, ImmutableMap.of(), ImmutableMap.of());
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroup test = CurveGroup.of(NAME, DISCOUNT_CURVES, IBOR_CURVES);
    assertSerialization(test);
  }

}
