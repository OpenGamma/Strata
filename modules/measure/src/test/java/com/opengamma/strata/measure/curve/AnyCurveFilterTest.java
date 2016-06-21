/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.curve;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;

/**
 * Test {@link AnyCurveFilter}.
 */
@Test
public class AnyCurveFilterTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void match() {
    AnyCurveFilter test = AnyCurveFilter.INSTANCE;
    assertThat(test.getMarketDataIdType()).isEqualTo(CurveId.class);
    CurveId id = CurveId.of("group", "name");
    Curve curve = ConstantCurve.of("name", 1);
    assertThat(test.matches(id, MarketDataBox.ofSingleValue(curve), REF_DATA)).isTrue();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    AnyCurveFilter test = AnyCurveFilter.INSTANCE;
    coverImmutableBean(test);
  }

}
