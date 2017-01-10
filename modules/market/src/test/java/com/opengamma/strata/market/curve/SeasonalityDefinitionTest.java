/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ShiftType;

/**
 * Test {@link SeasonalityDefinition}.
 */
@Test
public class SeasonalityDefinitionTest {

  private static final DoubleArray SEASONALITY_ADDITIVE = DoubleArray.of(
      1.0, 1.5, 1.0, -0.5,
      -0.5, -1.0, -1.5, 0.0,
      0.5, 1.0, 1.0, -2.5);
  private static final ShiftType ADDITIVE = ShiftType.ABSOLUTE;

  public void test_builder1() {
    SeasonalityDefinition test = SeasonalityDefinition.of(SEASONALITY_ADDITIVE, ADDITIVE);
    assertEquals(test.getSeasonalityMonthOnMonth(), SEASONALITY_ADDITIVE);
    assertEquals(test.getAdjustmentType(), ADDITIVE);
  }

  public void test_of() {
    SeasonalityDefinition test = SeasonalityDefinition.of(SEASONALITY_ADDITIVE, ADDITIVE);
    assertEquals(test.getSeasonalityMonthOnMonth(), SEASONALITY_ADDITIVE);
    assertEquals(test.getAdjustmentType(), ADDITIVE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SeasonalityDefinition test = SeasonalityDefinition.of(SEASONALITY_ADDITIVE, ADDITIVE);
    coverImmutableBean(test);
    DoubleArray seasonalityMultiplicative = DoubleArray.of(
        1.0, 1.0, 1.1d, 1.0,
        1.0, 1.0, 1.0d / 1.1d, 1.0,
        1.0, 1.0, 1.0, 1.0);
    SeasonalityDefinition test2 = SeasonalityDefinition.of(seasonalityMultiplicative, ShiftType.SCALED);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SeasonalityDefinition test = SeasonalityDefinition.of(SEASONALITY_ADDITIVE, ADDITIVE);
    assertSerialization(test);
  }

}
