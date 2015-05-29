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
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.id.DiscountCurveId;

@Test
public class CurveNameFilterTest {

  public void match() {
    CurveNameFilter filter = CurveNameFilter.of(CurveName.of("curveName"));
    DiscountCurveId id = DiscountCurveId.of(Currency.GBP, CurveGroupName.of("curveGroupName"));
    YieldCurve curve = YieldCurve.from(ConstantDoublesCurve.from(1d, "curveName"));
    assertThat(filter.apply(id, curve)).isTrue();
  }

  public void noMatch() {
    CurveNameFilter filter = CurveNameFilter.of(CurveName.of("curveName"));
    DiscountCurveId id = DiscountCurveId.of(Currency.GBP, CurveGroupName.of("curveGroupName"));
    YieldCurve curve = YieldCurve.from(ConstantDoublesCurve.from(1d, "notCurveName"));
    assertThat(filter.apply(id, curve)).isFalse();
  }
}
