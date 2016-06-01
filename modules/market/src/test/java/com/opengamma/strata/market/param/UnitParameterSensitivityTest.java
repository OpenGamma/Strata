/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link UnitParameterSensitivity}.
 */
@Test
public class UnitParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleArray VECTOR1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR1_FACTOR =
      DoubleArray.of(100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1);
  private static final DoubleArray VECTOR2 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final List<ParameterMetadata> METADATA1 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA2 = ParameterMetadata.listOfEmpty(5);
  private static final List<ParameterMetadata> METADATA_BAD = ParameterMetadata.listOfEmpty(1);

  //-------------------------------------------------------------------------
  public void test_of_metadata() {
    UnitParameterSensitivity test = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    assertEquals(test.getMarketDataName(), NAME1);
    assertEquals(test.getParameterCount(), VECTOR1.size());
    assertEquals(test.getParameterMetadata(), METADATA1);
    assertEquals(test.getParameterMetadata(0), METADATA1.get(0));
    assertEquals(test.getSensitivity(), VECTOR1);
  }

  public void test_of_metadata_badMetadata() {
    assertThrowsIllegalArg(() -> UnitParameterSensitivity.of(NAME1, METADATA_BAD, VECTOR1));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy_currency() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    CurrencyParameterSensitivity test = base.multipliedBy(USD, FACTOR1);
    assertEquals(test, CurrencyParameterSensitivity.of(NAME1, METADATA1, USD, VECTOR1_FACTOR));
  }

  public void test_multipliedBy() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertEquals(test, UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1_FACTOR));
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity test = base.withSensitivity(VECTOR1_FACTOR);
    assertEquals(test, UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1_FACTOR));
    assertThrowsIllegalArg(() -> base.withSensitivity(DoubleArray.of(1d)));
  }

  //-------------------------------------------------------------------------
  public void test_total() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    double test = base.total();
    assertEquals(test, VECTOR1.get(0) + VECTOR1.get(1) + VECTOR1.get(2) + VECTOR1.get(3));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    UnitParameterSensitivity test = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    coverImmutableBean(test);
    UnitParameterSensitivity test2 = UnitParameterSensitivity.of(NAME2, METADATA2, VECTOR2);
    coverBeanEquals(test, test2);
  }

}
