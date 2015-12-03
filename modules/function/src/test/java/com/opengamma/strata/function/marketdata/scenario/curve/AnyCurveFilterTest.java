/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenario.curve;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.CurveId;
import com.opengamma.strata.market.id.DiscountCurveId;

/**
 * Test {@link AnyCurveFilter}.
 */
@Test
public class AnyCurveFilterTest {

  public void match() {
    AnyCurveFilter test = AnyCurveFilter.INSTANCE;
    assertThat(test.getMarketDataIdType()).isEqualTo(CurveId.class);
    CurveId id = DiscountCurveId.of(Currency.AUD, CurveGroupName.of("group"));
    Curve curve = ConstantNodalCurve.of("curveName", 1);
    assertThat(test.matches(id, MarketDataBox.ofSingleValue(curve))).isTrue();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    AnyCurveFilter test = AnyCurveFilter.INSTANCE;
    coverImmutableBean(test);
  }

}
