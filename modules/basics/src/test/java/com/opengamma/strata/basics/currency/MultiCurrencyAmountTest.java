/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.currency.MultiCurrencyAmount.toMultiCurrencyAmount;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.beans.BeanBuilder;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Test {@link MultiCurrencyAmount}.
 */
public class MultiCurrencyAmountTest {

  private static final Currency CCY1 = Currency.AUD;
  private static final Currency CCY2 = Currency.CAD;
  private static final Currency CCY3 = Currency.CHF;
  private static final double AMT1 = 101;
  private static final double AMT2 = 103;
  private static final double AMT3 = 107;
  private static final CurrencyAmount CA1 = CurrencyAmount.of(CCY1, AMT1);
  private static final CurrencyAmount CA2 = CurrencyAmount.of(CCY2, AMT2);
  private static final CurrencyAmount CA3 = CurrencyAmount.of(CCY3, AMT3);

  //-------------------------------------------------------------------------
  @Test
  public void test_empty() {
    assertMCA(MultiCurrencyAmount.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_CurrencyDouble() {
    assertMCA(MultiCurrencyAmount.of(CCY1, AMT1), CA1);
  }

  @Test
  public void test_of_CurrencyDouble_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> MultiCurrencyAmount.of(null, AMT1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_VarArgs_empty() {
    assertMCA(MultiCurrencyAmount.of());
  }

  @Test
  public void test_of_VarArgs() {
    assertMCA(MultiCurrencyAmount.of(CA1, CA3), CA1, CA3);
  }

  @Test
  public void test_of_VarArgs_duplicate() {
    assertThatIllegalArgumentException().isThrownBy(() -> MultiCurrencyAmount.of(CA1, CurrencyAmount.of(CCY1, AMT2)));
  }

  @Test
  public void test_of_VarArgs_null() {
    CurrencyAmount[] array = null;
    assertThatIllegalArgumentException().isThrownBy(() -> MultiCurrencyAmount.of(array));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_Iterable() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, CA3);
    assertMCA(MultiCurrencyAmount.of(iterable), CA1, CA3);
  }

  @Test
  public void test_of_Iterable_duplicate() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, CurrencyAmount.of(CCY1, AMT2));
    assertThatIllegalArgumentException().isThrownBy(() -> MultiCurrencyAmount.of(iterable));
  }

  @Test
  public void test_of_Iterable_null() {
    Iterable<CurrencyAmount> iterable = null;
    assertThatIllegalArgumentException().isThrownBy(() -> MultiCurrencyAmount.of(iterable));
  }

  @Test
  public void test_of_Iterable_containsNull() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, null, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> MultiCurrencyAmount.of(iterable));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_Map() {
    Map<Currency, Double> map = ImmutableMap.<Currency, Double>builder()
        .put(CCY1, AMT1)
        .put(CCY3, AMT3)
        .build();
    assertMCA(MultiCurrencyAmount.of(map), CA1, CA3);
  }

  @Test
  public void test_of_Map_null() {
    Map<Currency, Double> map = null;
    assertThatIllegalArgumentException().isThrownBy(() -> MultiCurrencyAmount.of(map));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_total_Iterable() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, CA3);
    assertMCA(MultiCurrencyAmount.total(iterable), CA1, CA3);
  }

  @Test
  public void test_total_Iterable_duplicate() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, CurrencyAmount.of(CCY1, AMT2), CA2);
    assertMCA(MultiCurrencyAmount.total(iterable), CurrencyAmount.of(CCY1, AMT1 + AMT2), CA2);
  }

  @Test
  public void test_total_Iterable_null() {
    Iterable<CurrencyAmount> iterable = null;
    assertThatIllegalArgumentException().isThrownBy(() -> MultiCurrencyAmount.total(iterable));
  }

  @Test
  public void test_total_Iterable_containsNull() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, null, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> MultiCurrencyAmount.total(iterable));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collector() {
    List<CurrencyAmount> amount = ImmutableList.of(
        CurrencyAmount.of(CCY1, 100), CurrencyAmount.of(CCY1, 150), CurrencyAmount.of(CCY2, 100));
    MultiCurrencyAmount test = amount.stream().collect(toMultiCurrencyAmount());
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(CCY1, 250), CurrencyAmount.of(CCY2, 100));
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_collector_parallel() {
    List<CurrencyAmount> amount = ImmutableList.of(
        CurrencyAmount.of(CCY1, 100), CurrencyAmount.of(CCY1, 150), CurrencyAmount.of(CCY2, 100));
    MultiCurrencyAmount test = amount.parallelStream().collect(toMultiCurrencyAmount());
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(CCY1, 250), CurrencyAmount.of(CCY2, 100));
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_collector_null() {
    List<CurrencyAmount> amount = Arrays.asList(
        CurrencyAmount.of(CCY1, 100), null, CurrencyAmount.of(CCY2, 100));
    assertThatIllegalArgumentException().isThrownBy(() -> amount.stream().collect(toMultiCurrencyAmount()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_beanBuilder() {
    MultiCurrencyAmount test = MultiCurrencyAmount.meta().builder()
        .set(MultiCurrencyAmount.meta().amounts(), ImmutableSortedSet.of(CA1, CA2, CA3))
        .build();
    assertMCA(test, CA1, CA2, CA3);
  }

  @Test
  public void test_beanBuilder_invalid() {
    BeanBuilder<? extends MultiCurrencyAmount> test = MultiCurrencyAmount.meta().builder()
        .set(MultiCurrencyAmount.meta().amounts(),
            ImmutableSortedSet.of(CA1, CA2, CurrencyAmount.of(CA1.getCurrency(), AMT3)));
    assertThatIllegalArgumentException().isThrownBy(() -> test.build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_contains_null() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> base.contains(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_CurrencyDouble_merge() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.plus(Currency.AUD, 3);
    assertMCA(test, cb, CurrencyAmount.of(Currency.AUD, 120));
  }

  @Test
  public void test_plus_CurrencyDouble_add() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.plus(Currency.NZD, 3);
    assertMCA(test, ca, cb, CurrencyAmount.of(Currency.NZD, 3));
  }

  @Test
  public void test_plus_CurrencyDouble_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.plus((Currency) null, 1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_CurrencyAmount_merge() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.AUD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.plus(cc);
    assertMCA(test, cb, CurrencyAmount.of(Currency.AUD, 120));
  }

  @Test
  public void test_plus_CurrencyAmount_add() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.NZD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.plus(cc);
    assertMCA(test, ca, cb, cc);
  }

  @Test
  public void test_plus_CurrencyAmount_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.plus((CurrencyAmount) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_MultiCurrencyAmount_mergeAndAdd() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.AUD, 3);
    CurrencyAmount cd = CurrencyAmount.of(Currency.NZD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount mc2 = MultiCurrencyAmount.of(cc, cd);
    MultiCurrencyAmount test = mc1.plus(mc2);
    assertMCA(test, cb, cd, CurrencyAmount.of(Currency.AUD, 120));
  }

  @Test
  public void test_plus_MultiCurrencyAmount_empty() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount mc2 = MultiCurrencyAmount.of();
    MultiCurrencyAmount test = mc1.plus(mc2);
    assertMCA(test, ca, cb);
  }

  @Test
  public void test_plus_MultiCurrencyAmount_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.plus((MultiCurrencyAmount) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_minus_CurrencyDouble_merge() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.minus(Currency.AUD, 3);
    assertMCA(test, cb, CurrencyAmount.of(Currency.AUD, 114));
  }

  @Test
  public void test_minus_CurrencyDouble_add() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.minus(Currency.NZD, 3);
    assertMCA(test, ca, cb, CurrencyAmount.of(Currency.NZD, -3));
  }

  @Test
  public void test_minus_CurrencyDouble_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.minus((Currency) null, 1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_minus_CurrencyAmount_merge() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.AUD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.minus(cc);
    assertMCA(test, cb, CurrencyAmount.of(Currency.AUD, 114));
  }

  @Test
  public void test_minus_CurrencyAmount_add() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.NZD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.minus(cc);
    assertMCA(test, ca, cb, cc.negated());
  }

  @Test
  public void test_minus_CurrencyAmount_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.minus((CurrencyAmount) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_minus_MultiCurrencyAmount_mergeAndAdd() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.AUD, 3);
    CurrencyAmount cd = CurrencyAmount.of(Currency.NZD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount mc2 = MultiCurrencyAmount.of(cc, cd);
    MultiCurrencyAmount test = mc1.minus(mc2);
    assertMCA(test, cb, cd.negated(), CurrencyAmount.of(Currency.AUD, 114));
  }

  @Test
  public void test_minus_MultiCurrencyAmount_empty() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount mc2 = MultiCurrencyAmount.of();
    MultiCurrencyAmount test = mc1.minus(mc2);
    assertMCA(test, ca, cb);
  }

  @Test
  public void test_minus_MultiCurrencyAmount_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.minus((MultiCurrencyAmount) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    MultiCurrencyAmount test = base.multipliedBy(2.5);
    assertMCA(test, CA1.multipliedBy(2.5), CA2.multipliedBy(2.5));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_negated() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    MultiCurrencyAmount test = base.negated();
    assertMCA(test, CA1.negated(), CA2.negated());
    assertThat(MultiCurrencyAmount.of(CurrencyAmount.zero(USD), CurrencyAmount.zero(EUR)).negated())
        .isEqualTo(MultiCurrencyAmount.of(CurrencyAmount.zero(USD), CurrencyAmount.zero(EUR)));
    assertThat(MultiCurrencyAmount.of(CurrencyAmount.of(USD, -0d), CurrencyAmount.of(EUR, -0d)).negated())
        .isEqualTo(MultiCurrencyAmount.of(CurrencyAmount.zero(USD), CurrencyAmount.zero(EUR)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapAmounts() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    MultiCurrencyAmount test = base.mapAmounts(a -> a * 2.5 + 1);
    assertMCA(test, CA1.mapAmount(a -> a * 2.5 + 1), CA2.mapAmount(a -> a * 2.5 + 1));
  }

  @Test
  public void test_mapAmounts_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.mapAmounts(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapCurrencyAmounts() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    MultiCurrencyAmount test = base.mapCurrencyAmounts(a -> CurrencyAmount.of(CCY3, 1));
    assertMCA(test, CurrencyAmount.of(CCY3, 2));
  }

  @Test
  public void test_mapCurrencyAmounts_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.mapCurrencyAmounts(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_stream() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    MultiCurrencyAmount test = base.stream()
        .map(ca -> ca.mapAmount(a -> a * 3))
        .collect(toMultiCurrencyAmount());
    assertMCA(test, CA1.mapAmount(a -> a * 3), CA2.mapAmount(a -> a * 3));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toMap() {
    ImmutableSortedMap<Currency, Double> test = MultiCurrencyAmount.of(CA1, CA2).toMap();
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.containsKey(CA1.getCurrency())).isEqualTo(true);
    assertThat(test.containsKey(CA2.getCurrency())).isEqualTo(true);
    assertThat(test.get(CA1.getCurrency())).isEqualTo(Double.valueOf(CA1.getAmount()));
    assertThat(test.get(CA2.getCurrency())).isEqualTo(Double.valueOf(CA2.getAmount()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo_rateProvider_noConversionSize1() {
    FxRateProvider provider = (ccy1, ccy2) -> {
      throw new IllegalArgumentException();
    };
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA2);
    assertThat(test.convertedTo(CCY2, provider)).isEqualTo(CA2);
  }

  @Test
  public void test_convertedTo_rateProvider_conversionSize1() {
    FxRateProvider provider = (ccy1, ccy2) -> {
      if (ccy1.equals(CCY1) && ccy2.equals(CCY2)) {
        return 2.5d;
      }
      throw new IllegalArgumentException();
    };
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1);
    assertThat(test.convertedTo(CCY2, provider)).isEqualTo(CurrencyAmount.of(CCY2, AMT1 * 2.5d));
  }

  @Test
  public void test_convertedTo_rateProvider_conversionSize2() {
    FxRateProvider provider = (ccy1, ccy2) -> {
      if (ccy1.equals(ccy2)) {
        return 1d;
      }
      if (ccy1.equals(CCY1) && ccy2.equals(CCY2)) {
        return 2.5d;
      }
      throw new IllegalArgumentException();
    };
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThat(test.convertedTo(CCY2, provider)).isEqualTo(CA2.plus(CurrencyAmount.of(CCY2, AMT1 * 2.5d)));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(MultiCurrencyAmount.of(CA1, CA2, CA3));
  }

  @Test
  public void coverage() {
    coverImmutableBean(MultiCurrencyAmount.of(CA1, CA2, CA3));
  }

  //-------------------------------------------------------------------------
  private void assertMCA(MultiCurrencyAmount actual, CurrencyAmount... expected) {
    assertThat(actual.size()).isEqualTo(expected.length);
    assertThat(actual.getAmounts().size()).isEqualTo(expected.length);
    assertThat(actual.getAmounts()).containsOnly(expected);
    Set<Currency> currencies = new HashSet<>();
    for (CurrencyAmount expectedAmount : expected) {
      currencies.add(expectedAmount.getCurrency());
      assertThat(actual.contains(expectedAmount.getCurrency())).isEqualTo(true);
      assertThat(actual.getAmount(expectedAmount.getCurrency())).isEqualTo(expectedAmount);
      assertThat(actual.getAmountOrZero(expectedAmount.getCurrency())).isEqualTo(expectedAmount);
    }
    assertThat(actual.getCurrencies()).isEqualTo(currencies);
    Currency nonExisting = Currency.of("FRZ");
    assertThat(actual.contains(nonExisting)).isEqualTo(false);
    assertThatIllegalArgumentException().isThrownBy(() -> actual.getAmount(nonExisting));
    assertThat(actual.getAmountOrZero(nonExisting)).isEqualTo(CurrencyAmount.zero(nonExisting));
  }

}
