/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link UnitParameterSensitivity}.
 */
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
  @Test
  public void test_of_metadata() {
    UnitParameterSensitivity test = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    assertThat(test.getMarketDataName()).isEqualTo(NAME1);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR1.size());
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA1);
    assertThat(test.getParameterMetadata(0)).isEqualTo(METADATA1.get(0));
    assertThat(test.getSensitivity()).isEqualTo(VECTOR1);
    assertThat(test.getParameterSplit()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_metadata_badMetadata() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UnitParameterSensitivity.of(NAME1, METADATA_BAD, VECTOR1));
  }

  @Test
  public void test_of_metadataParamSplit() {
    UnitParameterSensitivity test = UnitParameterSensitivity.of(NAME_COMBINED, METADATA_COMBINED, VECTOR_COMBINED, PARAM_SPLIT);
    assertThat(test.getMarketDataName()).isEqualTo(NAME_COMBINED);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_COMBINED.size());
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA_COMBINED);
    assertThat(test.getParameterMetadata(0)).isEqualTo(METADATA_COMBINED.get(0));
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_COMBINED);
    assertThat(test.getParameterSplit()).isEqualTo(Optional.of(PARAM_SPLIT));
  }

  @Test
  public void test_of_metadataParamSplit_badSplit() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UnitParameterSensitivity.of(NAME_COMBINED, METADATA1, VECTOR1, PARAM_SPLIT));
  }

  @Test
  public void test_combine() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity base2 = UnitParameterSensitivity.of(NAME2, METADATA2, VECTOR2);
    UnitParameterSensitivity test = UnitParameterSensitivity.combine(NAME_COMBINED, base1, base2);
    assertThat(test.getMarketDataName()).isEqualTo(NAME_COMBINED);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_COMBINED.size());
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA_COMBINED);
    assertThat(test.getParameterMetadata(0)).isEqualTo(METADATA_COMBINED.get(0));
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_COMBINED);
    assertThat(test.getParameterSplit()).isEqualTo(Optional.of(PARAM_SPLIT));
  }

  @Test
  public void test_combine_arraySize0() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UnitParameterSensitivity.combine(NAME_COMBINED));
  }

  @Test
  public void test_combine_arraySize1() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UnitParameterSensitivity.combine(NAME_COMBINED, base));
  }

  @Test
  public void test_combine_duplicateNames() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity base2 = UnitParameterSensitivity.of(NAME1, METADATA2, VECTOR2);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UnitParameterSensitivity.combine(NAME_COMBINED, base1, base2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy_currency() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    CurrencyParameterSensitivity test = base.multipliedBy(USD, FACTOR1);
    assertThat(test).isEqualTo(CurrencyParameterSensitivity.of(NAME1, METADATA1, USD, VECTOR1_FACTOR));
  }

  @Test
  public void test_multipliedBy() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertThat(test).isEqualTo(UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1_FACTOR));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity test = base.withSensitivity(VECTOR1_FACTOR);
    assertThat(test).isEqualTo(UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1_FACTOR));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withSensitivity(DoubleArray.of(1d)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_array() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity test = base.plus(VECTOR1);
    assertThat(test).isEqualTo(base.multipliedBy(2));
  }

  @Test
  public void test_plus_array_wrongSize() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.plus(VECTOR2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_sensitivity() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity test = base1.plus(base1);
    assertThat(test).isEqualTo(base1.multipliedBy(2));
  }

  @Test
  public void test_plus_sensitivity_wrongName() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity base2 = UnitParameterSensitivity.of(NAME2, METADATA1, VECTOR1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base1.plus(base2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_split1() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    ImmutableList<UnitParameterSensitivity> test = base.split();
    assertThat(test).hasSize(1);
    assertThat(test.get(0)).isEqualTo(base);
  }

  @Test
  public void test_split2() {
    UnitParameterSensitivity base1 = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    UnitParameterSensitivity base2 = UnitParameterSensitivity.of(NAME2, METADATA2, VECTOR2);
    UnitParameterSensitivity combined = UnitParameterSensitivity.combine(NAME_COMBINED, base1, base2);
    ImmutableList<UnitParameterSensitivity> test = combined.split();
    assertThat(test).hasSize(2);
    assertThat(test.get(0)).isEqualTo(base1);
    assertThat(test.get(1)).isEqualTo(base2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_total() {
    UnitParameterSensitivity base = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    double test = base.total();
    assertThat(test).isEqualTo(VECTOR1.get(0) + VECTOR1.get(1) + VECTOR1.get(2) + VECTOR1.get(3));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    UnitParameterSensitivity test = UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
    coverImmutableBean(test);
    UnitParameterSensitivity test2 = UnitParameterSensitivity.of(NAME2, METADATA2, VECTOR2);
    coverBeanEquals(test, test2);
  }

}
