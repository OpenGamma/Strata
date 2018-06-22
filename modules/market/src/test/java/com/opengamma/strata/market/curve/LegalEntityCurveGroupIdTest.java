/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.data.ObservableSource;

/**
 * Test {@link LegalEntityCurveGroupId}.
 */
@Test
public class LegalEntityCurveGroupIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final ObservableSource OBS_SOURCE2 = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  public void test_of_String() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1.toString());
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), LegalEntityCurveGroup.class);
    assertEquals(test.toString(), "LegalEntityCurveGroupId:Group1");
  }

  public void test_of_Type() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), LegalEntityCurveGroup.class);
    assertEquals(test.toString(), "LegalEntityCurveGroupId:Group1");
  }

  public void test_of_TypeSource() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1, OBS_SOURCE2);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getObservableSource(), OBS_SOURCE2);
    assertEquals(test.getMarketDataType(), LegalEntityCurveGroup.class);
    assertEquals(test.toString(), "LegalEntityCurveGroupId:Group1/Vendor");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1);
    coverImmutableBean(test);
    LegalEntityCurveGroupId test2 = LegalEntityCurveGroupId.of(GROUP2, OBS_SOURCE2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1);
    assertSerialization(test);
  }

}
