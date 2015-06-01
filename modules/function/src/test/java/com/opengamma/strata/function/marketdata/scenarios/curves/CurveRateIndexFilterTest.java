/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenarios.curves;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.RateIndexCurveId;

@Test
public class CurveRateIndexFilterTest {

  public void match() {
    CurveRateIndexFilter filter = CurveRateIndexFilter.of(IborIndices.USD_LIBOR_1M);
    RateIndexCurveId id = RateIndexCurveId.of(IborIndices.USD_LIBOR_1M, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(filter.apply(id, curve)).isTrue();
  }

  public void noMatch() {
    CurveRateIndexFilter filter = CurveRateIndexFilter.of(IborIndices.USD_LIBOR_1M);
    RateIndexCurveId id = RateIndexCurveId.of(IborIndices.USD_LIBOR_3M, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(filter.apply(id, curve)).isFalse();
  }
}
