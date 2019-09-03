/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.Tenor.TENOR_15Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_5Y;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.IsdaCreditCurveDefinition;
import com.opengamma.strata.market.curve.IsdaCreditCurveNode;
import com.opengamma.strata.market.curve.SwapIsdaCreditCurveNode;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.observable.QuoteId;

/**
 * Test {@code IsdaCreditCurveDefinition}.
 */
public class IsdaCreditCurveDefinitionTest {

  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final LocalDate CURVE_VALUATION_DATE = LocalDate.of(2014, 2, 3);
  private static final ImmutableList<IsdaCreditCurveNode> NODES = ImmutableList.of(
      SwapIsdaCreditCurveNode.of(
          QuoteId.of(StandardId.of("OG", "swap1Y")), DaysAdjustment.NONE, BusinessDayAdjustment.NONE, TENOR_1Y, ACT_360, P3M),
      SwapIsdaCreditCurveNode.of(
          QuoteId.of(StandardId.of("OG", "swap5Y")), DaysAdjustment.NONE, BusinessDayAdjustment.NONE, TENOR_5Y, ACT_360, P3M),
      SwapIsdaCreditCurveNode.of(
          QuoteId.of(StandardId.of("OG", "swap15Y")), DaysAdjustment.NONE, BusinessDayAdjustment.NONE, TENOR_15Y, ACT_360, P3M));

  @Test
  public void test_of() {
    IsdaCreditCurveDefinition test =
        IsdaCreditCurveDefinition.of(NAME, USD, CURVE_VALUATION_DATE, ACT_ACT_ISDA, NODES, true, false);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurveNodes()).isEqualTo(NODES);
    assertThat(test.getCurveValuationDate()).isEqualTo(CURVE_VALUATION_DATE);
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.isComputeJacobian()).isTrue();
    assertThat(test.isStoreNodeTrade()).isFalse();
    DoubleArray time = DoubleArray.of(1, 2, 3);
    DoubleArray rate = DoubleArray.of(0.01, 0.014, 0.02);
    InterpolatedNodalCurve expectedCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(NAME, ACT_ACT_ISDA),
        time,
        rate,
        CurveInterpolators.PRODUCT_LINEAR,
        CurveExtrapolators.FLAT,
        CurveExtrapolators.PRODUCT_LINEAR);
    assertThat(test.curve(time, rate)).isEqualTo(expectedCurve);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IsdaCreditCurveDefinition test1 =
        IsdaCreditCurveDefinition.of(NAME, USD, CURVE_VALUATION_DATE, ACT_ACT_ISDA, NODES, true, true);
    coverImmutableBean(test1);
    IsdaCreditCurveDefinition test2 = IsdaCreditCurveDefinition.of(
        CurveName.of("TestCurve1"), EUR, CURVE_VALUATION_DATE.plusDays(1), ACT_365F, NODES.subList(0, 2), false, false);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    IsdaCreditCurveDefinition test =
        IsdaCreditCurveDefinition.of(NAME, USD, CURVE_VALUATION_DATE, ACT_ACT_ISDA, NODES, true, true);
    assertSerialization(test);
  }

}
