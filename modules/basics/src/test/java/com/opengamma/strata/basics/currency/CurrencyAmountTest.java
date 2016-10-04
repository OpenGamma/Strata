/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link CurrencyAmount}.
 */
@Test
public class CurrencyAmountTest {

  private static final Currency CCY1 = Currency.AUD;
  private static final Currency CCY2 = Currency.CAD;
  private static final double AMT1 = 100;
  private static final double AMT2 = 200;
  private static final CurrencyAmount CCY_AMOUNT = CurrencyAmount.of(CCY1, AMT1);
  private static final CurrencyAmount CCY_AMOUNT_NEGATIVE = CurrencyAmount.of(CCY1, -AMT1);

  public void test_fixture() {
    assertEquals(CCY_AMOUNT.getCurrency(), CCY1);
    assertEquals(CCY_AMOUNT.getAmount(), AMT1, 0);
  }

  //-------------------------------------------------------------------------
  public void test_zero_Currency() {
    CurrencyAmount test = CurrencyAmount.zero(Currency.USD);
    assertEquals(test.getCurrency(), Currency.USD);
    assertEquals(test.getAmount(), 0d, 0);
  }

  public void test_zero_Currency_nullCurrency() {
    assertThrowsIllegalArg(() -> CurrencyAmount.zero(null));
  }

  //-------------------------------------------------------------------------
  public void test_of_Currency() {
    CurrencyAmount test = CurrencyAmount.of(Currency.USD, AMT1);
    assertEquals(test.getCurrency(), Currency.USD);
    assertEquals(test.getAmount(), AMT1, 0);
  }

  public void test_of_Currency_nullCurrency() {
    assertThrowsIllegalArg(() -> CurrencyAmount.of((Currency) null, AMT1));
  }

  //-------------------------------------------------------------------------
  public void test_of_String() {
    CurrencyAmount test = CurrencyAmount.of("USD", AMT1);
    assertEquals(test.getCurrency(), Currency.USD);
    assertEquals(test.getAmount(), AMT1, 0);
  }

  public void test_of_String_nullCurrency() {
    assertThrowsIllegalArg(() -> CurrencyAmount.of((String) null, AMT1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_String_roundTrip() {
    assertEquals(CurrencyAmount.parse(CCY_AMOUNT.toString()), CCY_AMOUNT);
  }

  @DataProvider(name = "parseGood")
  Object[][] data_parseGood() {
    return new Object[][] {
        {"AUD 100.001", Currency.AUD, 100.001d},
        {"AUD 321.123", Currency.AUD, 321.123d},
        {"AUD 123", Currency.AUD, 123d},
        {"GBP 0", Currency.GBP, 0d},
        {"USD -0", Currency.USD, -0d},
        {"EUR -0.01", Currency.EUR, -0.01d},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_String_good(String input, Currency currency, double amount) {
    assertEquals(CurrencyAmount.parse(input), CurrencyAmount.of(currency, amount));
  }

  @DataProvider(name = "parseBad")
  Object[][] data_parseBad() {
    return new Object[][] {
        {"AUD"},
        {"AUD aa"},
        {"AUD -.+-"},
        {"123"},
        {null},
    };
  }

  @Test(dataProvider = "parseBad")
  public void test_parse_String_bad(String input) {
    assertThrowsIllegalArg(() -> CurrencyAmount.parse(input));
  }

  //-------------------------------------------------------------------------
  public void test_plus_CurrencyAmount() {
    CurrencyAmount ccyAmount = CurrencyAmount.of(CCY1, AMT2);
    CurrencyAmount test = CCY_AMOUNT.plus(ccyAmount);
    assertEquals(test, CurrencyAmount.of(CCY1, AMT1 + AMT2));
  }

  public void test_plus_CurrencyAmount_null() {
    assertThrowsIllegalArg(() -> CCY_AMOUNT.plus(null));
  }

  public void test_plus_CurrencyAmount_wrongCurrency() {
    assertThrowsIllegalArg(() -> CCY_AMOUNT.plus(CurrencyAmount.of(CCY2, AMT2)));
  }

  public void test_plus_double() {
    CurrencyAmount test = CCY_AMOUNT.plus(AMT2);
    assertEquals(test, CurrencyAmount.of(CCY1, AMT1 + AMT2));
  }

  //-------------------------------------------------------------------------
  public void test_minus_CurrencyAmount() {
    CurrencyAmount ccyAmount = CurrencyAmount.of(CCY1, AMT2);
    CurrencyAmount test = CCY_AMOUNT.minus(ccyAmount);
    assertEquals(test, CurrencyAmount.of(CCY1, AMT1 - AMT2));
  }

  public void test_minus_CurrencyAmount_null() {
    assertThrowsIllegalArg(() -> CCY_AMOUNT.minus(null));
  }

  public void test_minus_CurrencyAmount_wrongCurrency() {
    assertThrowsIllegalArg(() -> CCY_AMOUNT.minus(CurrencyAmount.of(CCY2, AMT2)));
  }

  public void test_minus_double() {
    CurrencyAmount test = CCY_AMOUNT.minus(AMT2);
    assertEquals(test, CurrencyAmount.of(CCY1, AMT1 - AMT2));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    CurrencyAmount test = CCY_AMOUNT.multipliedBy(3.5);
    assertEquals(test, CurrencyAmount.of(CCY1, AMT1 * 3.5));
  }

  public void test_mapAmount() {
    CurrencyAmount test = CCY_AMOUNT.mapAmount(v -> v * 2 + 1);
    assertEquals(test, CurrencyAmount.of(CCY1, AMT1 * 2 + 1));
  }

  //-------------------------------------------------------------------------
  public void test_negated() {
    assertEquals(CCY_AMOUNT.negated(), CCY_AMOUNT_NEGATIVE);
    assertEquals(CCY_AMOUNT_NEGATIVE.negated(), CCY_AMOUNT);
    assertEquals(CurrencyAmount.zero(Currency.USD), CurrencyAmount.zero(Currency.USD).negated());
    assertEquals(CurrencyAmount.of(Currency.USD, -0d).negated(), CurrencyAmount.zero(Currency.USD));
  }

  public void test_negative() {
    assertEquals(CCY_AMOUNT.negative(), CCY_AMOUNT_NEGATIVE);
    assertEquals(CCY_AMOUNT_NEGATIVE.negative(), CCY_AMOUNT_NEGATIVE);
  }

  public void test_positive() {
    assertEquals(CCY_AMOUNT.positive(), CCY_AMOUNT);
    assertEquals(CCY_AMOUNT_NEGATIVE.positive(), CCY_AMOUNT);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo_explicitRate() {
    assertEquals(CCY_AMOUNT.convertedTo(CCY2, 2.5d), CurrencyAmount.of(CCY2, AMT1 * 2.5d));
    assertEquals(CCY_AMOUNT.convertedTo(CCY1, 1d), CCY_AMOUNT);
    assertThrowsIllegalArg(() -> CCY_AMOUNT.convertedTo(CCY1, 1.5d));
  }

  public void test_convertedTo_rateProvider() {
    FxRateProvider provider = (ccy1, ccy2) -> 2.5d;
    assertEquals(CCY_AMOUNT.convertedTo(CCY2, provider), CurrencyAmount.of(CCY2, AMT1 * 2.5d));
    assertEquals(CCY_AMOUNT.convertedTo(CCY1, provider), CCY_AMOUNT);
  }

  //-------------------------------------------------------------------------
  public void test_equals_hashCode() {
    CurrencyAmount other = CurrencyAmount.of(CCY1, AMT1);
    assertTrue(CCY_AMOUNT.equals(CCY_AMOUNT));
    assertTrue(CCY_AMOUNT.equals(other));
    assertTrue(other.equals(CCY_AMOUNT));
    assertEquals(CCY_AMOUNT.hashCode(), other.hashCode());
    other = CurrencyAmount.of(CCY1, AMT1);
    assertEquals(CCY_AMOUNT, other);
    assertEquals(CCY_AMOUNT.hashCode(), other.hashCode());
    other = CurrencyAmount.of(CCY2, AMT1);
    assertFalse(CCY_AMOUNT.equals(other));
    other = CurrencyAmount.of(CCY1, AMT2);
    assertFalse(CCY_AMOUNT.equals(other));
  }

  public void test_equals_bad() {
    assertFalse(CCY_AMOUNT.equals(""));
    assertFalse(CCY_AMOUNT.equals(null));
  }

  //-------------------------------------------------------------------------
  public void test_toString() {
    assertEquals(CurrencyAmount.of(Currency.AUD, 100d).toString(), "AUD 100");
    assertEquals(CurrencyAmount.of(Currency.AUD, 100.123d).toString(), "AUD 100.123");
  }

  //-----------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(CCY_AMOUNT);
  }

  public void test_jodaConvert() {
    assertJodaConvert(CurrencyAmount.class, CCY_AMOUNT);
  }

}
