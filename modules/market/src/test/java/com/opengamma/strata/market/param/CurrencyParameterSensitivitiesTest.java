/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CurrencyParameterSensitivities}.
 */
public class CurrencyParameterSensitivitiesTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleArray VECTOR_USD1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR_USD2 = DoubleArray.of(1000, 250, 321, 123);
  private static final DoubleArray VECTOR_USD2_IN_EUR = DoubleArray.of(1000 / 1.6, 250 / 1.6, 321 / 1.6, 123 / 1.6);
  private static final DoubleArray VECTOR_ZERO = DoubleArray.of(0, 0, 0, 0);
  private static final DoubleArray TOTAL_USD = DoubleArray.of(1100, 450, 621, 246);
  private static final DoubleArray VECTOR_EUR1 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final DoubleArray VECTOR_EUR1_IN_USD = DoubleArray.of(1000 * 1.6, 250 * 1.6, 321 * 1.6, 123 * 1.6, 321 * 1.6);
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.6d);
  private static final MarketDataName<?> NAME0 = CurveName.of("NAME-0");
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final MarketDataName<?> NAME3 = CurveName.of("NAME-3");
  private static final TenorParameterMetadata TENOR_MD_1Y = TenorParameterMetadata.of(Tenor.TENOR_1Y);
  private static final List<ParameterMetadata> METADATA0 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA1 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA2 = ParameterMetadata.listOfEmpty(5);
  private static final List<ParameterMetadata> METADATA3 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA1B = ImmutableList.of(
      TENOR_MD_1Y,
      TenorParameterMetadata.of(Tenor.TENOR_2Y),
      TenorParameterMetadata.of(Tenor.TENOR_3Y),
      TenorParameterMetadata.of(Tenor.TENOR_4Y));

  private static final CurrencyParameterSensitivity ENTRY_USD =
      CurrencyParameterSensitivity.of(NAME1, METADATA1, USD, VECTOR_USD1);
  private static final CurrencyParameterSensitivity ENTRY_USD2 =
      CurrencyParameterSensitivity.of(NAME1, METADATA1, USD, VECTOR_USD2);
  private static final CurrencyParameterSensitivity ENTRY_USD_TOTAL =
      CurrencyParameterSensitivity.of(NAME1, METADATA1, USD, TOTAL_USD);
  private static final CurrencyParameterSensitivity ENTRY_USD_SMALL =
      CurrencyParameterSensitivity.of(NAME1, ParameterMetadata.listOfEmpty(1), USD, DoubleArray.of(100d));
  private static final CurrencyParameterSensitivity ENTRY_USD2_IN_EUR =
      CurrencyParameterSensitivity.of(NAME1, METADATA1, EUR, VECTOR_USD2_IN_EUR);
  private static final CurrencyParameterSensitivity ENTRY_EUR =
      CurrencyParameterSensitivity.of(NAME2, METADATA2, EUR, VECTOR_EUR1);
  private static final CurrencyParameterSensitivity ENTRY_EUR_IN_USD =
      CurrencyParameterSensitivity.of(NAME2, METADATA2, USD, VECTOR_EUR1_IN_USD);
  private static final CurrencyParameterSensitivity ENTRY_ZERO0 =
      CurrencyParameterSensitivity.of(NAME0, METADATA0, USD, VECTOR_ZERO);
  private static final CurrencyParameterSensitivity ENTRY_ZERO3 =
      CurrencyParameterSensitivity.of(NAME3, METADATA3, USD, VECTOR_ZERO);
  private static final CurrencyParameterSensitivity ENTRY_COMBINED =
      CurrencyParameterSensitivity.combine(NAME3, ENTRY_USD, ENTRY_EUR_IN_USD);

  private static final CurrencyParameterSensitivities SENSI_1 = CurrencyParameterSensitivities.of(ENTRY_USD);
  private static final CurrencyParameterSensitivities SENSI_2 =
      CurrencyParameterSensitivities.of(ImmutableList.of(ENTRY_USD2, ENTRY_EUR));

  private static final double TOLERENCE_CMP = 1.0E-8;

  //-------------------------------------------------------------------------
  @Test
  public void test_empty() {
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.empty();
    assertThat(test.size()).isEqualTo(0);
    assertThat(test.getSensitivities()).hasSize(0);
  }

  @Test
  public void test_of_single() {
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.of(ENTRY_USD);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD);
  }

  @Test
  public void test_of_array_none() {
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.of();
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_of_list_none() {
    ImmutableList<CurrencyParameterSensitivity> list = ImmutableList.of();
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_of_list_notNormalized() {
    ImmutableList<CurrencyParameterSensitivity> list = ImmutableList.of(ENTRY_USD, ENTRY_EUR);
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD, ENTRY_EUR);
  }

  @Test
  public void test_of_list_normalized() {
    ImmutableList<CurrencyParameterSensitivity> list = ImmutableList.of(ENTRY_USD, ENTRY_USD2);
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD_TOTAL);
  }

  @Test
  public void test_of_list_normalizeNotPossible() {
    ImmutableList<CurrencyParameterSensitivity> list = ImmutableList.of(ENTRY_USD, ENTRY_USD_SMALL);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurrencyParameterSensitivities.of(list));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    CurrencyParameterSensitivity entry1 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B, USD, VECTOR_USD1);
    CurrencyParameterSensitivity entry2 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B.subList(0, 2), USD, VECTOR_USD1.subArray(0, 2));
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.builder()
        .add(entry1)
        .add(CurrencyParameterSensitivities.of(entry1))
        .add(entry2)
        .build();
    assertThat(test.getSensitivities()).hasSize(1);
    assertThat(test.getSensitivities().get(0).getParameterMetadata()).isEqualTo(METADATA1B);
    assertThat(test.getSensitivities().get(0).getSensitivity()).isEqualTo(DoubleArray.of(300, 600, 600, 246));
  }

  @Test
  public void test_builder_emptyMetadata() {
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyParameterSensitivities.builder().add(ENTRY_USD));
  }

  @Test
  public void test_builder_mapMetadata() {
    CurrencyParameterSensitivity entry1 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B, USD, DoubleArray.of(0, 1, 2, 3));
    CurrencyParameterSensitivity expected =
        CurrencyParameterSensitivity.of(NAME1, ImmutableList.of(TENOR_MD_1Y), USD, DoubleArray.of(6));
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.builder()
        .add(entry1)
        .mapMetadata(md -> TENOR_MD_1Y)
        .build();
    assertThat(test.getSensitivities()).hasSize(1);
    assertThat(test.getSensitivities().get(0)).isEqualTo(expected);
  }

  @Test
  public void test_builder_mapSensitivities() {
    CurrencyParameterSensitivity entry1 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B, USD, DoubleArray.of(0, 1, 2, 3));
    CurrencyParameterSensitivity expected =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B, USD, DoubleArray.of(0, 2, 4, 6));
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.builder()
        .add(entry1)
        .mapSensitivities((metadata, value) -> value * 2)
        .build();
    assertThat(test.getSensitivities()).hasSize(1);
    assertThat(test.getSensitivities().get(0)).isEqualTo(expected);
  }

  @Test
  public void test_builder_filterSensitivity() {
    CurrencyParameterSensitivity entry1 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B, USD, DoubleArray.of(0, 1, 2, 3));
    CurrencyParameterSensitivity expected =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B.subList(1, 4), USD, DoubleArray.of(1, 2, 3));
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.builder()
        .add(entry1)
        .filterSensitivity(v -> v != 0)
        .build();
    assertThat(test.getSensitivities()).hasSize(1);
    assertThat(test.getSensitivities().get(0)).isEqualTo(expected);
  }

  @Test
  public void test_builder_filterSensitivity_remove() {
    CurrencyParameterSensitivity entry1 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B, USD, DoubleArray.of(1, 1, 1, 1));
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.builder()
        .add(entry1)
        .filterSensitivity(v -> v != 1)
        .build();
    assertThat(test.getSensitivities()).hasSize(0);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getSensitivity() {
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.of(ENTRY_USD);
    assertThat(test.getSensitivity(NAME1, USD)).isEqualTo(ENTRY_USD);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getSensitivity(NAME1, EUR));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getSensitivity(NAME0, USD));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getSensitivity(NAME0, EUR));
  }

  @Test
  public void test_findSensitivity() {
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.of(ENTRY_USD);
    assertThat(test.findSensitivity(NAME1, USD)).isEqualTo(Optional.of(ENTRY_USD));
    assertThat(test.findSensitivity(NAME1, EUR)).isEqualTo(Optional.empty());
    assertThat(test.findSensitivity(NAME0, USD)).isEqualTo(Optional.empty());
    assertThat(test.findSensitivity(NAME0, EUR)).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith_one_notNormalized() {
    CurrencyParameterSensitivities test = SENSI_1.combinedWith(ENTRY_EUR);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD, ENTRY_EUR);
  }

  @Test
  public void test_combinedWith_one_normalized() {
    CurrencyParameterSensitivities test = SENSI_1.combinedWith(ENTRY_USD2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD_TOTAL);
  }

  @Test
  public void test_combinedWith_one_sizeMismatch() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SENSI_1.combinedWith(ENTRY_USD_SMALL));
  }

  @Test
  public void test_combinedWith_other() {
    CurrencyParameterSensitivities test = SENSI_1.combinedWith(SENSI_2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD_TOTAL, ENTRY_EUR);
  }

  @Test
  public void test_combinedWith_otherEmpty() {
    CurrencyParameterSensitivities test = SENSI_1.combinedWith(CurrencyParameterSensitivities.empty());
    assertThat(test).isEqualTo(SENSI_1);
  }

  @Test
  public void test_combinedWith_empty() {
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.empty().combinedWith(SENSI_1);
    assertThat(test).isEqualTo(SENSI_1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mergedWith() {
    CurrencyParameterSensitivity entry1 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B, USD, VECTOR_USD1);
    CurrencyParameterSensitivity entry2 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B.subList(0, 2), USD, VECTOR_USD1.subArray(0, 2));
    CurrencyParameterSensitivities base1 = CurrencyParameterSensitivities.of(entry1);
    CurrencyParameterSensitivities base2 = CurrencyParameterSensitivities.of(entry2);
    CurrencyParameterSensitivities test = base1.mergedWith(base2);
    assertThat(test.getSensitivities()).hasSize(1);
    assertThat(test.getSensitivities().get(0).getParameterMetadata()).isEqualTo(METADATA1B);
    assertThat(test.getSensitivities().get(0).getSensitivity()).isEqualTo(DoubleArray.of(200, 400, 300, 123));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withMarketDataNames() {
    CurrencyParameterSensitivity entry1 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B, USD, DoubleArray.of(0, 1, 2, 3));
    CurrencyParameterSensitivities base = CurrencyParameterSensitivities.of(entry1);
    CurrencyParameterSensitivities test = base.withMarketDataNames(name -> NAME2);
    assertThat(SENSI_1.getSensitivities().get(0).getMarketDataName()).isEqualTo(NAME1);
    assertThat(test.getSensitivities().get(0).getMarketDataName()).isEqualTo(NAME2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withParameterMetadatas() {
    CurrencyParameterSensitivity entry1 =
        CurrencyParameterSensitivity.of(NAME1, METADATA1B, USD, DoubleArray.of(0, 1, 2, 3));
    CurrencyParameterSensitivities base = CurrencyParameterSensitivities.of(entry1);
    CurrencyParameterSensitivities test = base.withParameterMetadatas(md -> TENOR_MD_1Y);
    assertThat(test.getSensitivities().get(0).getParameterMetadata()).hasSize(1);
    assertThat(test.getSensitivities().get(0).getParameterMetadata(0)).isEqualTo(TENOR_MD_1Y);
    assertThat(test.getSensitivities().get(0).getSensitivity()).isEqualTo(DoubleArray.of(6));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo_singleCurrency() {
    CurrencyParameterSensitivities test = SENSI_1.convertedTo(USD, FxMatrix.empty());
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD);
  }

  @Test
  public void test_convertedTo_multipleCurrency() {
    CurrencyParameterSensitivities test = SENSI_2.convertedTo(USD, FX_RATE);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD2, ENTRY_EUR_IN_USD);
  }

  @Test
  public void test_convertedTo_multipleCurrency_mergeWhenSameName() {
    CurrencyParameterSensitivities test = SENSI_1.combinedWith(ENTRY_USD2_IN_EUR).convertedTo(USD, FX_RATE);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD_TOTAL);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_total_singleCurrency() {
    assertThat(SENSI_1.total(USD, FxMatrix.empty()).getAmount()).isCloseTo(VECTOR_USD1.sum(), offset(1e-8));
  }

  @Test
  public void test_total_multipleCurrency() {
    assertThat(SENSI_2.total(USD, FX_RATE).getAmount()).isCloseTo(VECTOR_USD2.sum() + VECTOR_EUR1.sum() * 1.6d, offset(1e-8));
  }

  @Test
  public void test_totalMulti_singleCurrency() {
    assertThat(SENSI_1.total().size()).isEqualTo(1);
    assertThat(SENSI_1.total().getAmount(USD).getAmount()).isCloseTo(VECTOR_USD1.sum(), offset(1e-8));
  }

  @Test
  public void test_totalMulti_multipleCurrency() {
    assertThat(SENSI_2.total().size()).isEqualTo(2);
    assertThat(SENSI_2.total().getAmount(USD).getAmount()).isCloseTo(VECTOR_USD2.sum(), offset(1e-8));
    assertThat(SENSI_2.total().getAmount(EUR).getAmount()).isCloseTo(VECTOR_EUR1.sum(), offset(1e-8));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    CurrencyParameterSensitivities multiplied = SENSI_1.multipliedBy(FACTOR1);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR_USD1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(VECTOR_USD1.get(i) * FACTOR1);
    }
  }

  @Test
  public void test_mapSensitivities() {
    CurrencyParameterSensitivities multiplied = SENSI_1.mapSensitivities(a -> 1 / a);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR_USD1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(1 / VECTOR_USD1.get(i));
    }
  }

  @Test
  public void test_multipliedBy_vs_combinedWith() {
    CurrencyParameterSensitivities multiplied = SENSI_2.multipliedBy(2d);
    CurrencyParameterSensitivities added = SENSI_2.combinedWith(SENSI_2);
    assertThat(multiplied).isEqualTo(added);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_split() {
    CurrencyParameterSensitivities test = CurrencyParameterSensitivities.of(ENTRY_COMBINED).split();
    assertThat(test).isEqualTo(CurrencyParameterSensitivities.of(ENTRY_USD, ENTRY_EUR_IN_USD));
  }

  @Test
  public void test_split_noSplit() {
    CurrencyParameterSensitivities test = SENSI_1.split();
    assertThat(test).isEqualTo(SENSI_1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalWithTolerance() {
    CurrencyParameterSensitivities sensUsdTotal = CurrencyParameterSensitivities.of(ENTRY_USD_TOTAL);
    CurrencyParameterSensitivities sensEur = CurrencyParameterSensitivities.of(ENTRY_EUR);
    CurrencyParameterSensitivities sens1plus2 = SENSI_1.combinedWith(ENTRY_USD2);
    CurrencyParameterSensitivities sensZeroA = CurrencyParameterSensitivities.of(ENTRY_ZERO3);
    CurrencyParameterSensitivities sensZeroB = CurrencyParameterSensitivities.of(ENTRY_ZERO0);
    CurrencyParameterSensitivities sens1plus2plus0a = SENSI_1.combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO0);
    CurrencyParameterSensitivities sens1plus2plus0b = SENSI_1.combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO3);
    CurrencyParameterSensitivities sens1plus2plus0 = SENSI_1
        .combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO0).combinedWith(ENTRY_ZERO3);
    CurrencyParameterSensitivities sens2plus0 = SENSI_2.combinedWith(sensZeroA);
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

    assertThat(sensZeroA.equalWithTolerance(CurrencyParameterSensitivities.empty(), TOLERENCE_CMP)).isTrue();
    assertThat(CurrencyParameterSensitivities.empty().equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(CurrencyParameterSensitivities.empty());
    coverImmutableBean(SENSI_1);
    coverBeanEquals(SENSI_1, SENSI_2);
  }

}
