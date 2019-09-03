/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link CurveInfoType}.
 */
public class CurveInfoTypeTest {

  @Test
  public void test_DAY_COUNT() {
    CurveInfoType<DayCount> test = CurveInfoType.DAY_COUNT;
    assertThat(test.toString()).isEqualTo("DayCount");
  }

  @Test
  public void test_JACOBIAN() {
    CurveInfoType<JacobianCalibrationMatrix> test = CurveInfoType.JACOBIAN;
    assertThat(test.toString()).isEqualTo("Jacobian");
  }

  @Test
  public void test_COMPOUNDING_PER_YEAR() {
    CurveInfoType<Integer> test = CurveInfoType.COMPOUNDING_PER_YEAR;
    assertThat(test.toString()).isEqualTo("CompoundingPerYear");
  }

  @Test
  public void test_PV_SENSITIVITY_TO_MARKET_QUOTE() {
    CurveInfoType<DoubleArray> test = CurveInfoType.PV_SENSITIVITY_TO_MARKET_QUOTE;
    assertThat(test.toString()).isEqualTo("PVSensitivityToMarketQuote");
  }

  @Test
  public void test_CDS_INDEX_FACTOR() {
    CurveInfoType<Double> test = CurveInfoType.CDS_INDEX_FACTOR;
    assertThat(test.toString()).isEqualTo("CdsIndexFactor");
  }

  @Test
  public void coverage() {
    CurveInfoType<String> test = CurveInfoType.of("Foo");
    assertThat(test.toString()).isEqualTo("Foo");
  }

}
