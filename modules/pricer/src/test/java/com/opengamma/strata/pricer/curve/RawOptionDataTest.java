/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;

/**
 * Tests {@link RawOptionData}.
 */
@Test
public class RawOptionDataTest {

  private static final DoubleArray MONEYNESS = DoubleArray.of(-0.010, 0.00, 0.0100, 0.0200);
  private static final DoubleArray STRIKES = DoubleArray.of(-0.0050, 0.0050, 0.0150, 0.0250);
  private static final List<Period> EXPIRIES = new ArrayList<>();

  static {
    EXPIRIES.add(Period.ofMonths(1));
    EXPIRIES.add(Period.ofMonths(3));
    EXPIRIES.add(Period.ofYears(1));
  }

  private static final DoubleMatrix DATA_FULL = DoubleMatrix.ofUnsafe(
      new double[][] {{0.08, 0.09, 0.10, 0.11},
          {0.09, 0.10, 0.11, 0.12},
          {0.10, 0.11, 0.12, 0.13}});
  private static final DoubleMatrix DATA_SPARSE = DoubleMatrix.ofUnsafe(
      new double[][] {{Double.NaN, Double.NaN, Double.NaN, Double.NaN},
          {Double.NaN, 0.10, 0.11, 0.12},
          {0.10, 0.11, 0.12, 0.13}});

  public void of() {
    RawOptionData test =
        RawOptionData.of(MONEYNESS, ValueType.SIMPLE_MONEYNESS, EXPIRIES, DATA_FULL, ValueType.NORMAL_VOLATILITY);
    assertEquals(test.getStrikes(), MONEYNESS);
    assertEquals(test.getStrikeType(), ValueType.SIMPLE_MONEYNESS);
    assertEquals(test.getData(), DATA_FULL);
    assertEquals(test.getDataType(), ValueType.NORMAL_VOLATILITY);
  }

  public void of2() {
    double shift = 0.0075;
    RawOptionData test =
        RawOptionData.of(STRIKES, ValueType.STRIKE, EXPIRIES, DATA_SPARSE, shift);
    assertEquals(test.getStrikes(), STRIKES);
    assertEquals(test.getStrikeType(), ValueType.STRIKE);
    assertEquals(test.getData(), DATA_SPARSE);
    assertEquals(test.getDataType(), ValueType.BLACK_VOLATILITY);
    assertEquals(test.getShift(), OptionalDouble.of(shift));
  }

  public void available_smile_at_expiry() {
    double shift = 0.0075;
    RawOptionData test =
        RawOptionData.of(STRIKES, ValueType.STRIKE, EXPIRIES, DATA_SPARSE, shift);
    DoubleArray[] strikesAvailable = new DoubleArray[3];
    strikesAvailable[0] = DoubleArray.EMPTY;
    strikesAvailable[1] = DoubleArray.of(0.0050, 0.0150, 0.0250);
    strikesAvailable[2] = DoubleArray.of(-0.0050, 0.0050, 0.0150, 0.0250);
    DoubleArray[] volAvailable = new DoubleArray[3];
    volAvailable[0] = DoubleArray.EMPTY;
    volAvailable[1] = DoubleArray.of(0.10, 0.11, 0.12);
    volAvailable[2] = DoubleArray.of(0.10, 0.11, 0.12, 0.13);
    for (int i = 0; i < DATA_SPARSE.rowCount(); i++) {
      Pair<DoubleArray, DoubleArray> smile = test.availableSmileAtExpiry(EXPIRIES.get(i));
      assertEquals(smile.getFirst(), strikesAvailable[i]);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RawOptionData test =
        RawOptionData.of(MONEYNESS, ValueType.SIMPLE_MONEYNESS, EXPIRIES, DATA_FULL, ValueType.NORMAL_VOLATILITY);
    coverImmutableBean(test);
    List<Period> expiries2 = new ArrayList<>();
    expiries2.add(Period.ofMonths(3));
    expiries2.add(Period.ofYears(1));
    expiries2.add(Period.ofYears(5));
    RawOptionData test2 =
        RawOptionData.of(STRIKES, ValueType.STRIKE, expiries2, DATA_SPARSE, ValueType.BLACK_VOLATILITY);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RawOptionData test =
        RawOptionData.of(MONEYNESS, ValueType.SIMPLE_MONEYNESS, EXPIRIES, DATA_FULL, ValueType.BLACK_VOLATILITY);
    assertSerialization(test);
  }

}
