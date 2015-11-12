/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.future;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Test {@link GenericFuture}.
 */
@Test
public class GenericFutureTest {

  private static final StandardId SYMBOL = StandardId.of("Exchange", "Sym01");
  private static final StandardId SYMBOL2 = StandardId.of("Exchange", "Sym02");
  private static final YearMonth YM_2015_06 = YearMonth.of(2015, 6);
  private static final YearMonth YM_2015_09 = YearMonth.of(2015, 9);
  private static final LocalDate DATE_2015_06 = date(2015, 6, 15);
  private static final LocalDate DATE_2015_09 = date(2015, 9, 15);
  private static final CurrencyAmount USD_10 = CurrencyAmount.of(USD, 10);
  private static final CurrencyAmount GBP_20 = CurrencyAmount.of(GBP, 20);

  //-------------------------------------------------------------------------
  public void test_builder() {
    GenericFuture test = GenericFuture.builder()
        .productId(SYMBOL)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .build();
    assertEquals(test.getProductId(), SYMBOL);
    assertEquals(test.getExpiryMonth(), YM_2015_06);
    assertEquals(test.getExpiryDate(), Optional.of(DATE_2015_06));
    assertEquals(test.getTickSize(), 0.0001);
    assertEquals(test.getTickValue(), USD_10);
    assertEquals(test.getCurrency(), USD);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    GenericFuture test = GenericFuture.builder()
        .productId(SYMBOL)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .build();
    coverImmutableBean(test);
    GenericFuture test2 = GenericFuture.builder()
        .productId(SYMBOL2)
        .expiryMonth(YM_2015_09)
        .expiryDate(DATE_2015_09)
        .tickSize(0.0002)
        .tickValue(GBP_20)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    GenericFuture test = GenericFuture.builder()
        .productId(SYMBOL)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .build();
    assertSerialization(test);
  }

}
