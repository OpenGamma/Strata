/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link CurveInfoType}.
 */
@Test
public class CurveInfoTypeTest {

  public void test_DAY_COUNT() {
    CurveInfoType<DayCount> test = CurveInfoType.DAY_COUNT;
    assertEquals(test.toString(), "DayCount");
  }

  public void test_JACOBIAN() {
    CurveInfoType<JacobianCalibrationMatrix> test = CurveInfoType.JACOBIAN;
    assertEquals(test.toString(), "Jacobian");
  }

  public void test_COMPOUNDING_PER_YEAR() {
    CurveInfoType<Integer> test = CurveInfoType.COMPOUNDING_PER_YEAR;
    assertEquals(test.toString(), "CompoundingPerYear");
  }

  public void test_PV_SENSITIVITY_TO_MARKET_QUOTE() {
    CurveInfoType<DoubleArray> test = CurveInfoType.PV_SENSITIVITY_TO_MARKET_QUOTE;
    assertEquals(test.toString(), "PVSensitivityToMarketQuote");
  }

  public void test_CDS_INDEX_FACTOR() {
    CurveInfoType<Double> test = CurveInfoType.CDS_INDEX_FACTOR;
    assertEquals(test.toString(), "CdsIndexFactor");
  }

  public void coverage() {
    CurveInfoType<String> test = CurveInfoType.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
