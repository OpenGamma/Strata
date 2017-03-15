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
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CrossGammaParameterSensitivities}.
 */
@Test
public class CrossGammaParameterSensitivitiesTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleMatrix MATRIX_USD1 = DoubleMatrix.of(2, 2, 100, 200, 300, 123);
  private static final DoubleMatrix MATRIX_USD2 = DoubleMatrix.of(2, 2, 1000, 250, 321, 123);
  private static final DoubleMatrix MATRIX_USD2_IN_EUR = DoubleMatrix.of(2, 2, 1000 / 1.6, 250 / 1.6, 321 / 1.6, 123 / 1.6);
  private static final DoubleMatrix MATRIX_USD12 = DoubleMatrix.of(2, 4, 100, 200, 1000, 250, 300, 123, 321, 123);
  private static final DoubleMatrix MATRIX_USD21 = DoubleMatrix.of(2, 4, 1000, 250, -500, -400, 321, 123, -200, -300);
  private static final DoubleMatrix MATRIX_ZERO = DoubleMatrix.of(2, 2, 0, 0, 0, 0);
  private static final DoubleMatrix TOTAL_USD = DoubleMatrix.of(2, 2, 1100, 450, 621, 246);
  private static final DoubleMatrix MATRIX_EUR1 = DoubleMatrix.of(2, 2, 1000, 250, 321, 123);
  private static final DoubleMatrix MATRIX_EUR1_IN_USD =
      DoubleMatrix.of(2, 2, 1000 * 1.6, 250 * 1.6, 321 * 1.6, 123 * 1.6);
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.6d);
  private static final MarketDataName<?> NAME0 = CurveName.of("NAME-0");
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final MarketDataName<?> NAME3 = CurveName.of("NAME-3");
  private static final List<ParameterMetadata> METADATA0 = ParameterMetadata.listOfEmpty(2);
  private static final List<ParameterMetadata> METADATA1 = ParameterMetadata.listOfEmpty(2);
  private static final List<ParameterMetadata> METADATA2 = ParameterMetadata.listOfEmpty(2);
  private static final List<ParameterMetadata> METADATA3 = ParameterMetadata.listOfEmpty(2);

  private static final CrossGammaParameterSensitivity ENTRY_USD =
      CrossGammaParameterSensitivity.of(NAME1, METADATA1, USD, MATRIX_USD1);
  private static final CrossGammaParameterSensitivity ENTRY_USD2 =
      CrossGammaParameterSensitivity.of(NAME1, METADATA1, USD, MATRIX_USD2);
  private static final CrossGammaParameterSensitivity ENTRY_USD_TOTAL =
      CrossGammaParameterSensitivity.of(NAME1, METADATA1, USD, TOTAL_USD);
  private static final CrossGammaParameterSensitivity ENTRY_USD2_IN_EUR =
      CrossGammaParameterSensitivity.of(NAME1, METADATA1, EUR, MATRIX_USD2_IN_EUR);
  private static final CrossGammaParameterSensitivity ENTRY_EUR =
      CrossGammaParameterSensitivity.of(NAME2, METADATA2, EUR, MATRIX_EUR1);
  private static final CrossGammaParameterSensitivity ENTRY_EUR_IN_USD =
      CrossGammaParameterSensitivity.of(NAME2, METADATA2, USD, MATRIX_EUR1_IN_USD);
  private static final CrossGammaParameterSensitivity ENTRY_ZERO0 =
      CrossGammaParameterSensitivity.of(NAME0, METADATA0, USD, MATRIX_ZERO);
  private static final CrossGammaParameterSensitivity ENTRY_ZERO3 =
      CrossGammaParameterSensitivity.of(NAME3, METADATA3, USD, MATRIX_ZERO);
  private static final CrossGammaParameterSensitivity ENTRY_USD12 = CrossGammaParameterSensitivity.of(
      NAME1, METADATA1, ImmutableList.of(Pair.of(NAME1, METADATA1), Pair.of(NAME2, METADATA2)), USD, MATRIX_USD12);
  private static final CrossGammaParameterSensitivity ENTRY_USD21 = CrossGammaParameterSensitivity.of(
      NAME2, METADATA2, ImmutableList.of(Pair.of(NAME1, METADATA1), Pair.of(NAME2, METADATA2)), USD, MATRIX_USD21);

  private static final CrossGammaParameterSensitivities SENSI_1 = CrossGammaParameterSensitivities.of(ENTRY_USD);
  private static final CrossGammaParameterSensitivities SENSI_2 =
      CrossGammaParameterSensitivities.of(ImmutableList.of(ENTRY_USD2, ENTRY_EUR));
  private static final CrossGammaParameterSensitivities SENSI_3 =
      CrossGammaParameterSensitivities.of(ImmutableList.of(ENTRY_USD12, ENTRY_USD21));

  private static final double TOLERENCE_CMP = 1.0E-8;

  //-------------------------------------------------------------------------
  public void test_empty() {
    CrossGammaParameterSensitivities test = CrossGammaParameterSensitivities.empty();
    assertEquals(test.size(), 0);
    assertEquals(test.getSensitivities().size(), 0);
  }

  public void test_of_single() {
    CrossGammaParameterSensitivities test = CrossGammaParameterSensitivities.of(ENTRY_USD);
    assertEquals(test.size(), 1);
    assertEquals(test.getSensitivities(), ImmutableList.of(ENTRY_USD));
  }

  public void test_of_array_none() {
    CrossGammaParameterSensitivities test = CrossGammaParameterSensitivities.of();
    assertEquals(test.size(), 0);
  }

  public void test_of_list_none() {
    ImmutableList<CrossGammaParameterSensitivity> list = ImmutableList.of();
    CrossGammaParameterSensitivities test = CrossGammaParameterSensitivities.of(list);
    assertEquals(test.size(), 0);
  }

  public void test_of_list_notNormalized() {
    ImmutableList<CrossGammaParameterSensitivity> list = ImmutableList.of(ENTRY_USD, ENTRY_EUR);
    CrossGammaParameterSensitivities test = CrossGammaParameterSensitivities.of(list);
    assertEquals(test.size(), 2);
    assertEquals(test.getSensitivities(), ImmutableList.of(ENTRY_USD, ENTRY_EUR));
  }

  public void test_of_list_normalized() {
    ImmutableList<CrossGammaParameterSensitivity> list = ImmutableList.of(ENTRY_USD, ENTRY_USD2);
    CrossGammaParameterSensitivities test = CrossGammaParameterSensitivities.of(list);
    assertEquals(test.size(), 1);
    assertEquals(test.getSensitivities(), ImmutableList.of(ENTRY_USD_TOTAL));
  }

  //-------------------------------------------------------------------------
  public void test_getSensitivity() {
    CrossGammaParameterSensitivities test = CrossGammaParameterSensitivities.of(ENTRY_USD);
    assertEquals(test.getSensitivity(NAME1, USD), ENTRY_USD);
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME1, EUR));
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME0, USD));
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME0, EUR));
  }

  public void test_findSensitivity() {
    CrossGammaParameterSensitivities test = CrossGammaParameterSensitivities.of(ENTRY_USD);
    assertEquals(test.findSensitivity(NAME1, USD), Optional.of(ENTRY_USD));
    assertEquals(test.findSensitivity(NAME1, EUR), Optional.empty());
    assertEquals(test.findSensitivity(NAME0, USD), Optional.empty());
    assertEquals(test.findSensitivity(NAME0, EUR), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_one_notNormalized() {
    CrossGammaParameterSensitivities test = SENSI_1.combinedWith(ENTRY_EUR);
    assertEquals(test.getSensitivities(), ImmutableList.of(ENTRY_USD, ENTRY_EUR));
  }

  public void test_combinedWith_one_normalized() {
    CrossGammaParameterSensitivities test = SENSI_1.combinedWith(ENTRY_USD2);
    assertEquals(test.getSensitivities(), ImmutableList.of(ENTRY_USD_TOTAL));
  }

  public void test_combinedWith_other() {
    CrossGammaParameterSensitivities test = SENSI_1.combinedWith(SENSI_2);
    assertEquals(test.getSensitivities(), ImmutableList.of(ENTRY_USD_TOTAL, ENTRY_EUR));
  }

  public void test_combinedWith_otherEmpty() {
    CrossGammaParameterSensitivities test = SENSI_1.combinedWith(CrossGammaParameterSensitivities.empty());
    assertEquals(test, SENSI_1);
  }

  public void test_combinedWith_empty() {
    CrossGammaParameterSensitivities test = CrossGammaParameterSensitivities.empty().combinedWith(SENSI_1);
    assertEquals(test, SENSI_1);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo_singleCurrency() {
    CrossGammaParameterSensitivities test = SENSI_1.convertedTo(USD, FxMatrix.empty());
    assertEquals(test.getSensitivities(), ImmutableList.of(ENTRY_USD));
  }

  public void test_convertedTo_multipleCurrency() {
    CrossGammaParameterSensitivities test = SENSI_2.convertedTo(USD, FX_RATE);
    assertEquals(test.getSensitivities(), ImmutableList.of(ENTRY_USD2, ENTRY_EUR_IN_USD));
  }

  public void test_convertedTo_multipleCurrency_mergeWhenSameName() {
    CrossGammaParameterSensitivities test = SENSI_1.combinedWith(ENTRY_USD2_IN_EUR).convertedTo(USD, FX_RATE);
    assertEquals(test.getSensitivities(), ImmutableList.of(ENTRY_USD_TOTAL));
  }

  //-------------------------------------------------------------------------
  public void test_total_singleCurrency() {
    assertEquals(SENSI_1.total(USD, FxMatrix.empty()).getAmount(), MATRIX_USD1.total(), 1e-8);
  }

  public void test_total_multipleCurrency() {
    assertEquals(SENSI_2.total(USD, FX_RATE).getAmount(), MATRIX_USD2.total() + MATRIX_EUR1.total() * 1.6d, 1e-8);
  }

  public void test_totalMulti_singleCurrency() {
    assertEquals(SENSI_1.total().size(), 1);
    assertEquals(SENSI_1.total().getAmount(USD).getAmount(), MATRIX_USD1.total(), 1e-8);
  }

  public void test_totalMulti_multipleCurrency() {
    assertEquals(SENSI_2.total().size(), 2);
    assertEquals(SENSI_2.total().getAmount(USD).getAmount(), MATRIX_USD2.total(), 1e-8);
    assertEquals(SENSI_2.total().getAmount(EUR).getAmount(), MATRIX_EUR1.total(), 1e-8);
  }

  public void test_diagonal() {
    assertEquals(SENSI_2.diagonal().size(), 2);
    assertEquals(SENSI_2.diagonal().getSensitivity(NAME1, USD), ENTRY_USD2.diagonal());
    assertEquals(SENSI_2.diagonal().getSensitivity(NAME2, EUR), ENTRY_EUR.diagonal());
    assertEquals(SENSI_3.diagonal().getSensitivity(NAME1, USD), ENTRY_USD12.diagonal());
    assertEquals(SENSI_3.diagonal().getSensitivity(NAME2, USD), ENTRY_USD21.diagonal());
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    CrossGammaParameterSensitivities multiplied = SENSI_1.multipliedBy(FACTOR1);
    DoubleMatrix test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < MATRIX_USD1.columnCount(); i++) {
      for (int j = 0; j < MATRIX_USD1.rowCount(); j++) {
        assertEquals(test.get(i, j), MATRIX_USD1.get(i, j) * FACTOR1);
      }
    }
  }

  public void test_mapSensitivities() {
    CrossGammaParameterSensitivities multiplied = SENSI_1.mapSensitivities(a -> 1 / a);
    DoubleMatrix test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < MATRIX_USD1.columnCount(); i++) {
      for (int j = 0; j < MATRIX_USD1.rowCount(); j++) {
        assertEquals(test.get(i, j), 1 / MATRIX_USD1.get(i, j));
      }
    }
  }

  public void test_multipliedBy_vs_combinedWith() {
    CrossGammaParameterSensitivities multiplied = SENSI_2.multipliedBy(2d);
    CrossGammaParameterSensitivities added = SENSI_2.combinedWith(SENSI_2);
    assertEquals(multiplied, added);
  }

  public void test_getSensitivity_name() {
    assertEquals(SENSI_3.getSensitivity(NAME1, NAME1, USD), ENTRY_USD);
    assertEquals(SENSI_3.getSensitivity(NAME1, NAME2, USD),
        CrossGammaParameterSensitivity.of(NAME1, METADATA1, NAME2, METADATA2, USD, MATRIX_USD2));
    assertEquals(SENSI_3.getSensitivity(NAME2, NAME1, USD),
        CrossGammaParameterSensitivity.of(NAME2, METADATA2, NAME1, METADATA1, USD, MATRIX_USD2));
    assertEquals(SENSI_3.getSensitivity(NAME2, NAME2, USD),
        CrossGammaParameterSensitivity.of(NAME2, METADATA2, NAME2, METADATA2, USD,
            DoubleMatrix.of(2, 2, -500, -400, -200, -300)));
  }

  //-------------------------------------------------------------------------
  public void test_equalWithTolerance() {
    CrossGammaParameterSensitivities sensUsdTotal = CrossGammaParameterSensitivities.of(ENTRY_USD_TOTAL);
    CrossGammaParameterSensitivities sensEur = CrossGammaParameterSensitivities.of(ENTRY_EUR);
    CrossGammaParameterSensitivities sens1plus2 = SENSI_1.combinedWith(ENTRY_USD2);
    CrossGammaParameterSensitivities sensZeroA = CrossGammaParameterSensitivities.of(ENTRY_ZERO3);
    CrossGammaParameterSensitivities sensZeroB = CrossGammaParameterSensitivities.of(ENTRY_ZERO0);
    CrossGammaParameterSensitivities sens1plus2plus0a = SENSI_1.combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO0);
    CrossGammaParameterSensitivities sens1plus2plus0b = SENSI_1.combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO3);
    CrossGammaParameterSensitivities sens1plus2plus0 = SENSI_1
        .combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO0).combinedWith(ENTRY_ZERO3);
    CrossGammaParameterSensitivities sens2plus0 = SENSI_2.combinedWith(sensZeroA);
    assertEquals(SENSI_1.equalWithTolerance(sensZeroA, TOLERENCE_CMP), false);
    assertEquals(SENSI_1.equalWithTolerance(SENSI_1, TOLERENCE_CMP), true);
    assertEquals(SENSI_1.equalWithTolerance(SENSI_2, TOLERENCE_CMP), false);
    assertEquals(SENSI_1.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP), false);
    assertEquals(SENSI_1.equalWithTolerance(sensEur, TOLERENCE_CMP), false);
    assertEquals(SENSI_1.equalWithTolerance(sens1plus2, TOLERENCE_CMP), false);
    assertEquals(SENSI_1.equalWithTolerance(sens2plus0, TOLERENCE_CMP), false);

    assertEquals(SENSI_2.equalWithTolerance(sensZeroA, TOLERENCE_CMP), false);
    assertEquals(SENSI_2.equalWithTolerance(SENSI_1, TOLERENCE_CMP), false);
    assertEquals(SENSI_2.equalWithTolerance(SENSI_2, TOLERENCE_CMP), true);
    assertEquals(SENSI_2.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP), false);
    assertEquals(SENSI_2.equalWithTolerance(sensEur, TOLERENCE_CMP), false);
    assertEquals(SENSI_2.equalWithTolerance(sens1plus2, TOLERENCE_CMP), false);
    assertEquals(SENSI_2.equalWithTolerance(sens2plus0, TOLERENCE_CMP), true);

    assertEquals(sensZeroA.equalWithTolerance(sensZeroA, TOLERENCE_CMP), true);
    assertEquals(sensZeroA.equalWithTolerance(SENSI_1, TOLERENCE_CMP), false);
    assertEquals(sensZeroA.equalWithTolerance(SENSI_2, TOLERENCE_CMP), false);
    assertEquals(sensZeroA.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP), false);
    assertEquals(sensZeroA.equalWithTolerance(sensEur, TOLERENCE_CMP), false);
    assertEquals(sensZeroA.equalWithTolerance(sens1plus2, TOLERENCE_CMP), false);
    assertEquals(sensZeroA.equalWithTolerance(sens2plus0, TOLERENCE_CMP), false);
    assertEquals(sensZeroA.equalWithTolerance(sensZeroB, TOLERENCE_CMP), true);

    assertEquals(sensZeroB.equalWithTolerance(sensZeroB, TOLERENCE_CMP), true);
    assertEquals(sensZeroB.equalWithTolerance(SENSI_1, TOLERENCE_CMP), false);
    assertEquals(sensZeroB.equalWithTolerance(SENSI_2, TOLERENCE_CMP), false);
    assertEquals(sensZeroB.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP), false);
    assertEquals(sensZeroB.equalWithTolerance(sensEur, TOLERENCE_CMP), false);
    assertEquals(sensZeroB.equalWithTolerance(sens1plus2, TOLERENCE_CMP), false);
    assertEquals(sensZeroB.equalWithTolerance(sens2plus0, TOLERENCE_CMP), false);
    assertEquals(sensZeroB.equalWithTolerance(sensZeroA, TOLERENCE_CMP), true);

    assertEquals(sens1plus2.equalWithTolerance(sens1plus2, TOLERENCE_CMP), true);
    assertEquals(sens1plus2.equalWithTolerance(sens1plus2plus0a, TOLERENCE_CMP), true);
    assertEquals(sens1plus2.equalWithTolerance(sens1plus2plus0b, TOLERENCE_CMP), true);
    assertEquals(sens1plus2plus0a.equalWithTolerance(sens1plus2, TOLERENCE_CMP), true);
    assertEquals(sens1plus2plus0a.equalWithTolerance(sens1plus2plus0, TOLERENCE_CMP), true);
    assertEquals(sens1plus2plus0a.equalWithTolerance(sens1plus2plus0a, TOLERENCE_CMP), true);
    assertEquals(sens1plus2plus0a.equalWithTolerance(sens1plus2plus0b, TOLERENCE_CMP), true);
    assertEquals(sens1plus2plus0b.equalWithTolerance(sens1plus2, TOLERENCE_CMP), true);
    assertEquals(sens1plus2plus0b.equalWithTolerance(sens1plus2plus0, TOLERENCE_CMP), true);
    assertEquals(sens1plus2plus0b.equalWithTolerance(sens1plus2plus0a, TOLERENCE_CMP), true);
    assertEquals(sens1plus2plus0b.equalWithTolerance(sens1plus2plus0b, TOLERENCE_CMP), true);
    assertEquals(sens2plus0.equalWithTolerance(sens2plus0, TOLERENCE_CMP), true);

    assertEquals(sensZeroA.equalWithTolerance(CrossGammaParameterSensitivities.empty(), TOLERENCE_CMP), true);
    assertEquals(CrossGammaParameterSensitivities.empty().equalWithTolerance(sensZeroA, TOLERENCE_CMP), true);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(CrossGammaParameterSensitivities.empty());
    coverImmutableBean(SENSI_1);
    coverBeanEquals(SENSI_1, SENSI_2);
  }

}
