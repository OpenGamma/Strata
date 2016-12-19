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
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
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
  private static final DoubleArray VECTOR_COMBINED = VECTOR1.concat(VECTOR2);
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final MarketDataName<?> NAME_COMBINED = CurveName.of("NAME-COMBINED");
  private static final List<ParameterMetadata> METADATA1 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA2 = ParameterMetadata.listOfEmpty(5);
  private static final ImmutableList<ParameterMetadata> METADATA_COMBINED =
      ImmutableList.<ParameterMetadata>builder().addAll(METADATA1).addAll(METADATA2).build();
  private static final List<ParameterMetadata> METADATA_BAD = ParameterMetadata.listOfEmpty(1);
  private static final List<ParameterSize> PARAM_SPLIT = ImmutableList.of(ParameterSize.of(NAME1, 4), ParameterSize.of(NAME2, 5));

  //-------------------------------------------------------------------------
  public void test_of_metadata() {
    UnitParameterSensitivity test = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    assertEquals(test.getMarketDataName(), NAME1);
    assertEquals(test.getParameterCount(), VECTOR1.size());
    assertEquals(test.getParameterMetadata(), METADATA1);
    assertEquals(test.getParameterMetadata(0), METADATA1.get(0));
    assertEquals(test.getSensitivity(), VECTOR1);
    assertEquals(test.getParameterSplit(), Optional.empty());
  }

  public void test_of_metadata_badMetadata() {
    assertThrowsIllegalArg(() -> UnitParameterSensitivity.of(NAME1, METADATA_BAD, VECTOR1));
  }

  public void test_of_metadataParamSplit() {
    UnitParameterSensitivity test = UnitParameterSensitivity.of(NAME_COMBINED, METADATA_COMBINED, VECTOR_COMBINED, PARAM_SPLIT);
    assertEquals(test.getMarketDataName(), NAME_COMBINED);
    assertEquals(test.getParameterCount(), VECTOR_COMBINED.size());
    assertEquals(test.getParameterMetadata(), METADATA_COMBINED);
    assertEquals(test.getParameterMetadata(0), METADATA_COMBINED.get(0));
    assertEquals(test.getSensitivity(), VECTOR_COMBINED);
    assertEquals(test.getParameterSplit(), Optional.of(PARAM_SPLIT));
  }

  public void test_of_metadataParamSplit_badSplit() {
    assertThrowsIllegalArg(() -> UnitParameterSensitivity.of(NAME_COMBINED, METADATA1, VECTOR1, PARAM_SPLIT));
  }

  public void test_combine() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity base2 = UnitParameterSensitivity.of(NAME2, METADATA2, VECTOR2);
    UnitParameterSensitivity test = UnitParameterSensitivity.combine(NAME_COMBINED, base1, base2);
    assertEquals(test.getMarketDataName(), NAME_COMBINED);
    assertEquals(test.getParameterCount(), VECTOR_COMBINED.size());
    assertEquals(test.getParameterMetadata(), METADATA_COMBINED);
    assertEquals(test.getParameterMetadata(0), METADATA_COMBINED.get(0));
    assertEquals(test.getSensitivity(), VECTOR_COMBINED);
    assertEquals(test.getParameterSplit(), Optional.of(PARAM_SPLIT));
  }

  public void test_combine_arraySize0() {
    assertThrowsIllegalArg(() -> UnitParameterSensitivity.combine(NAME_COMBINED));
  }

  public void test_combine_arraySize1() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    assertThrowsIllegalArg(() -> UnitParameterSensitivity.combine(NAME_COMBINED, base));
  }

  public void test_combine_duplicateNames() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity base2 = UnitParameterSensitivity.of(NAME1, METADATA2, VECTOR2);
    assertThrowsIllegalArg(() -> UnitParameterSensitivity.combine(NAME_COMBINED, base1, base2));
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
  public void test_plus_array() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity test = base.plus(VECTOR1);
    assertEquals(test, base.multipliedBy(2));
  }

  public void test_plus_array_wrongSize() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    assertThrowsIllegalArg(() -> base.plus(VECTOR2));
  }

  //-------------------------------------------------------------------------
  public void test_plus_sensitivity() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity test = base1.plus(base1);
    assertEquals(test, base1.multipliedBy(2));
  }

  public void test_plus_sensitivity_wrongName() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity base2 = UnitParameterSensitivity.of(NAME2, METADATA1, VECTOR1);
    assertThrowsIllegalArg(() -> base1.plus(base2));
  }

  //-------------------------------------------------------------------------
  public void test_split1() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    ImmutableList<UnitParameterSensitivity> test = base.split();
    assertEquals(test.size(), 1);
    assertEquals(test.get(0), base);
  }

  public void test_split2() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity base2 = UnitParameterSensitivity.of(NAME2, METADATA2, VECTOR2);
    UnitParameterSensitivity combined = UnitParameterSensitivity.combine(NAME_COMBINED, base1, base2);
    ImmutableList<UnitParameterSensitivity> test = combined.split();
    assertEquals(test.size(), 2);
    assertEquals(test.get(0), base1);
    assertEquals(test.get(1), base2);
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
