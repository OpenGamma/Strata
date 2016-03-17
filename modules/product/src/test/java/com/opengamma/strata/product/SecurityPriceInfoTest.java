/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link SecurityPriceInfo}.
 */
@Test
public class SecurityPriceInfoTest {

  public void test_of() {
    SecurityPriceInfo test = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01));
    assertEquals(test.getTickSize(), 0.01);
    assertEquals(test.getTickValue(), CurrencyAmount.of(GBP, 0.01));
    assertEquals(test.getContractSize(), 1d);
    assertEquals(test.getCurrency(), GBP);
  }

  public void test_of_withContractSize() {
    SecurityPriceInfo test = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01), 20);
    assertEquals(test.getTickSize(), 0.01);
    assertEquals(test.getTickValue(), CurrencyAmount.of(GBP, 0.01));
    assertEquals(test.getContractSize(), 20d);
    assertEquals(test.getCurrency(), GBP);
  }

  public void test_ofCurrencyMinorUnit_GBP() {
    SecurityPriceInfo test = SecurityPriceInfo.ofCurrencyMinorUnit(GBP);
    assertEquals(test.getTickSize(), 0.01);
    assertEquals(test.getTickValue(), CurrencyAmount.of(GBP, 0.01));
    assertEquals(test.getContractSize(), 1d);
    assertEquals(test.getCurrency(), GBP);
  }

  public void test_ofCurrencyMinorUnit_JPY() {
    SecurityPriceInfo test = SecurityPriceInfo.ofCurrencyMinorUnit(JPY);
    assertEquals(test.getTickSize(), 1d);
    assertEquals(test.getTickValue(), CurrencyAmount.of(JPY, 1));
    assertEquals(test.getContractSize(), 1d);
    assertEquals(test.getCurrency(), JPY);
  }

  //-------------------------------------------------------------------------
  public void test_calculateMonetaryValue1() {
    // CME-ED, 1bp = $25
    SecurityPriceInfo test = SecurityPriceInfo.of(0.005, CurrencyAmount.of(USD, 12.50), 1);
    assertEquals(test.calculateMonetaryValue(1, 98), CurrencyAmount.of(USD, 245_000));
    assertEquals(test.calculateMonetaryValue(1, 98.02), CurrencyAmount.of(USD, 245_050));
    // quantity is simple multiplier
    assertEquals(test.calculateMonetaryValue(2, 98), CurrencyAmount.of(USD, 2 * 245_000));
    assertEquals(test.calculateMonetaryValue(3, 98), CurrencyAmount.of(USD, 3 * 245_000));
    // contract size is simple multiplier
    SecurityPriceInfo test2 = SecurityPriceInfo.of(0.005, CurrencyAmount.of(USD, 12.50), 2);
    assertEquals(test2.calculateMonetaryValue(1, 98), CurrencyAmount.of(USD, 2 * 245_000));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SecurityPriceInfo test = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01));
    coverImmutableBean(test);
    SecurityPriceInfo test2 = SecurityPriceInfo.of(0.02, CurrencyAmount.of(GBP, 1), 20);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SecurityPriceInfo test = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01));
    assertSerialization(test);
  }

}
