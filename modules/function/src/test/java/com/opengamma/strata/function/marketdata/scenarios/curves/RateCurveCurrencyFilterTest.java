/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenarios.curves;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.ConstantDoublesCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.market.id.DiscountingCurveId;
import com.opengamma.strata.market.id.RateIndexCurveId;

@Test
public class RateCurveCurrencyFilterTest {

  public void matchIndexCurve() {
    RateCurveCurrencyFilter filter = RateCurveCurrencyFilter.of(Currency.USD);
    RateIndexCurveId id = RateIndexCurveId.of(IborIndices.USD_LIBOR_1M, "curveName");
    YieldCurve curve = YieldCurve.from(ConstantDoublesCurve.from(1d, "curveName"));
    assertThat(filter.apply(id, curve)).isTrue();
  }

  public void noMatchIndexCurve() {
    RateCurveCurrencyFilter filter = RateCurveCurrencyFilter.of(Currency.GBP);
    RateIndexCurveId id = RateIndexCurveId.of(IborIndices.USD_LIBOR_1M, "curveName");
    YieldCurve curve = YieldCurve.from(ConstantDoublesCurve.from(1d, "curveName"));
    assertThat(filter.apply(id, curve)).isFalse();
  }

  public void matchDiscountingCurve() {
    RateCurveCurrencyFilter filter = RateCurveCurrencyFilter.of(Currency.USD);
    DiscountingCurveId id = DiscountingCurveId.of(Currency.USD, "curveName");
    YieldCurve curve = YieldCurve.from(ConstantDoublesCurve.from(1d, "curveName"));
    assertThat(filter.apply(id, curve)).isTrue();
  }

  public void noMatchDiscountingCurve() {
    RateCurveCurrencyFilter filter = RateCurveCurrencyFilter.of(Currency.GBP);
    DiscountingCurveId id = DiscountingCurveId.of(Currency.USD, "curveName");
    YieldCurve curve = YieldCurve.from(ConstantDoublesCurve.from(1d, "curveName"));
    assertThat(filter.apply(id, curve)).isFalse();
  }
}
