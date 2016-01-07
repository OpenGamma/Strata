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

import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.IborIndexCurveId;
import com.opengamma.strata.market.id.IndexCurveId;

/**
 * Test {@link IndexCurveFilter}.
 */
@Test
public class IndexCurveFilterTest {

  public void match() {
    IndexCurveFilter test = IndexCurveFilter.of(IborIndices.USD_LIBOR_1M);
    assertThat(test.getMarketDataIdType()).isEqualTo(IndexCurveId.class);
    IndexCurveId id = IborIndexCurveId.of(IborIndices.USD_LIBOR_1M, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(test.matches(id, MarketDataBox.ofSingleValue(curve))).isTrue();
  }

  public void noMatch() {
    IndexCurveFilter test = IndexCurveFilter.of(IborIndices.USD_LIBOR_1M);
    IndexCurveId id = IborIndexCurveId.of(IborIndices.USD_LIBOR_3M, CurveGroupName.of("curveName"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(test.matches(id, MarketDataBox.ofSingleValue(curve))).isFalse();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IndexCurveFilter test = IndexCurveFilter.of(IborIndices.USD_LIBOR_1M);
    coverImmutableBean(test);
    IndexCurveFilter test2 = IndexCurveFilter.of(IborIndices.USD_LIBOR_3M);
    coverBeanEquals(test, test2);
  }

}
