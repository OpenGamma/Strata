/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenarios.curves;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.RateIndexCurveId;

@Test
public class RateCurveCurrencyFilterTest {

  public void matchIndexCurve() {
    RateCurveCurrencyFilter filter = RateCurveCurrencyFilter.of(Currency.USD);
    RateIndexCurveId id = RateIndexCurveId.of(IborIndices.USD_LIBOR_1M, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(filter.apply(id, curve)).isTrue();
  }

  public void noMatchIndexCurve() {
    RateCurveCurrencyFilter filter = RateCurveCurrencyFilter.of(Currency.GBP);
    RateIndexCurveId id = RateIndexCurveId.of(IborIndices.USD_LIBOR_1M, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(filter.apply(id, curve)).isFalse();
  }

  public void matchDiscountingCurve() {
    RateCurveCurrencyFilter filter = RateCurveCurrencyFilter.of(Currency.USD);
    DiscountCurveId id = DiscountCurveId.of(Currency.USD, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(filter.apply(id, curve)).isTrue();
  }

  public void noMatchDiscountingCurve() {
    RateCurveCurrencyFilter filter = RateCurveCurrencyFilter.of(Currency.GBP);
    DiscountCurveId id = DiscountCurveId.of(Currency.USD, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(filter.apply(id, curve)).isFalse();
  }
}
