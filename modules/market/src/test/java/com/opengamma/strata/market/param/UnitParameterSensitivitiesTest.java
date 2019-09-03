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
 * Test {@link UnitParameterSensitivities}.
 */
public class UnitParameterSensitivitiesTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleArray VECTOR1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR2 = DoubleArray.of(1000, 250, 321, 123);
  private static final DoubleArray VECTOR_ZERO = DoubleArray.of(0, 0, 0, 0);
  private static final DoubleArray TOTAL_USD = DoubleArray.of(1100, 450, 621, 246);
  private static final DoubleArray VECTOR3 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final MarketDataName<?> NAME0 = CurveName.of("NAME-0");
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final MarketDataName<?> NAME3 = CurveName.of("NAME-3");
  private static final List<ParameterMetadata> METADATA0 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA1 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA2 = ParameterMetadata.listOfEmpty(5);
  private static final List<ParameterMetadata> METADATA3 = ParameterMetadata.listOfEmpty(4);

  private static final UnitParameterSensitivity ENTRY1 =
      UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR1);
  private static final UnitParameterSensitivity ENTRY2 =
      UnitParameterSensitivity.of(NAME1, METADATA1, VECTOR2);
  private static final UnitParameterSensitivity ENTRY_TOTAL_1_2 =
      UnitParameterSensitivity.of(NAME1, METADATA1, TOTAL_USD);
  private static final UnitParameterSensitivity ENTRY_SMALL =
      UnitParameterSensitivity.of(NAME1, ParameterMetadata.listOfEmpty(1), DoubleArray.of(100d));
  private static final UnitParameterSensitivity ENTRY3 =
      UnitParameterSensitivity.of(NAME2, METADATA2, VECTOR3);
  private static final UnitParameterSensitivity ENTRY_ZERO0 =
      UnitParameterSensitivity.of(NAME0, METADATA0, VECTOR_ZERO);
  private static final UnitParameterSensitivity ENTRY_ZERO3 =
      UnitParameterSensitivity.of(NAME3, METADATA3, VECTOR_ZERO);
  private static final UnitParameterSensitivity ENTRY_COMBINED =
      UnitParameterSensitivity.combine(NAME3, ENTRY1, ENTRY3);

  private static final UnitParameterSensitivities SENSI_1 = UnitParameterSensitivities.of(ENTRY1);
  private static final UnitParameterSensitivities SENSI_2 =
      UnitParameterSensitivities.of(ImmutableList.of(ENTRY2, ENTRY3));

  private static final double TOLERENCE_CMP = 1.0E-8;

  //-------------------------------------------------------------------------
  @Test
  public void test_empty() {
    UnitParameterSensitivities test = UnitParameterSensitivities.empty();
    assertThat(test.size()).isEqualTo(0);
    assertThat(test.getSensitivities()).hasSize(0);
  }

  @Test
  public void test_of_single() {
    UnitParameterSensitivities test = UnitParameterSensitivities.of(ENTRY1);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsExactly(ENTRY1);
  }

  @Test
  public void test_of_array_none() {
    UnitParameterSensitivities test = UnitParameterSensitivities.of();
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_of_list_none() {
    ImmutableList<UnitParameterSensitivity> list = ImmutableList.of();
    UnitParameterSensitivities test = UnitParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_of_list_notNormalized() {
    ImmutableList<UnitParameterSensitivity> list = ImmutableList.of(ENTRY1, ENTRY3);
    UnitParameterSensitivities test = UnitParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY1, ENTRY3);
  }

  @Test
  public void test_of_list_normalized() {
    ImmutableList<UnitParameterSensitivity> list = ImmutableList.of(ENTRY1, ENTRY2);
    UnitParameterSensitivities test = UnitParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_TOTAL_1_2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getSensitivity() {
    UnitParameterSensitivities test = UnitParameterSensitivities.of(ENTRY1);
    assertThat(test.getSensitivity(NAME1)).isEqualTo(ENTRY1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getSensitivity(NAME0));
  }

  @Test
  public void test_findSensitivity() {
    UnitParameterSensitivities test = UnitParameterSensitivities.of(ENTRY1);
    assertThat(test.findSensitivity(NAME1)).isEqualTo(Optional.of(ENTRY1));
    assertThat(test.findSensitivity(NAME0)).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith_one_notNormalized() {
    UnitParameterSensitivities test = SENSI_1.combinedWith(ENTRY3);
    assertThat(test.getSensitivities()).containsExactly(ENTRY1, ENTRY3);
  }

  @Test
  public void test_combinedWith_one_normalized() {
    UnitParameterSensitivities test = SENSI_1.combinedWith(ENTRY2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_TOTAL_1_2);
  }

  @Test
  public void test_combinedWith_one_sizeMismatch() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SENSI_1.combinedWith(ENTRY_SMALL));
  }

  @Test
  public void test_combinedWith_other() {
    UnitParameterSensitivities test = SENSI_1.combinedWith(SENSI_2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_TOTAL_1_2, ENTRY3);
  }

  @Test
  public void test_combinedWith_otherEmpty() {
    UnitParameterSensitivities test = SENSI_1.combinedWith(UnitParameterSensitivities.empty());
    assertThat(test).isEqualTo(SENSI_1);
  }

  @Test
  public void test_combinedWith_empty() {
    UnitParameterSensitivities test = UnitParameterSensitivities.empty().combinedWith(SENSI_1);
    assertThat(test).isEqualTo(SENSI_1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy_currency() {
    CurrencyParameterSensitivities multiplied = SENSI_2.multipliedBy(USD, FACTOR1);
    assertThat(multiplied.size()).isEqualTo(2);
    DoubleArray test1 = multiplied.getSensitivity(NAME1, USD).getSensitivity();
    for (int i = 0; i < VECTOR1.size(); i++) {
      assertThat(test1.get(i)).isEqualTo(VECTOR2.get(i) * FACTOR1);
    }
    DoubleArray test2 = multiplied.getSensitivity(NAME2, USD).getSensitivity();
    for (int i = 0; i < VECTOR1.size(); i++) {
      assertThat(test2.get(i)).isEqualTo(VECTOR3.get(i) * FACTOR1);
    }
  }

  @Test
  public void test_multipliedBy() {
    UnitParameterSensitivities multiplied = SENSI_1.multipliedBy(FACTOR1);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(VECTOR1.get(i) * FACTOR1);
    }
  }

  @Test
  public void test_mapSensitivities() {
    UnitParameterSensitivities multiplied = SENSI_1.mapSensitivities(a -> 1 / a);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(1 / VECTOR1.get(i));
    }
  }

  @Test
  public void test_multipliedBy_vs_combinedWith() {
    UnitParameterSensitivities multiplied = SENSI_2.multipliedBy(2d);
    UnitParameterSensitivities added = SENSI_2.combinedWith(SENSI_2);
    assertThat(multiplied).isEqualTo(added);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_split() {
    UnitParameterSensitivities test = UnitParameterSensitivities.of(ENTRY_COMBINED).split();
    assertThat(test).isEqualTo(UnitParameterSensitivities.of(ENTRY1, ENTRY3));
  }

  @Test
  public void test_split_noSplit() {
    UnitParameterSensitivities test = SENSI_1.split();
    assertThat(test).isEqualTo(SENSI_1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalWithTolerance() {
    UnitParameterSensitivities sensUsdTotal = UnitParameterSensitivities.of(ENTRY_TOTAL_1_2);
    UnitParameterSensitivities sensEur = UnitParameterSensitivities.of(ENTRY3);
    UnitParameterSensitivities sens1plus2 = SENSI_1.combinedWith(ENTRY2);
    UnitParameterSensitivities sensZeroA = UnitParameterSensitivities.of(ENTRY_ZERO3);
    UnitParameterSensitivities sensZeroB = UnitParameterSensitivities.of(ENTRY_ZERO0);
    UnitParameterSensitivities sens1plus2plus0a = SENSI_1.combinedWith(ENTRY2).combinedWith(ENTRY_ZERO0);
    UnitParameterSensitivities sens1plus2plus0b = SENSI_1.combinedWith(ENTRY2).combinedWith(ENTRY_ZERO3);
    UnitParameterSensitivities sens1plus2plus0 = SENSI_1
        .combinedWith(ENTRY2).combinedWith(ENTRY_ZERO0).combinedWith(ENTRY_ZERO3);
    UnitParameterSensitivities sens2plus0 = SENSI_2.combinedWith(sensZeroA);
    assertThat(SENSI_1.equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isTrue();
    assertThat(SENSI_1.equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(sensEur, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isFalse();

    assertThat(SENSI_2.equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isTrue();
    assertThat(SENSI_2.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(sensEur, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isTrue();

    assertThat(sensZeroA.equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isTrue();
    assertThat(sensZeroA.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sensEur, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sensZeroB, TOLERENCE_CMP)).isTrue();

    assertThat(sensZeroB.equalWithTolerance(sensZeroB, TOLERENCE_CMP)).isTrue();
    assertThat(sensZeroB.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sensEur, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isTrue();

    assertThat(sens1plus2.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2.equalWithTolerance(sens1plus2plus0a, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2.equalWithTolerance(sens1plus2plus0b, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0a.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0a.equalWithTolerance(sens1plus2plus0, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0a.equalWithTolerance(sens1plus2plus0a, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0a.equalWithTolerance(sens1plus2plus0b, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0b.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0b.equalWithTolerance(sens1plus2plus0, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0b.equalWithTolerance(sens1plus2plus0a, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0b.equalWithTolerance(sens1plus2plus0b, TOLERENCE_CMP)).isTrue();
    assertThat(sens2plus0.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isTrue();

    assertThat(sensZeroA.equalWithTolerance(UnitParameterSensitivities.empty(), TOLERENCE_CMP)).isTrue();
    assertThat(UnitParameterSensitivities.empty().equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(UnitParameterSensitivities.empty());
    coverImmutableBean(SENSI_1);
    coverBeanEquals(SENSI_1, SENSI_2);
  }

}
