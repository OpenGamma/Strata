/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.currency;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Test {@link MultiCurrencyAmount}.
 */
@Test
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
  public void test_of_CurrencyDouble() {
    assertMCA(MultiCurrencyAmount.of(CCY1, AMT1), CA1);
  }

  public void test_of_CurrencyDouble_null() {
    assertThrows(() -> MultiCurrencyAmount.of(null, AMT1), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_VarArgs() {
    assertMCA(MultiCurrencyAmount.of(CA1, CA3), CA1, CA3);
  }

  public void test_of_VarArgs_duplicate() {
    assertThrows(() -> MultiCurrencyAmount.of(CA1, CurrencyAmount.of(CCY1, AMT2)), IllegalArgumentException.class);
  }

  public void test_of_VarArgs_null() {
    CurrencyAmount[] array = null;
    assertThrows(() -> MultiCurrencyAmount.of(array), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_Iterable() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, CA3);
    assertMCA(MultiCurrencyAmount.of(iterable), CA1, CA3);
  }

  public void test_of_Iterable_duplicate() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, CurrencyAmount.of(CCY1, AMT2));
    assertThrows(() -> MultiCurrencyAmount.of(iterable), IllegalArgumentException.class);
  }

  public void test_of_Iterable_null() {
    Iterable<CurrencyAmount> iterable = null;
    assertThrows(() -> MultiCurrencyAmount.of(iterable), IllegalArgumentException.class);
  }

  public void test_of_Iterable_containsNull() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, null, CA2);
    assertThrows(() -> MultiCurrencyAmount.of(iterable), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_Map() {
    Map<Currency, Double> map = ImmutableMap.<Currency, Double>builder()
        .put(CCY1, AMT1)
        .put(CCY3, AMT3)
        .build();
    assertMCA(MultiCurrencyAmount.of(map), CA1, CA3);
  }

  public void test_of_Map_null() {
    Map<Currency, Double> map = null;
    assertThrows(() -> MultiCurrencyAmount.of(map), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_total_Iterable() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, CA3);
    assertMCA(MultiCurrencyAmount.total(iterable), CA1, CA3);
  }

  public void test_total_Iterable_duplicate() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, CurrencyAmount.of(CCY1, AMT2), CA2);
    assertMCA(MultiCurrencyAmount.total(iterable), CurrencyAmount.of(CCY1, AMT1 + AMT2), CA2);
  }

  public void test_total_Iterable_null() {
    Iterable<CurrencyAmount> iterable = null;
    assertThrows(() -> MultiCurrencyAmount.total(iterable), IllegalArgumentException.class);
  }

  public void test_total_Iterable_containsNull() {
    Iterable<CurrencyAmount> iterable = Arrays.asList(CA1, null, CA2);
    assertThrows(() -> MultiCurrencyAmount.total(iterable), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_beanBuilder() {
    MultiCurrencyAmount test = MultiCurrencyAmount.meta().builder()
        .set(MultiCurrencyAmount.meta().amounts(), ImmutableSortedSet.of(CA1, CA2, CA3))
        .build();
    assertMCA(test, CA1, CA2, CA3);
  }

  public void test_beanBuilder_invalid() {
    BeanBuilder<? extends MultiCurrencyAmount> test = MultiCurrencyAmount.meta().builder()
        .set(MultiCurrencyAmount.meta().amounts(),
            ImmutableSortedSet.of(CA1, CA2, CurrencyAmount.of(CA1.getCurrency(), AMT3)));
    assertThrows(() -> test.build(), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_plus_CurrencyDouble_merge() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.plus(Currency.AUD, 3);
    assertMCA(test, cb, CurrencyAmount.of(Currency.AUD, 120));
  }

  public void test_plus_CurrencyDouble_add() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.plus(Currency.NZD, 3);
    assertMCA(test, ca, cb, CurrencyAmount.of(Currency.NZD, 3));
  }

  public void test_plus_CurrencyDouble_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThrows(() -> test.plus((Currency) null, 1), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_plus_CurrencyAmount_merge() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.AUD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.plus(cc);
    assertMCA(test, cb, CurrencyAmount.of(Currency.AUD, 120));
  }

  public void test_plus_CurrencyAmount_add() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.NZD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.plus(cc);
    assertMCA(test, ca, cb, cc);
  }

  public void test_plus_CurrencyAmount_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThrows(() -> test.plus((CurrencyAmount) null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
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

  public void test_plus_MultiCurrencyAmount_empty() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount mc2 = MultiCurrencyAmount.of();
    MultiCurrencyAmount test = mc1.plus(mc2);
    assertMCA(test, ca, cb);
  }

  public void test_plus_MultiCurrencyAmount_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThrows(() -> test.plus((MultiCurrencyAmount) null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_minus_CurrencyDouble_merge() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.minus(Currency.AUD, 3);
    assertMCA(test, cb, CurrencyAmount.of(Currency.AUD, 114));
  }

  public void test_minus_CurrencyDouble_add() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.minus(Currency.NZD, 3);
    assertMCA(test, ca, cb, CurrencyAmount.of(Currency.NZD, -3));
  }

  public void test_minus_CurrencyDouble_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThrows(() -> test.minus((Currency) null, 1), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_minus_CurrencyAmount_merge() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.AUD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.minus(cc);
    assertMCA(test, cb, CurrencyAmount.of(Currency.AUD, 114));
  }

  public void test_minus_CurrencyAmount_add() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    CurrencyAmount cc = CurrencyAmount.of(Currency.NZD, 3);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount test = mc1.minus(cc);
    assertMCA(test, ca, cb, cc.negated());
  }

  public void test_minus_CurrencyAmount_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThrows(() -> test.minus((CurrencyAmount) null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
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

  public void test_minus_MultiCurrencyAmount_empty() {
    CurrencyAmount ca = CurrencyAmount.of(Currency.AUD, 117);
    CurrencyAmount cb = CurrencyAmount.of(Currency.USD, 12);
    MultiCurrencyAmount mc1 = MultiCurrencyAmount.of(ca, cb);
    MultiCurrencyAmount mc2 = MultiCurrencyAmount.of();
    MultiCurrencyAmount test = mc1.minus(mc2);
    assertMCA(test, ca, cb);
  }

  public void test_minus_MultiCurrencyAmount_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThrows(() -> test.minus((MultiCurrencyAmount) null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    MultiCurrencyAmount test = base.multipliedBy(2.5);
    assertMCA(test, CA1.multipliedBy(2.5), CA2.multipliedBy(2.5));
  }

  //-------------------------------------------------------------------------
  public void test_negated() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    MultiCurrencyAmount test = base.negated();
    assertMCA(test, CA1.negated(), CA2.negated());
  }

  //-------------------------------------------------------------------------
  public void test_mapAmounts() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    MultiCurrencyAmount test = base.mapAmounts(a -> a * 2.5 + 1);
    assertMCA(test, CA1.mapAmount(a -> a * 2.5 + 1), CA2.mapAmount(a -> a * 2.5 + 1));
  }

  public void test_mapAmounts_null() {
    MultiCurrencyAmount test = MultiCurrencyAmount.of(CA1, CA2);
    assertThrows(() -> test.mapAmounts(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_stream() {
    MultiCurrencyAmount base = MultiCurrencyAmount.of(CA1, CA2);
    MultiCurrencyAmount test = base.stream()
        .map(ca -> ca.mapAmount(a -> a * 3))
        .collect(MultiCurrencyAmount.collector());
    assertMCA(test, CA1.mapAmount(a -> a * 3), CA2.mapAmount(a -> a * 3));
  }

  //-------------------------------------------------------------------------
  public void test_toMap() {
    ImmutableSortedMap<Currency, Double> test = MultiCurrencyAmount.of(CA1, CA2).toMap();
    assertEquals(test.size(), 2);
    assertEquals(test.containsKey(CA1.getCurrency()), true);
    assertEquals(test.containsKey(CA2.getCurrency()), true);
    assertEquals(test.get(CA1.getCurrency()), Double.valueOf(CA1.getAmount()));
    assertEquals(test.get(CA2.getCurrency()), Double.valueOf(CA2.getAmount()));
  }

  //-----------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(MultiCurrencyAmount.of(CA1, CA2, CA3));
  }

  public void coverage() {
    coverImmutableBean(MultiCurrencyAmount.of(CA1, CA2, CA3));
  }

  //-------------------------------------------------------------------------
  private void assertMCA(MultiCurrencyAmount actual, CurrencyAmount... expected) {
    assertEquals(actual.size(), expected.length);
    assertEquals(actual.getAmounts().size(), expected.length);
    assertEquals(actual.getAmounts(), ImmutableSet.copyOf(expected));
    Set<Currency> currencies = new HashSet<>();
    for (CurrencyAmount expectedAmount : expected) {
      currencies.add(expectedAmount.getCurrency());
      assertEquals(actual.contains(expectedAmount.getCurrency()), true);
      assertEquals(actual.getAmount(expectedAmount.getCurrency()), expectedAmount);
    }
    assertEquals(actual.getCurrencies(), currencies);
    Currency nonExisting = Currency.of("FRZ");
    assertEquals(actual.contains(nonExisting), false);
    assertThrows(() -> actual.getAmount(nonExisting), IllegalArgumentException.class);
  }

}
