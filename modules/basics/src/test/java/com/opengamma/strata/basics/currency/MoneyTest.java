package com.opengamma.strata.basics.currency;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class MoneyTest {
  private static final Currency CCY_AUD = Currency.AUD;
  private static final double AMT_100 = 100.12;
  private static final Currency CCY_RON = Currency.RON;
  private static final double AMT_200 = 200.23;
  private static final CurrencyAmount CCYAMT = CurrencyAmount.of(CCY_RON, AMT_200);
  private static final Money MONEY_200_RON = Money.of(CCYAMT);
  private static final Money MONEY_100_AUD = Money.of(CCY_AUD, AMT_100);
  private static final Money MONEY_200_RON_ALTERNATIVE = Money.of(CCY_RON, AMT_200);

  @Test
  public void testOfCurrencyAndAmount() throws Exception {
    assertEquals(MONEY_100_AUD.getCurrency(), CCY_AUD);
    assertEquals(MONEY_100_AUD.getAmountAsDouble(), AMT_100);
  }

  @Test
  public void testOfCurrencyAmount() throws Exception {
    assertEquals(MONEY_200_RON.getCurrency(), CCY_RON);
    assertEquals(MONEY_200_RON.getAmountAsDouble(), AMT_200);
  }

  @Test
  public void testConvertedToWithExplicitRate() throws Exception {
    assertEquals(Money.of(Currency.RON, 260), MONEY_100_AUD.convertedTo(CCY_RON, 2.6d));
  }

  @Test
  public void testConvertedToWithRateProvider() throws Exception {
    FxRateProvider provider = (ccy1, ccy2) -> 2.5d;
    assertEquals(Money.of(Currency.RON, 250), MONEY_100_AUD.convertedTo(CCY_RON, provider));
  }

  @Test
  public void testCompareTo() throws Exception {
    assertEquals(-1, MONEY_100_AUD.compareTo(MONEY_200_RON));
    assertEquals(0, MONEY_200_RON.compareTo(MONEY_200_RON_ALTERNATIVE));
  }

  @Test
  public void testEquals() throws Exception {
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
    assertEquals("RON 200.00", MONEY_200_RON.toString());
  }

}