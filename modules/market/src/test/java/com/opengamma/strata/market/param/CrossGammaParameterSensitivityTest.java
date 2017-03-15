/*
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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CrossGammaParameterSensitivity}.
 */
@Test
public class CrossGammaParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleMatrix MATRIX_USD1 = DoubleMatrix.of(2, 2, 100, 200, 300, 123);
  private static final DoubleMatrix MATRIX_USD_FACTOR =
      DoubleMatrix.of(2, 2, 100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1);
  private static final DoubleMatrix MATRIX_EUR1 = DoubleMatrix.of(2, 2, 1000, 250, 321, 123);
  private static final DoubleMatrix MATRIX_EUR1_IN_USD =
      DoubleMatrix.of(2, 2, 1000 * 1.5, 250 * 1.5, 321 * 1.5, 123 * 1.5);
  private static final DoubleMatrix MATRIX_USD_EUR = DoubleMatrix.of(2, 4, 100, 200, 1000, 250, 300, 123, 321, 123);
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.5d);
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final List<ParameterMetadata> METADATA_USD1 = ParameterMetadata.listOfEmpty(2);
  private static final List<ParameterMetadata> METADATA_EUR1 = ParameterMetadata.listOfEmpty(2);
  private static final List<ParameterMetadata> METADATA_BAD = ParameterMetadata.listOfEmpty(1);

  //-------------------------------------------------------------------------
  public void test_of_metadata() {
    CrossGammaParameterSensitivity test = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    assertEquals(test.getMarketDataName(), NAME1);
    assertEquals(test.getParameterCount(), 2);
    assertEquals(test.getParameterMetadata(), METADATA_USD1);
    assertEquals(test.getParameterMetadata(0), METADATA_USD1.get(0));
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getSensitivity(), MATRIX_USD1);
    assertEquals(test.getOrder(), ImmutableList.of(Pair.of(NAME1, METADATA_USD1)));
  }

  public void test_of_eurUsd() {
    CrossGammaParameterSensitivity test = CrossGammaParameterSensitivity.of(
        NAME1, METADATA_USD1, ImmutableList.of(Pair.of(NAME1, METADATA_USD1), Pair.of(NAME2, METADATA_EUR1)), USD,
        MATRIX_USD_EUR);
    assertEquals(test.getMarketDataName(), NAME1);
    assertEquals(test.getParameterCount(), 2);
    assertEquals(test.getParameterMetadata(), METADATA_USD1);
    assertEquals(test.getParameterMetadata(0), METADATA_USD1.get(0));
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getSensitivity(), MATRIX_USD_EUR);
    assertEquals(test.getOrder(), ImmutableList.of(Pair.of(NAME1, METADATA_USD1), Pair.of(NAME2, METADATA_EUR1)));
  }

  public void test_of_metadata_badMetadata() {
    assertThrowsIllegalArg(() -> CrossGammaParameterSensitivity.of(NAME1, METADATA_BAD, USD, MATRIX_USD1));
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_EUR1, EUR, MATRIX_EUR1);
    CrossGammaParameterSensitivity test = base.convertedTo(USD, FX_RATE);
    assertEquals(base.convertedTo(EUR, FX_RATE), base);
    assertEquals(test, CrossGammaParameterSensitivity.of(NAME1, METADATA_EUR1, USD, MATRIX_EUR1_IN_USD));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    CrossGammaParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertEquals(test, CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD_FACTOR));
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    CrossGammaParameterSensitivity test = base.withSensitivity(MATRIX_USD_FACTOR);
    assertEquals(test, CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD_FACTOR));
    assertThrowsIllegalArg(() -> base.withSensitivity(DoubleMatrix.of(1, 1, 1d)));
  }

  //-------------------------------------------------------------------------
  public void test_total() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    CurrencyAmount test = base.total();
    assertEquals(test.getCurrency(), USD);
    double expected = MATRIX_USD1.get(0, 0) + MATRIX_USD1.get(0, 1) + MATRIX_USD1.get(1, 0) + MATRIX_USD1.get(1, 1);
    assertEquals(test.getAmount(), expected);
  }

  public void test_diagonal() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    CurrencyParameterSensitivity test = base.diagonal();
    DoubleArray value = DoubleArray.of(MATRIX_USD1.get(0, 0), MATRIX_USD1.get(1, 1));
    assertEquals(test, CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, value));
  }

  public void test_diagonal_eurUsd() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(
        NAME1, METADATA_USD1, ImmutableList.of(Pair.of(NAME1, METADATA_USD1), Pair.of(NAME2, METADATA_EUR1)), USD,
        MATRIX_USD_EUR);
    CurrencyParameterSensitivity test = base.diagonal();
    DoubleArray value = DoubleArray.of(MATRIX_USD1.get(0, 0), MATRIX_USD1.get(1, 1));
    assertEquals(test, CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, value));
  }

  public void test_getSensitivity_eurUsd() {
    CrossGammaParameterSensitivity test = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1,
        ImmutableList.of(Pair.of(NAME1, METADATA_USD1), Pair.of(NAME2, METADATA_EUR1)), USD, MATRIX_USD_EUR);
    CrossGammaParameterSensitivity expected1 = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    assertEquals(test.getSensitivity(NAME1), expected1);
    CrossGammaParameterSensitivity expected2 =
        CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, NAME2, METADATA_EUR1, USD, MATRIX_EUR1);
    assertEquals(test.getSensitivity(NAME2), expected2);
    assertThrowsIllegalArg(() -> test.getSensitivity(CurveName.of("NAME-3")));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CrossGammaParameterSensitivity test = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    coverImmutableBean(test);
    CrossGammaParameterSensitivity test2 = CrossGammaParameterSensitivity.of(NAME2, METADATA_EUR1, EUR, MATRIX_EUR1);
    coverBeanEquals(test, test2);
  }

}
