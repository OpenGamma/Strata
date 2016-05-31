/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.basics.currency.Currency.CAD;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;

@Test
public class MultiCurrencyValuesArrayTest {

  private static final MultiCurrencyValuesArray VALUES_ARRAY =
      MultiCurrencyValuesArray.of(
          ImmutableList.of(
              MultiCurrencyAmount.of(
                  CurrencyAmount.of(Currency.GBP, 20),
                  CurrencyAmount.of(Currency.USD, 30),
                  CurrencyAmount.of(Currency.EUR, 40)),
              MultiCurrencyAmount.of(
                  CurrencyAmount.of(Currency.GBP, 21),
                  CurrencyAmount.of(Currency.USD, 32),
                  CurrencyAmount.of(Currency.EUR, 43)),
              MultiCurrencyAmount.of(
                  CurrencyAmount.of(Currency.GBP, 22),
                  CurrencyAmount.of(Currency.USD, 33),
                  CurrencyAmount.of(Currency.EUR, 44))));

  public void createAndGetValues() {
    assertThat(VALUES_ARRAY.getValues(Currency.GBP)).isEqualTo(DoubleArray.of(20, 21, 22));
    assertThat(VALUES_ARRAY.getValues(Currency.USD)).isEqualTo(DoubleArray.of(30, 32, 33));
    assertThat(VALUES_ARRAY.getValues(Currency.EUR)).isEqualTo(DoubleArray.of(40, 43, 44));

    MultiCurrencyValuesArray raggedArray = MultiCurrencyValuesArray.of(
        ImmutableList.of(
            MultiCurrencyAmount.of(
                CurrencyAmount.of(Currency.EUR, 4)),
            MultiCurrencyAmount.of(
                CurrencyAmount.of(Currency.GBP, 21),
                CurrencyAmount.of(Currency.USD, 32),
                CurrencyAmount.of(Currency.EUR, 43)),
            MultiCurrencyAmount.of(
                CurrencyAmount.of(Currency.EUR, 44))));

    assertThat(raggedArray.getScenarioCount()).isEqualTo(3);
    assertThat(raggedArray.getValues(Currency.GBP)).isEqualTo(DoubleArray.of(0, 21, 0));
    assertThat(raggedArray.getValues(Currency.USD)).isEqualTo(DoubleArray.of(0, 32, 0));
    assertThat(raggedArray.getValues(Currency.EUR)).isEqualTo(DoubleArray.of(4, 43, 44));
    assertThrowsIllegalArg(() -> raggedArray.getValues(Currency.AUD));
  }

  public void createByFunction() {
    MultiCurrencyAmount mca1 = MultiCurrencyAmount.of(CurrencyAmount.of(Currency.GBP, 10), CurrencyAmount.of(Currency.USD, 20));
    MultiCurrencyAmount mca2 = MultiCurrencyAmount.of(CurrencyAmount.of(Currency.GBP, 10), CurrencyAmount.of(Currency.EUR, 30));
    MultiCurrencyAmount mca3 = MultiCurrencyAmount.of(CurrencyAmount.of(Currency.USD, 40));
    List<MultiCurrencyAmount> amounts = ImmutableList.of(mca1, mca2, mca3);

    MultiCurrencyValuesArray test = MultiCurrencyValuesArray.of(3, i -> amounts.get(i));
    assertThat(test.get(0)).isEqualTo(mca1.plus(Currency.EUR, 0));
    assertThat(test.get(1)).isEqualTo(mca2.plus(Currency.USD, 0));
    assertThat(test.get(2)).isEqualTo(mca3.plus(Currency.GBP, 0).plus(Currency.EUR, 0));
  }

  public void mapFactoryMethod() {
    MultiCurrencyValuesArray array = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.GBP, DoubleArray.of(20, 21, 22),
            Currency.USD, DoubleArray.of(30, 32, 33),
            Currency.EUR, DoubleArray.of(40, 43, 44)));

    assertThat(array).isEqualTo(VALUES_ARRAY);

    assertThrowsIllegalArg(
        () -> MultiCurrencyValuesArray.of(
            ImmutableMap.of(
                Currency.GBP, DoubleArray.of(20, 21),
                Currency.EUR, DoubleArray.of(40, 43, 44))),
        "Arrays must have the same size.*");
  }

  public void getAllValuesUnsafe() {
    Map<Currency, DoubleArray> expected = ImmutableMap.of(
        Currency.GBP, DoubleArray.of(20, 21, 22),
        Currency.USD, DoubleArray.of(30, 32, 33),
        Currency.EUR, DoubleArray.of(40, 43, 44));
    assertThat(VALUES_ARRAY.getValues()).isEqualTo(expected);
  }

  public void get() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(
        CurrencyAmount.of(Currency.GBP, 22),
        CurrencyAmount.of(Currency.USD, 33),
        CurrencyAmount.of(Currency.EUR, 44));
    assertThat(VALUES_ARRAY.get(2)).isEqualTo(expected);
    assertThrows(() -> VALUES_ARRAY.get(3), IndexOutOfBoundsException.class);
    assertThrows(() -> VALUES_ARRAY.get(-1), IndexOutOfBoundsException.class);
  }

  public void stream() {
    List<MultiCurrencyAmount> expected = ImmutableList.of(
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 20),
            CurrencyAmount.of(Currency.USD, 30),
            CurrencyAmount.of(Currency.EUR, 40)),
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 21),
            CurrencyAmount.of(Currency.USD, 32),
            CurrencyAmount.of(Currency.EUR, 43)),
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 22),
            CurrencyAmount.of(Currency.USD, 33),
            CurrencyAmount.of(Currency.EUR, 44)));

    assertThat(VALUES_ARRAY.stream().collect(toList())).isEqualTo(expected);
  }

  public void convert() {
    FxRatesArray rates1 = FxRatesArray.of(GBP, CAD, DoubleArray.of(2.00, 2.01, 2.02));
    FxRatesArray rates2 = FxRatesArray.of(USD, CAD, DoubleArray.of(1.30, 1.31, 1.32));
    FxRatesArray rates3 = FxRatesArray.of(EUR, CAD, DoubleArray.of(1.4, 1.4, 1.4));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates1, rates2, rates3);
    CurrencyValuesArray convertedArray = VALUES_ARRAY.convertedTo(Currency.CAD, fxProvider);
    DoubleArray expected = DoubleArray.of(
        20 * 2.00 + 30 * 1.30 + 40 * 1.4,
        21 * 2.01 + 32 * 1.31 + 43 * 1.4,
        22 * 2.02 + 33 * 1.32 + 44 * 1.4);
    assertThat(convertedArray.getValues()).isEqualTo(expected);
  }

  public void convertIntoAnExistingCurrency() {
    FxRatesArray rates1 = FxRatesArray.of(USD, GBP, DoubleArray.of(1 / 1.50, 1 / 1.51, 1 / 1.52));
    FxRatesArray rates2 = FxRatesArray.of(EUR, GBP, DoubleArray.of(0.7, 0.7, 0.7));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates1, rates2);
    CurrencyValuesArray convertedArray = VALUES_ARRAY.convertedTo(Currency.GBP, fxProvider);
    assertThat(convertedArray.getCurrency()).isEqualTo(Currency.GBP);
    double[] expected = new double[]{
        20 + 30 / 1.50 + 40 * 0.7,
        21 + 32 / 1.51 + 43 * 0.7,
        22 + 33 / 1.52 + 44 * 0.7};

    for (int i = 0; i < 3; i++) {
      assertThat(convertedArray.get(i).getAmount()).isEqualTo(expected[i], offset(1e-6));
    }
  }

  /**
   * Test the hand-written equals and hashCode methods which correctly handle maps with array values
   */
  public void equalsHashCode() {
    MultiCurrencyValuesArray array =
        MultiCurrencyValuesArray.of(
            ImmutableList.of(
                MultiCurrencyAmount.of(
                    CurrencyAmount.of(Currency.GBP, 20),
                    CurrencyAmount.of(Currency.USD, 30),
                    CurrencyAmount.of(Currency.EUR, 40)),
                MultiCurrencyAmount.of(
                    CurrencyAmount.of(Currency.GBP, 21),
                    CurrencyAmount.of(Currency.USD, 32),
                    CurrencyAmount.of(Currency.EUR, 43)),
                MultiCurrencyAmount.of(
                    CurrencyAmount.of(Currency.GBP, 22),
                    CurrencyAmount.of(Currency.USD, 33),
                    CurrencyAmount.of(Currency.EUR, 44))));
    assertThat(array).isEqualTo(VALUES_ARRAY);
    assertThat(array.hashCode()).isEqualTo(VALUES_ARRAY.hashCode());
  }

  public void getCurrencies() {
    assertThat(VALUES_ARRAY.getCurrencies()).isEqualTo(ImmutableSet.of(Currency.GBP, Currency.USD, Currency.EUR));
  }

  public void coverage() {
    coverImmutableBean(VALUES_ARRAY);
    MultiCurrencyValuesArray test2 = MultiCurrencyValuesArray.of(
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 21),
            CurrencyAmount.of(Currency.USD, 31),
            CurrencyAmount.of(Currency.EUR, 41)),
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 22),
            CurrencyAmount.of(Currency.USD, 33),
            CurrencyAmount.of(Currency.EUR, 44)));
    coverBeanEquals(VALUES_ARRAY, test2);
  }

  public void plusArray() {
    MultiCurrencyValuesArray array1 = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.USD, DoubleArray.of(30, 32, 33),
            Currency.EUR, DoubleArray.of(40, 43, 44),
            Currency.CHF, DoubleArray.of(50, 54, 56)));
    MultiCurrencyValuesArray array2 = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.GBP, DoubleArray.of(20, 21, 22),
            Currency.EUR, DoubleArray.of(140, 143, 144),
            Currency.CHF, DoubleArray.of(250, 254, 256)));
    MultiCurrencyValuesArray expected = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.GBP, DoubleArray.of(20, 21, 22),
            Currency.USD, DoubleArray.of(30, 32, 33),
            Currency.EUR, DoubleArray.of(180, 186, 188),
            Currency.CHF, DoubleArray.of(300, 308, 312)));

    assertThat(array1.plus(array2)).isEqualTo(expected);
  }

  public void plusAmount() {
    MultiCurrencyValuesArray array = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.USD, DoubleArray.of(30, 32, 33),
            Currency.EUR, DoubleArray.of(40, 43, 44),
            Currency.CHF, DoubleArray.of(50, 54, 56)));
    MultiCurrencyAmount amount = MultiCurrencyAmount.of(
        ImmutableMap.of(
            Currency.GBP, 21d,
            Currency.EUR, 143d,
            Currency.CHF, 254d));
    MultiCurrencyValuesArray expected = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.GBP, DoubleArray.of(21, 21, 21),
            Currency.USD, DoubleArray.of(30, 32, 33),
            Currency.EUR, DoubleArray.of(183, 186, 187),
            Currency.CHF, DoubleArray.of(304, 308, 310)));

    assertThat(array.plus(amount)).isEqualTo(expected);
  }

  public void plusDifferentSize() {
    MultiCurrencyValuesArray array1 = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.USD, DoubleArray.of(30, 32),
            Currency.EUR, DoubleArray.of(40, 43),
            Currency.CHF, DoubleArray.of(50, 54)));
    MultiCurrencyValuesArray array2 = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.GBP, DoubleArray.of(20, 21, 22),
            Currency.EUR, DoubleArray.of(140, 143, 144),
            Currency.CHF, DoubleArray.of(250, 254, 256)));

    assertThrowsIllegalArg(() -> array1.plus(array2));
  }

  public void minusArray() {
    MultiCurrencyValuesArray array1 = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.USD, DoubleArray.of(30, 32, 33),
            Currency.EUR, DoubleArray.of(40, 43, 44),
            Currency.CHF, DoubleArray.of(50, 54, 56)));
    MultiCurrencyValuesArray array2 = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.GBP, DoubleArray.of(20, 21, 22),
            Currency.EUR, DoubleArray.of(140, 143, 144),
            Currency.CHF, DoubleArray.of(250, 254, 256)));
    MultiCurrencyValuesArray expected = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.GBP, DoubleArray.of(-20, -21, -22),
            Currency.USD, DoubleArray.of(30, 32, 33),
            Currency.EUR, DoubleArray.of(-100, -100, -100),
            Currency.CHF, DoubleArray.of(-200, -200, -200)));

    assertThat(array1.minus(array2)).isEqualTo(expected);
  }

  public void minusAmount() {
    MultiCurrencyValuesArray array = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.USD, DoubleArray.of(30, 32, 33),
            Currency.EUR, DoubleArray.of(40, 43, 44),
            Currency.CHF, DoubleArray.of(50, 54, 56)));
    MultiCurrencyAmount amount = MultiCurrencyAmount.of(
        ImmutableMap.of(
            Currency.GBP, 21d,
            Currency.EUR, 143d,
            Currency.CHF, 254d));
    MultiCurrencyValuesArray expected = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.GBP, DoubleArray.of(-21, -21, -21),
            Currency.USD, DoubleArray.of(30, 32, 33),
            Currency.EUR, DoubleArray.of(-103, -100, -99),
            Currency.CHF, DoubleArray.of(-204, -200, -198)));

    assertThat(array.minus(amount)).isEqualTo(expected);
  }

  public void minusDifferentSize() {
    MultiCurrencyValuesArray array1 = MultiCurrencyValuesArray.of(
      ImmutableMap.of(
          Currency.USD, DoubleArray.of(30, 32),
          Currency.EUR, DoubleArray.of(40, 43),
          Currency.CHF, DoubleArray.of(50, 54)));
    MultiCurrencyValuesArray array2 = MultiCurrencyValuesArray.of(
        ImmutableMap.of(
            Currency.GBP, DoubleArray.of(20, 21, 22),
            Currency.EUR, DoubleArray.of(140, 143, 144),
            Currency.CHF, DoubleArray.of(250, 254, 256)));

    assertThrowsIllegalArg(() -> array1.minus(array2));
  }
}
