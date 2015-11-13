/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenario.curve;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.id.CurveId;
import com.opengamma.strata.market.id.DiscountCurveId;

/**
 * Test {@link CurveNameFilter}.
 */
@Test
public class CurveNameFilterTest {

  public void match() {
    CurveNameFilter test = CurveNameFilter.of(CurveName.of("curveName"));
    assertThat(test.getMarketDataIdType()).isEqualTo(CurveId.class);
    DiscountCurveId id = DiscountCurveId.of(Currency.GBP, CurveGroupName.of("curveGroupName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(test.matches(id, MarketDataBox.ofSingleValue(curve))).isTrue();
  }

  public void noMatch() {
    CurveNameFilter test = CurveNameFilter.of(CurveName.of("curveName"));
    DiscountCurveId id = DiscountCurveId.of(Currency.GBP, CurveGroupName.of("curveGroupName"));
    Curve curve = ConstantNodalCurve.of("notCurveName", 1);
    assertThat(test.matches(id, MarketDataBox.ofSingleValue(curve))).isFalse();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveNameFilter test = CurveNameFilter.of(CurveName.of("curveName1"));
    coverImmutableBean(test);
    CurveNameFilter test2 = CurveNameFilter.of(CurveName.of("curveName2"));
    coverBeanEquals(test, test2);
  }

}
