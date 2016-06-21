/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.curve;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CurveNameFilter}.
 */
@Test
public class CurveNameFilterTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void match() {
    CurveNameFilter test = CurveNameFilter.of(CurveName.of("name"));
    assertThat(test.getMarketDataIdType()).isEqualTo(CurveId.class);
    CurveId id = CurveId.of("group", "name");
    Curve curve = ConstantCurve.of("name", 1);
    assertThat(test.matches(id, MarketDataBox.ofSingleValue(curve), REF_DATA)).isTrue();
  }

  public void noMatch() {
    CurveNameFilter test = CurveNameFilter.of(CurveName.of("name"));
    CurveId id = CurveId.of("group", "name");
    Curve curve = ConstantCurve.of("notCurveName", 1);
    assertThat(test.matches(id, MarketDataBox.ofSingleValue(curve), REF_DATA)).isFalse();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveNameFilter test = CurveNameFilter.of(CurveName.of("curveName1"));
    coverImmutableBean(test);
    CurveNameFilter test2 = CurveNameFilter.of(CurveName.of("curveName2"));
    coverBeanEquals(test, test2);
  }

}
