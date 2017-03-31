/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.testng.annotations.Test;

public class MoneyTest {

  private static final Currency CCY_AUD = Currency.AUD;
  private static final double AMT_100_12 = 100.12;
  private static final double AMT_100_MORE_DECIMALS = 100.1249;
  private static final Currency CCY_RON = Currency.RON;
  private static final double AMT_200_23 = 200.23;
  private static final Currency CCY_BHD = Currency.BHD; //3 decimals
  private static final CurrencyAmount CCYAMT = CurrencyAmount.of(CCY_RON, AMT_200_23);
  //Not the Money instances
  private static final Money MONEY_200_RON = Money.of(CCYAMT);
  private static final Money MONEY_100_AUD = Money.of(CCY_AUD, AMT_100_12);
  private static final Money MONEY_100_13_AUD = Money.of(CCY_AUD, AMT_100_MORE_DECIMALS);
  private static final Money MONEY_200_RON_ALTERNATIVE = Money.of(CCY_RON, AMT_200_23);
  private static final Money MONEY_100_12_BHD = Money.of(CCY_BHD, AMT_100_12);
  private static final Money MONEY_100_125_BHD = Money.of(CCY_BHD, AMT_100_MORE_DECIMALS);

  @Test
  public void testOfCurrencyAndAmount() throws Exception {
    assertEquals(MONEY_100_AUD.getCurrency(), CCY_AUD);
    assertEquals(MONEY_100_AUD.getAmount(), new BigDecimal(AMT_100_12).setScale(2, BigDecimal.ROUND_HALF_UP));
    assertEquals(MONEY_100_13_AUD.getCurrency(), CCY_AUD);
    assertEquals(MONEY_100_13_AUD.getAmount(), BigDecimal.valueOf(AMT_100_12).setScale(2, BigDecimal.ROUND_HALF_UP)); //Testing the rounding from 3 to 2 decimals
    assertEquals(MONEY_100_12_BHD.getCurrency(), CCY_BHD);
    assertEquals(MONEY_100_12_BHD.getAmount(), BigDecimal.valueOf(AMT_100_12).setScale(3, BigDecimal.ROUND_HALF_UP));
    assertEquals(MONEY_100_125_BHD.getCurrency(), CCY_BHD);
    assertEquals(MONEY_100_125_BHD.getAmount(), BigDecimal.valueOf(100.125)); //Testing the rounding from 4 to 3 decimals

  }

  @Test
  public void testOfCurrencyAmount() throws Exception {
    assertEquals(MONEY_200_RON.getCurrency(), CCY_RON);
    assertEquals(MONEY_200_RON.getAmount(), new BigDecimal(AMT_200_23).setScale(2, RoundingMode.HALF_EVEN));
  }

  @Test
  public void testConvertedToWithExplicitRate() throws Exception {
    assertEquals(Money.of(Currency.RON, 200.23), MONEY_200_RON.convertedTo(CCY_RON, BigDecimal.valueOf(1)));
    assertEquals(Money.of(Currency.RON, 260.31), MONEY_100_AUD.convertedTo(CCY_RON, BigDecimal.valueOf(2.6d)));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "FX rate must be 1 when no conversion required")
  public void testConvertedToWithExplicitRateForSameCurrency() throws Exception {
    assertEquals(Money.of(Currency.RON, 200.23), MONEY_200_RON.convertedTo(CCY_RON, BigDecimal.valueOf(1.1)));
  }

  @Test
  public void testConvertedToWithRateProvider() throws Exception {
    FxRateProvider provider = (ccy1, ccy2) -> 2.5d;
    assertEquals(Money.of(Currency.RON, 250.30), MONEY_100_AUD.convertedTo(CCY_RON, provider));
    assertEquals(Money.of(Currency.RON, 200.23), MONEY_200_RON.convertedTo(CCY_RON, provider));
  }

  @Test
  public void testCompareTo() throws Exception {
    assertEquals(-1, MONEY_100_AUD.compareTo(MONEY_200_RON));
    assertEquals(0, MONEY_200_RON.compareTo(MONEY_200_RON_ALTERNATIVE));
  }

  @Test
  public void testEquals() throws Exception {
    assertTrue(MONEY_200_RON.equals(MONEY_200_RON));
    assertFalse(MONEY_200_RON.equals(null));
    assertTrue(MONEY_200_RON.equals(MONEY_200_RON_ALTERNATIVE));
    assertFalse(MONEY_100_AUD.equals(MONEY_200_RON));
  }

  @Test
  public void testHashCode() throws Exception {
    assertTrue(MONEY_200_RON.hashCode() == MONEY_200_RON_ALTERNATIVE.hashCode());
    assertFalse(MONEY_200_RON.hashCode() == MONEY_100_AUD.hashCode());
  }

  @Test
  public void testToString() throws Exception {
    assertEquals("RON 200.23", MONEY_200_RON.toString());
  }

  @Test
  public void testParse() throws Exception {
    assertEquals(Money.parse("RON 200.23"), MONEY_200_RON);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Unable to parse amount: 200.23 RON")
  public void testParseWrongFormat() throws Exception {
    Money.parse("200.23 RON");
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Unable to parse amount, invalid format: [$]100")
  public void testParseWrongElementsNumber() throws Exception {
    Money.parse("$100");
  }

}
