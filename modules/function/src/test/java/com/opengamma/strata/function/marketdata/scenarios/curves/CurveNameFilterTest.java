/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenarios.curves;


import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.ConstantDoublesCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.market.id.DiscountingCurveId;

@Test
public class CurveNameFilterTest {

  public void match() {
    CurveNameFilter filter = CurveNameFilter.of("curveName");
    DiscountingCurveId id = DiscountingCurveId.of(Currency.GBP, "curveName");
    YieldCurve curve = YieldCurve.from(ConstantDoublesCurve.from(1d, "curveName"));
    assertThat(filter.apply(id, curve)).isTrue();
  }

  public void noMatch() {
    CurveNameFilter filter = CurveNameFilter.of("curveName");
    DiscountingCurveId id = DiscountingCurveId.of(Currency.GBP, "notCurveName");
    YieldCurve curve = YieldCurve.from(ConstantDoublesCurve.from(1d, "notCurveName"));
    assertThat(filter.apply(id, curve)).isFalse();
  }
}
