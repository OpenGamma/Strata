/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;

/**
 * Tests {@link RawOptionData}.
 */
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
  private static final DoubleMatrix ERROR = DoubleMatrix.ofUnsafe(
      new double[][] {{1.0e-4, 1.0e-4, 1.0e-4, 1.0e-4},
          {1.0e-4, 1.0e-4, 1.0e-4, 1.0e-4},
          {1.0e-3, 1.0e-3, 1.0e-3, 1.0e-3}});

  //-------------------------------------------------------------------------
  @Test
  public void of() {
    RawOptionData test = sut();
    assertThat(test.getStrikes()).isEqualTo(MONEYNESS);
    assertThat(test.getStrikeType()).isEqualTo(ValueType.SIMPLE_MONEYNESS);
    assertThat(test.getData()).isEqualTo(DATA_FULL);
    assertThat(test.getDataType()).isEqualTo(ValueType.NORMAL_VOLATILITY);
  }

  @Test
  public void ofBlackVolatility() {
    double shift = 0.0075;
    RawOptionData test =
        RawOptionData.ofBlackVolatility(EXPIRIES, STRIKES, ValueType.STRIKE, DATA_SPARSE, shift);
    assertThat(test.getStrikes()).isEqualTo(STRIKES);
    assertThat(test.getStrikeType()).isEqualTo(ValueType.STRIKE);
    assertThat(test.getData()).isEqualTo(DATA_SPARSE);
    assertThat(test.getDataType()).isEqualTo(ValueType.BLACK_VOLATILITY);
    assertThat(test.getShift()).isEqualTo(OptionalDouble.of(shift));
    assertThat(test.getError().isPresent()).isFalse();
  }

  @Test
  public void available_smile_at_expiry() {
    double shift = 0.0075;
    RawOptionData test =
        RawOptionData.ofBlackVolatility(EXPIRIES, STRIKES, ValueType.STRIKE, DATA_SPARSE, shift);
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
      assertThat(smile.getFirst()).isEqualTo(strikesAvailable[i]);
    }
  }

  @Test
  public void of_error() {
    RawOptionData test = sut3();
    assertThat(test.getStrikes()).isEqualTo(MONEYNESS);
    assertThat(test.getStrikeType()).isEqualTo(ValueType.SIMPLE_MONEYNESS);
    assertThat(test.getData()).isEqualTo(DATA_FULL);
    assertThat(test.getDataType()).isEqualTo(ValueType.NORMAL_VOLATILITY);
    assertThat(test.getError().get()).isEqualTo(ERROR);
  }

  @Test
  public void ofBlackVolatility_error() {
    double shift = 0.0075;
    RawOptionData test =
        RawOptionData.ofBlackVolatility(EXPIRIES, STRIKES, ValueType.STRIKE, DATA_SPARSE, ERROR, shift);
    assertThat(test.getStrikes()).isEqualTo(STRIKES);
    assertThat(test.getStrikeType()).isEqualTo(ValueType.STRIKE);
    assertThat(test.getData()).isEqualTo(DATA_SPARSE);
    assertThat(test.getDataType()).isEqualTo(ValueType.BLACK_VOLATILITY);
    assertThat(test.getShift()).isEqualTo(OptionalDouble.of(shift));
    assertThat(test.getError().get()).isEqualTo(ERROR);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    RawOptionData test = sut();
    coverImmutableBean(test);
    RawOptionData test2 = sut2();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    RawOptionData test =
        RawOptionData.of(EXPIRIES, MONEYNESS, ValueType.SIMPLE_MONEYNESS, DATA_FULL, ValueType.BLACK_VOLATILITY);
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  static RawOptionData sut() {
    return RawOptionData.of(EXPIRIES, MONEYNESS, ValueType.SIMPLE_MONEYNESS, DATA_FULL, ValueType.NORMAL_VOLATILITY);
  }

  static RawOptionData sut2() {
    List<Period> expiries2 = new ArrayList<>();
    expiries2.add(Period.ofMonths(3));
    expiries2.add(Period.ofYears(1));
    expiries2.add(Period.ofYears(5));
    RawOptionData test2 =
        RawOptionData.of(expiries2, STRIKES, ValueType.STRIKE, DATA_SPARSE, ERROR, ValueType.BLACK_VOLATILITY);
    return test2;
  }

  static RawOptionData sut3() {
    return RawOptionData.of(EXPIRIES, MONEYNESS, ValueType.SIMPLE_MONEYNESS, DATA_FULL, ERROR, ValueType.NORMAL_VOLATILITY);
  }

}
