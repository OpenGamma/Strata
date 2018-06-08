/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link TradedPrice}.
 */
@Test
public class TradedPriceTest {

  private static final double PRICE = 123d;
  private static final LocalDate DATE = LocalDate.of(2018, 6, 1);

  //-------------------------------------------------------------------------
  public void test_methods() {
    TradedPrice test = sut();
    assertEquals(test.getTradeDate(), DATE);
    assertEquals(test.getPrice(), PRICE);
  }

  public void coverage() {
    TradedPrice test = sut();
    coverImmutableBean(test);
    TradedPrice test2 = TradedPrice.of(DATE.plusDays(1), PRICE + 1d);
    coverBeanEquals(test, test2);
  }

  //-------------------------------------------------------------------------
  static TradedPrice sut() {
    return TradedPrice.of(DATE, PRICE);
  }

}
