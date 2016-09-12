/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CurrencyParameterSensitivity}.
 */
@Test
public class CurrencyParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleArray VECTOR_USD1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR_USD_FACTOR =
      DoubleArray.of(100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1);
  private static final DoubleArray VECTOR_EUR1 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final DoubleArray VECTOR_EUR1_IN_USD =
      DoubleArray.of(1000 * 1.5, 250 * 1.5, 321 * 1.5, 123 * 1.5, 321 * 1.5);
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.5d);
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final List<ParameterMetadata> METADATA_USD1 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA_EUR1 = ParameterMetadata.listOfEmpty(5);
  private static final List<ParameterMetadata> METADATA_BAD = ParameterMetadata.listOfEmpty(1);

  //-------------------------------------------------------------------------
  public void test_of_metadata() {
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    assertEquals(test.getMarketDataName(), NAME1);
    assertEquals(test.getParameterCount(), VECTOR_USD1.size());
    assertEquals(test.getParameterMetadata(), METADATA_USD1);
    assertEquals(test.getParameterMetadata(0), METADATA_USD1.get(0));
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getSensitivity(), VECTOR_USD1);
  }

  public void test_of_metadata_badMetadata() {
    assertThrowsIllegalArg(() -> CurrencyParameterSensitivity.of(NAME1, METADATA_BAD, USD, VECTOR_USD1));
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_EUR1, EUR, VECTOR_EUR1);
    CurrencyParameterSensitivity test = base.convertedTo(USD, FX_RATE);
    assertEquals(test, CurrencyParameterSensitivity.of(NAME1, METADATA_EUR1, USD, VECTOR_EUR1_IN_USD));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertEquals(test, CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD_FACTOR));
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity test = base.withSensitivity(VECTOR_USD_FACTOR);
    assertEquals(test, CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD_FACTOR));
    assertThrowsIllegalArg(() -> base.withSensitivity(DoubleArray.of(1d)));
  }

  //-------------------------------------------------------------------------
  public void test_total() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyAmount test = base.total();
    assertEquals(test.getCurrency(), USD);
    double expected = VECTOR_USD1.get(0) + VECTOR_USD1.get(1) + VECTOR_USD1.get(2) + VECTOR_USD1.get(3);
    assertEquals(test.getAmount(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_toUnitParameterSensitivity() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    UnitParameterSensitivity test = base.toUnitParameterSensitivity();
    assertEquals(test, UnitParameterSensitivity.of(NAME1, METADATA_USD1, VECTOR_USD1));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    coverImmutableBean(test);
    CurrencyParameterSensitivity test2 = CurrencyParameterSensitivity.of(NAME2, METADATA_EUR1, EUR, VECTOR_EUR1);
    coverBeanEquals(test, test2);
  }

}
