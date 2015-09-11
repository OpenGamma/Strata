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
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.RateCurveId;
import com.opengamma.strata.market.id.RateIndexCurveId;

/**
 * Test {@link RateCurveCurrencyFilter}.
 */
@Test
public class RateCurveCurrencyFilterTest {

  public void matchIndexCurve() {
    RateCurveCurrencyFilter test = RateCurveCurrencyFilter.of(Currency.USD);
    assertThat(test.getMarketDataIdType()).isEqualTo(RateCurveId.class);
    RateCurveId id = RateIndexCurveId.of(IborIndices.USD_LIBOR_1M, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(test.matches(id, curve)).isTrue();
  }

  public void noMatchIndexCurve() {
    RateCurveCurrencyFilter test = RateCurveCurrencyFilter.of(Currency.GBP);
    RateCurveId id = RateIndexCurveId.of(IborIndices.USD_LIBOR_1M, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(test.matches(id, curve)).isFalse();
  }

  public void matchDiscountingCurve() {
    RateCurveCurrencyFilter test = RateCurveCurrencyFilter.of(Currency.USD);
    RateCurveId id = DiscountCurveId.of(Currency.USD, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(test.matches(id, curve)).isTrue();
  }

  public void noMatchDiscountingCurve() {
    RateCurveCurrencyFilter test = RateCurveCurrencyFilter.of(Currency.GBP);
    RateCurveId id = DiscountCurveId.of(Currency.USD, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(test.matches(id, curve)).isFalse();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RateCurveCurrencyFilter test = RateCurveCurrencyFilter.of(Currency.GBP);
    coverImmutableBean(test);
    RateCurveCurrencyFilter test2 = RateCurveCurrencyFilter.of(Currency.USD);
    coverBeanEquals(test, test2);
  }

}
