/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.market.GenericDoubleShifts;
import com.opengamma.strata.market.ShiftType;

/**
 * Test {@link GenericDoubleShifts}.
 */
@Test
public class GenericDoubleShiftsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final DoubleArray SHIFT_AMOUNT = DoubleArray.of(3.5d, 2d, -5d);
  private static final double SPREAD = 0.5;

  public void test_of() {
    GenericDoubleShifts test = GenericDoubleShifts.of(ShiftType.ABSOLUTE, SHIFT_AMOUNT);
    assertEquals(test.getScenarioCount(), 3);
    assertEquals(test.getShiftAmount(), SHIFT_AMOUNT);
    assertEquals(test.getShiftType(), ShiftType.ABSOLUTE);
    assertEquals(test.getSpread(), 0d);
  }

  public void test_of_spread() {
    GenericDoubleShifts test = GenericDoubleShifts.of(ShiftType.RELATIVE, SHIFT_AMOUNT, SPREAD);
    assertEquals(test.getScenarioCount(), 3);
    assertEquals(test.getShiftAmount(), SHIFT_AMOUNT);
    assertEquals(test.getShiftType(), ShiftType.RELATIVE);
    assertEquals(test.getSpread(), SPREAD);
  }

  public void test_applyTo() {
    double baseValue = 3d;
    MarketDataBox<Double> marketData = MarketDataBox.ofSingleValue(baseValue);
    GenericDoubleShifts testScaled = GenericDoubleShifts.of(ShiftType.SCALED, SHIFT_AMOUNT, SPREAD);
    MarketDataBox<Double> computedScaled = testScaled.applyTo(marketData, REF_DATA);
    MarketDataBox<Double> expectedScaled = MarketDataBox.ofScenarioValues(ImmutableList.of(
        (baseValue + SPREAD) * SHIFT_AMOUNT.get(0) - SPREAD,
        (baseValue + SPREAD) * SHIFT_AMOUNT.get(1) - SPREAD,
        (baseValue + SPREAD) * SHIFT_AMOUNT.get(2) - SPREAD));
    assertEquals(computedScaled, expectedScaled);
    GenericDoubleShifts testRelative = GenericDoubleShifts.of(ShiftType.RELATIVE, SHIFT_AMOUNT, SPREAD);
    MarketDataBox<Double> computedRelative = testRelative.applyTo(marketData, REF_DATA);
    MarketDataBox<Double> expectedRelative = MarketDataBox.ofScenarioValues(ImmutableList.of(
        (baseValue + SPREAD) * (1d + SHIFT_AMOUNT.get(0)) - SPREAD,
        (baseValue + SPREAD) * (1d + SHIFT_AMOUNT.get(1)) - SPREAD,
        (baseValue + SPREAD) * (1d + SHIFT_AMOUNT.get(2)) - SPREAD));
    assertEquals(computedRelative, expectedRelative);
    GenericDoubleShifts testAbsolute = GenericDoubleShifts.of(ShiftType.ABSOLUTE, SHIFT_AMOUNT);
    MarketDataBox<Double> computedAbsolute = testAbsolute.applyTo(marketData, REF_DATA);
    MarketDataBox<Double> expectedAbsolute = MarketDataBox.ofScenarioValues(ImmutableList.of(
        baseValue + SHIFT_AMOUNT.get(0),
        baseValue + SHIFT_AMOUNT.get(1),
        baseValue + SHIFT_AMOUNT.get(2)));
    assertEquals(computedAbsolute, expectedAbsolute);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    GenericDoubleShifts test1 = GenericDoubleShifts.of(ShiftType.ABSOLUTE, SHIFT_AMOUNT);
    coverImmutableBean(test1);
    GenericDoubleShifts test2 = GenericDoubleShifts.of(ShiftType.SCALED, DoubleArray.of(3.5d, 2d), SPREAD);
    coverBeanEquals(test1, test2);
  }

}
