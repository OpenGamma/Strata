/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.BasicProjectAssertions.assertThat;
import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.CAD;
import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.NZD;
import static com.opengamma.strata.basics.currency.Currency.SEK;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.currency.FxMatrix.entriesToFxMatrix;
import static com.opengamma.strata.basics.currency.FxMatrix.pairsToFxMatrix;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.core.data.Offset;
import org.joda.beans.Bean;
import org.joda.beans.ser.JodaBeanSer;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.tuple.Pair;

@Test
public class FxMatrixTest {

  private static final double TOLERANCE = 1e-6;
  public static final Offset<Double> TOL = offset(TOLERANCE);

  public void emptyMatrixCanHandleTrivialRate() {
    FxMatrix matrix = FxMatrix.empty();
    assertThat(matrix.getCurrencies()).isEmpty();
    assertThat(matrix.fxRate(USD, USD)).isEqualTo(1.0);
    assertThat(matrix.toString()).isEqualTo("FxMatrix[ : ]");
  }

  public void emptyMatrixCannotDoConversion() {
    FxMatrix matrix = FxMatrix.builder().build();
    assertThat(matrix.getCurrencies()).isEmpty();
    assertThrowsIllegalArg(() -> matrix.fxRate(USD, EUR));
  }

  public void singleRateMatrixByOfCurrencyPairFactory() {
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), 1.6);
    assertThat(matrix.getCurrencies()).containsOnly(GBP, USD);
    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(USD, GBP)).isEqualTo(0.625);
    assertThat(matrix.toString()).isEqualTo("FxMatrix[GBP, USD : [1.0, 1.6],[0.625, 1.0]]");
  }

  public void singleRateMatrixByOfCurrenciesFactory() {
    FxMatrix matrix = FxMatrix.of(GBP, USD, 1.6);
    assertThat(matrix.getCurrencies()).containsOnly(GBP, USD);
    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(USD, GBP)).isEqualTo(0.625);
  }

  public void singleRateMatrixByBuilder() {
    FxMatrix matrix = FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .build();
    assertThat(matrix.getCurrencies()).containsOnly(GBP, USD);
    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(USD, GBP)).isEqualTo(0.625);
  }

  public void canAddRateUsingCurrencyPair() {
    FxMatrix matrix = FxMatrix.builder()
        .addRate(CurrencyPair.of(GBP, USD), 1.6)
        .build();
    assertThat(matrix.getCurrencies()).containsOnly(GBP, USD);
    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(USD, GBP)).isEqualTo(0.625);
  }

  public void singleRateMatrixCannotDoConversionForUnknownCurrency() {
    FxMatrix matrix = FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .build();
    assertThat(matrix.getCurrencies()).containsOnly(GBP, USD);
    assertThrowsIllegalArg(() -> matrix.fxRate(USD, EUR));
  }

  public void matrixCalculatesCrossRates() {

    FxMatrix matrix = FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .addRate(EUR, USD, 1.4)
        .addRate(EUR, CHF, 1.2)
        .build();

    assertThat(matrix.getCurrencies()).containsOnly(GBP, USD, EUR, CHF);

    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(USD, GBP)).isEqualTo(1 / 1.6);
    assertThat(matrix.fxRate(EUR, USD)).isEqualTo(1.4);
    assertThat(matrix.fxRate(USD, EUR)).isEqualTo(1 / 1.4);
    assertThat(matrix.fxRate(EUR, GBP)).isEqualTo(1.4 / 1.6, TOL);
    assertThat(matrix.fxRate(GBP, EUR)).isEqualTo(1.6 / 1.4, TOL);
    assertThat(matrix.fxRate(EUR, CHF)).isEqualTo(1.2);
  }

  public void cannotAddEntryWithNoCommonCurrencyAndBuild() {
    assertThrows(
        () -> FxMatrix.builder()
            .addRate(GBP, USD, 1.6)
            .addRate(CHF, AUD, 1.6)
            .build(),
        IllegalStateException.class);
  }

  public void canAddEntryWithNoCommonCurrencyIfSuppliedBySubsequentEntries() {
    FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .addRate(CHF, AUD, 1.6)  // Cannot be added as nothing to tie it to USD or GBP
        .addRate(EUR, CHF, 1.2)  // Again cannot be added
        .addRate(EUR, USD, 1.4)  // Now everything can be tied together
        .build();
  }

  public void rateCanBeUpdatedInBuilder() {
    FxMatrix matrix = FxMatrix.builder()
        .addRate(GBP, USD, 1.5)
        .addRate(GBP, USD, 1.6)
        .build();
    assertThat(matrix.getCurrencies()).containsOnly(GBP, USD);
    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(USD, GBP)).isEqualTo(0.625);
  }

  public void ratedCanBeUpdatedAndAddedViaBuilder() {
    FxMatrix matrix1 = FxMatrix.builder()
        .addRate(GBP, USD, 1.5)
        .build();

    assertThat(matrix1.getCurrencies()).containsOnly(GBP, USD);
    assertThat(matrix1.fxRate(GBP, USD)).isEqualTo(1.5);

    FxMatrix matrix2 = matrix1.toBuilder()
        .addRate(GBP, USD, 1.6)
        .addRate(EUR, USD, 1.4)
        .build();

    assertThat(matrix2.getCurrencies()).containsOnly(GBP, USD, EUR);
    assertThat(matrix2.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix2.fxRate(EUR, USD)).isEqualTo(1.4);
  }

  public void updatingRateIsNotSymmetric() {

    /*
    Expected data as produced from old analytics FxMatrix
    
    [USD, GBP,    EUR] - {
    USD {1.0 ,0.666, 0.714283},
    GBP {1.5, 1.0,   1.071428},
    EUR {1.4, 0.933, 1.0}}
    
    [USD,     GBP,    EUR] - {
    {1.0,     0.625,  0.66964},
    {1.6,     1.0,    1.071428},
    {1.49333, 0.9333, 1.0}}
    
     [USD,    GBP,    EUR] - {
     {1.0,    0.625,  0.71428},
     {1.6,    1.0,    1.14285},
     {1.4,    0.875,  1.0}}
     */

    FxMatrix matrix1 = FxMatrix.builder()
        .addRate(GBP, USD, 1.5)
        .addRate(EUR, USD, 1.4)
        .build();

    FxMatrix matrix2 = matrix1.toBuilder()
        .addRate(GBP, USD, 1.6)
        .build();

    // Switching the currency order for the update gives a
    // different matrix and has a different effect on the
    // the rates
    FxMatrix matrix3 = matrix1.toBuilder()
        .addRate(USD, GBP, 1 / 1.6)
        .build();

    assertThat(matrix2).isNotEqualTo(matrix3);

    assertThat(matrix1.getCurrencies()).hasSize(3);
    assertThat(matrix1.fxRate(GBP, USD)).isEqualTo(1.5);
    assertThat(matrix1.fxRate(EUR, USD)).isEqualTo(1.4);
    assertThat(matrix1.fxRate(EUR, GBP)).isEqualTo(1.4 / 1.5, TOL);

    // The rate we updated
    assertThat(matrix2.fxRate(GBP, USD)).isEqualTo(1.6);

    // Matrix2 update was restating USD wrt GBP so
    // EUR/USD is affected
    assertThat(matrix2.fxRate(EUR, USD)).isEqualTo(1.4 * (1.6 / 1.5), TOL); // = 1.49333
    // but EUR/GBP is not
    assertThat(matrix2.fxRate(EUR, GBP)).isEqualTo(1.4 / 1.5, TOL);  // = 0.9333

    // The rate we updated
    assertThat(matrix3.fxRate(GBP, USD)).isEqualTo(1.6);

    // As matrix3 update was restating GBP wrt USD, there is
    // no effect on EUR/USD
    assertThat(matrix3.fxRate(EUR, USD)).isEqualTo(1.4);
    // but there is an effect on EUR/GBP
    assertThat(matrix3.fxRate(EUR, GBP)).isEqualTo((1.4 / 1.5) * (1.5 / 1.6), TOL); // = 0.875
  }

  public void rateCanBeUpdatedWithDirectionSwitched() {
    FxMatrix matrix1 = FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .build();

    assertThat(matrix1.getCurrencies()).hasSize(2);
    assertThat(matrix1.fxRate(GBP, USD)).isEqualTo(1.6);

    FxMatrix matrix2 = matrix1.toBuilder()
        .addRate(USD, GBP, 0.625)
        .build();

    assertThat(matrix2.getCurrencies()).hasSize(2);
    assertThat(matrix2.fxRate(GBP, USD)).isEqualTo(1.6);
  }

  public void addSimpleMultipleRates() {

    // Use linked to force the order of evaluation
    // want to see that builder recovers when
    // encountering a currency pair for 2 unknown
    // currencies but which will appear later
    LinkedHashMap<CurrencyPair, Double> rates = new LinkedHashMap<>();
    rates.put(CurrencyPair.of(GBP, USD), 1.6);
    rates.put(CurrencyPair.of(EUR, USD), 1.4);

    FxMatrix matrix = FxMatrix.builder()
        .addRates(rates)
        .build();

    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(USD, GBP)).isEqualTo(1 / 1.6);
    assertThat(matrix.fxRate(EUR, USD)).isEqualTo(1.4);
    assertThat(matrix.fxRate(USD, EUR)).isEqualTo(1 / 1.4);
    assertThat(matrix.fxRate(EUR, GBP)).isEqualTo(1.4 / 1.6, TOL);
    assertThat(matrix.fxRate(GBP, EUR)).isEqualTo(1.6 / 1.4, TOL);
  }

  public void addMultipleRatesContainingEntryWithNoCommonCurrency() {

    LinkedHashMap<CurrencyPair, Double> rates = new LinkedHashMap<>();
    rates.put(CurrencyPair.of(GBP, USD), 1.6);
    rates.put(CurrencyPair.of(EUR, USD), 1.4);
    rates.put(CurrencyPair.of(JPY, CAD), 0.01); // Neither currency linked to one of the others

    assertThrows(
        () -> FxMatrix.builder().addRates(rates).build(),
        IllegalStateException.class);
  }

  public void addMultipleRates() {

    // Use linked map to force the order of evaluation
    // want to see that builder recovers when
    // encountering a currency pair for 2 unknown
    // currencies but which will appear later
    LinkedHashMap<CurrencyPair, Double> rates = new LinkedHashMap<>();
    rates.put(CurrencyPair.of(GBP, USD), 1.6);
    rates.put(CurrencyPair.of(EUR, USD), 1.4);
    rates.put(CurrencyPair.of(CHF, AUD), 1.2);  // Neither currency seen before
    rates.put(CurrencyPair.of(SEK, AUD), 0.16); // AUD seen before but not added yet
    rates.put(CurrencyPair.of(JPY, CAD), 0.01); // Neither currency seen before
    rates.put(CurrencyPair.of(EUR, CHF), 1.2);
    rates.put(CurrencyPair.of(JPY, USD), 0.0084);

    FxMatrix matrix = FxMatrix.builder()
        .addRates(rates)
        .build();

    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(USD, GBP)).isEqualTo(1 / 1.6);
    assertThat(matrix.fxRate(EUR, USD)).isEqualTo(1.4);
    assertThat(matrix.fxRate(USD, EUR)).isEqualTo(1 / 1.4);
    assertThat(matrix.fxRate(EUR, GBP)).isEqualTo(1.4 / 1.6, TOL);
    assertThat(matrix.fxRate(GBP, EUR)).isEqualTo(1.6 / 1.4, TOL);
    assertThat(matrix.fxRate(EUR, CHF)).isEqualTo(1.2);
  }

  public void streamEntriesToMatrix() {

    // If we obtain a stream of rates we can collect to an fx matrix
    Map<CurrencyPair, Double> rates =
        ImmutableMap.<CurrencyPair, Double>builder()
            .put(CurrencyPair.of(GBP, USD), 1.6)
            .put(CurrencyPair.of(EUR, USD), 1.4)
            .put(CurrencyPair.of(CHF, AUD), 1.2) // Neither currency seen before
            .put(CurrencyPair.of(SEK, AUD), 0.1) // AUD seen before but not added yet
            .put(CurrencyPair.of(JPY, CAD), 0.0) // Neither currency seen before
            .put(CurrencyPair.of(EUR, CHF), 1.2)
            .put(CurrencyPair.of(JPY, USD), 0.008)
            .build();

    FxMatrix matrix = rates.entrySet()
        .stream()
        .collect(entriesToFxMatrix());

    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(EUR, USD)).isEqualTo(1.4);
  }

  public void streamPairsToMatrix() {

    // If we obtain a stream of pairs with rates we can stream them
    // This could happen if an entry set undergoes a map operation

    Map<CurrencyPair, Double> rates =
        ImmutableMap.<CurrencyPair, Double>builder()
            .put(CurrencyPair.of(GBP, USD), 1.6)
            .put(CurrencyPair.of(EUR, USD), 1.4)
            .put(CurrencyPair.of(CHF, AUD), 1.2) // Neither currency seen before
            .put(CurrencyPair.of(SEK, AUD), 0.1) // AUD seen before but not added yet
            .put(CurrencyPair.of(JPY, CAD), 0.0) // Neither currency seen before
            .put(CurrencyPair.of(EUR, CHF), 1.2)
            .put(CurrencyPair.of(JPY, USD), 0.008)
            .build();

    FxMatrix matrix = rates.entrySet()
        .stream()
        .map(e -> Pair.of(e.getKey(), e.getValue() * 1.01)) // Apply some shift
        .collect(pairsToFxMatrix());

    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.616);
    assertThat(matrix.fxRate(EUR, USD)).isEqualTo(1.414);
  }

  // By adding more than 8 currencies we force a resizing
  // operation - ensure it causes no issues
  public void addMultipleRatesSingle() {

    FxMatrix matrix = FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .addRate(EUR, USD, 1.4)
        .addRate(EUR, CHF, 1.2)
        .addRate(EUR, CHF, 1.2)
        .addRate(CHF, AUD, 1.2)
        .addRate(SEK, AUD, 0.16)
        .addRate(JPY, USD, 0.0084)
        .addRate(JPY, CAD, 0.01)
        .addRate(USD, NZD, 1.3)
        .build();

    assertThat(matrix.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(matrix.fxRate(USD, GBP)).isEqualTo(1 / 1.6);
    assertThat(matrix.fxRate(EUR, USD)).isEqualTo(1.4, TOL);
    assertThat(matrix.fxRate(USD, EUR)).isEqualTo(1 / 1.4, TOL);
    assertThat(matrix.fxRate(EUR, GBP)).isEqualTo(1.4 / 1.6, TOL);
    assertThat(matrix.fxRate(GBP, EUR)).isEqualTo(1.6 / 1.4, TOL);
    assertThat(matrix.fxRate(EUR, CHF)).isEqualTo(1.2);
  }

  public void convertCurrencyAmount() {

    FxMatrix matrix = FxMatrix.builder()
        .addRate(GBP, EUR, 1.4)
        .addRate(GBP, USD, 1.6)
        .build();

    CurrencyAmount amount = CurrencyAmount.of(GBP, 1600);

    assertThat(matrix.convert(amount, GBP)).isEqualTo(amount);

    assertThat(matrix.convert(amount, USD))
        .hasCurrency(USD)
        .hasAmount(2560);

    assertThat(matrix.convert(amount, EUR))
        .hasCurrency(EUR)
        .hasAmount(2240);
  }

  public void convertMultipleCurrencyAmountWithNoEntries() {

    FxMatrix matrix = FxMatrix.builder()
        .addRate(GBP, EUR, 1.4)
        .addRate(GBP, USD, 1.6)
        .build();

    MultiCurrencyAmount amount = MultiCurrencyAmount.of();

    assertThat(matrix.convert(amount, GBP))
        .hasCurrency(GBP)
        .hasAmount(0);

    assertThat(matrix.convert(amount, USD))
        .hasCurrency(USD)
        .hasAmount(0);

    assertThat(matrix.convert(amount, EUR))
        .hasCurrency(EUR)
        .hasAmount(0);
  }

  public void convertMultipleCurrencyAmountWithSingleEntry() {

    FxMatrix matrix = FxMatrix.builder()
        .addRate(GBP, EUR, 1.4)
        .addRate(GBP, USD, 1.6)
        .build();

    MultiCurrencyAmount amount = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1600));

    assertThat(matrix.convert(amount, GBP))
        .hasCurrency(GBP)
        .hasAmount(1600);

    assertThat(matrix.convert(amount, USD))
        .hasCurrency(USD)
        .hasAmount(2560);

    assertThat(matrix.convert(amount, EUR))
        .hasCurrency(EUR)
        .hasAmount(2240);
  }

  public void convertMultipleCurrencyAmountWithMultipleEntries() {

    FxMatrix matrix = FxMatrix.builder()
        .addRate(GBP, EUR, 1.4)
        .addRate(GBP, USD, 1.6)
        .build();

    MultiCurrencyAmount amount = MultiCurrencyAmount.of(
        CurrencyAmount.of(GBP, 1600),
        CurrencyAmount.of(EUR, 1200),
        CurrencyAmount.of(USD, 1500));

    assertThat(matrix.convert(amount, GBP))
        .hasCurrency(GBP)
        .hasAmount(1600d + (1200 / 1.4) + (1500 / 1.6), TOL);

    assertThat(matrix.convert(amount, USD))
        .hasCurrency(USD)
        .hasAmount((1600d * 1.6) + ((1200 / 1.4) * 1.6) + 1500);

    assertThat(matrix.convert(amount, EUR))
        .hasCurrency(EUR)
        .hasAmount((1600d * 1.4) + 1200 + ((1500 / 1.6) * 1.4));
  }

  public void cannotMergeDisjointMatrices() {

    FxMatrix matrix1 = FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .addRate(EUR, USD, 1.4)
        .build();

    FxMatrix matrix2 = FxMatrix.builder()
        .addRate(CHF, AUD, 1.2)
        .addRate(SEK, AUD, 0.16)
        .build();

    assertThrowsIllegalArg(() -> matrix1.merge(matrix2));
  }

  public void mergeIgnoresDuplicateCurrencies() {

    FxMatrix matrix1 = FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .addRate(EUR, USD, 1.4)
        .addRate(EUR, CHF, 1.2)
        .build();

    FxMatrix matrix2 = FxMatrix.builder()
        .addRate(GBP, USD, 1.7)
        .addRate(EUR, USD, 1.5)
        .addRate(EUR, CHF, 1.3)
        .build();

    FxMatrix result = matrix1.merge(matrix2);
    assertThat(result).isEqualTo(matrix1);
  }

  public void mergeAddsInAdditionalCurrencies() {
    FxMatrix matrix1 = FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .addRate(EUR, USD, 1.4)
        .build();

    FxMatrix matrix2 = FxMatrix.builder()
        .addRate(EUR, CHF, 1.2)
        .addRate(CHF, AUD, 1.2)
        .build();

    FxMatrix result = matrix1.merge(matrix2);
    assertThat(result.getCurrencies()).contains(USD, GBP, EUR, CHF, AUD);

    assertThat(result.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(result.fxRate(GBP, EUR)).isEqualTo(1.6 / 1.4, TOL);

    assertThat(result.fxRate(EUR, CHF)).isEqualTo(1.2);
    assertThat(result.fxRate(CHF, AUD)).isEqualTo(1.2);

    assertThat(result.fxRate(GBP, CHF)).isEqualTo((1.6 / 1.4) * 1.2, TOL);
    assertThat(result.fxRate(GBP, AUD)).isEqualTo((1.6 / 1.4) * 1.2 * 1.2, TOL);
  }

  public void equalsGood() {
    FxMatrix m1 = FxMatrix.builder()
        .addRate(GBP, USD, 1.4)
        .build();
    FxMatrix m2 = FxMatrix.builder()
        .addRate(GBP, USD, 1.39)
        .build();
    FxMatrix m3 = FxMatrix.builder()
        .addRate(GBP, USD, 1.39)
        .build();
    FxMatrix m4 = FxMatrix.builder()
        .addRate(GBP, EUR, 1.2)
        .build();

    assertThat(m1.equals(m1)).isTrue();
    assertThat(m2.equals(m2)).isTrue();
    assertThat(m3.equals(m3)).isTrue();
    assertThat(m4.equals(m4)).isTrue();

    assertThat(m1.equals(m2)).isFalse();
    assertThat(m1.equals(m4)).isFalse();

    assertThat(m2.equals(m3)).isTrue();
  }

  public void equalsBad() {
    FxMatrix test = FxMatrix.builder()
        .addRate(USD, GBP, 1.4)
        .build();
    assertThat(test.equals("")).isFalse();
    assertThat(test.equals(null)).isFalse();
  }

  public void hashCodeCoverage() {
    FxMatrix m1 = FxMatrix.builder()
        .addRate(GBP, USD, 1.4)
        .build();
    FxMatrix m2 = FxMatrix.builder()
        .addRate(GBP, USD, 1.39)
        .build();
    FxMatrix m3 = FxMatrix.builder()
        .addRate(GBP, USD, 1.39)
        .build();

    assertThat(m1.hashCode()).isNotEqualTo(m2.hashCode());
    assertThat(m2.hashCode()).isEqualTo(m3.hashCode());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(FxMatrix.empty());
    coverImmutableBean(FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .addRate(EUR, USD, 1.4)
        .addRate(EUR, CHF, 1.2)
        .build());
  }

  public void testSerializeDeserialize() {
    FxMatrix test1 = FxMatrix.builder()
        .addRate(GBP, USD, 1.6)
        .addRate(EUR, USD, 1.4)
        .addRate(EUR, CHF, 1.2)
        .build();
    FxMatrix test2 = FxMatrix.builder()
        .addRate(GBP, USD, 1.7)
        .addRate(EUR, USD, 1.5)
        .addRate(EUR, CHF, 1.3)
        .build();
    cycleBean(FxMatrix.empty());
    cycleBean(test1);
    cycleBean(test2);
    assertSerialization(FxMatrix.empty());
    assertSerialization(test1);
    assertSerialization(test2);
  }

  private void cycleBean(Bean bean) {
    JodaBeanSer ser = JodaBeanSer.COMPACT;
    String result = ser.xmlWriter().write(bean);
    Bean cycled = ser.xmlReader().read(result);
    assertThat(cycled).isEqualTo(bean);
  }

}
