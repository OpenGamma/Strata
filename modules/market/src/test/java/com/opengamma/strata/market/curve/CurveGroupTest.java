/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.CHF_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.CollectProjectAssertions;

/**
 * Test {@link CurveGroup}.
 */
@Test
public class CurveGroupTest {

  private static final CurveGroupName NAME = CurveGroupName.of("Test");
  private static final CurveGroupName NAME2 = CurveGroupName.of("Test2");
  private static final Curve DISCOUNT_CURVE = ConstantNodalCurve.of("Discount", 0.99);
  private static final Map<Currency, Curve> DISCOUNT_CURVES = ImmutableMap.of(GBP, DISCOUNT_CURVE);
  private static final Curve FORWARD_CURVE = ConstantNodalCurve.of("Forward", 0.5);
  private static final Map<Index, Curve> FORWARD_CURVES = ImmutableMap.of(GBP_LIBOR_3M, FORWARD_CURVE);

  //-------------------------------------------------------------------------
  public void test_of() {
    CurveGroup test = CurveGroup.of(NAME, DISCOUNT_CURVES, FORWARD_CURVES);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getDiscountCurves()).isEqualTo(DISCOUNT_CURVES);
    assertThat(test.getForwardCurves()).isEqualTo(FORWARD_CURVES);
    CollectProjectAssertions.assertThat(test.findDiscountCurve(GBP)).hasValue(DISCOUNT_CURVE);
    CollectProjectAssertions.assertThat(test.findDiscountCurve(USD)).isEmpty();
    CollectProjectAssertions.assertThat(test.findForwardCurve(GBP_LIBOR_3M)).hasValue(FORWARD_CURVE);
    CollectProjectAssertions.assertThat(test.findForwardCurve(CHF_LIBOR_3M)).isEmpty();
  }

  public void test_builder() {
    CurveGroup test = CurveGroup.builder()
        .name(NAME)
        .discountCurves(DISCOUNT_CURVES)
        .forwardCurves(FORWARD_CURVES)
        .build();
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getDiscountCurves()).isEqualTo(DISCOUNT_CURVES);
    assertThat(test.getForwardCurves()).isEqualTo(FORWARD_CURVES);
    CollectProjectAssertions.assertThat(test.findDiscountCurve(GBP)).hasValue(DISCOUNT_CURVE);
    CollectProjectAssertions.assertThat(test.findDiscountCurve(USD)).isEmpty();
    CollectProjectAssertions.assertThat(test.findForwardCurve(GBP_LIBOR_3M)).hasValue(FORWARD_CURVE);
    CollectProjectAssertions.assertThat(test.findForwardCurve(CHF_LIBOR_3M)).isEmpty();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroup test = CurveGroup.of(NAME, DISCOUNT_CURVES, FORWARD_CURVES);
    coverImmutableBean(test);
    CurveGroup test2 = CurveGroup.of(NAME2, ImmutableMap.of(), ImmutableMap.of());
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroup test = CurveGroup.of(NAME, DISCOUNT_CURVES, FORWARD_CURVES);
    assertSerialization(test);
  }

}
