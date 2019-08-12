/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

/**
 * Test {@link Money}.
 */
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
    assertThat(MONEY_100_AUD.getCurrency()).isEqualTo(CCY_AUD);
    assertThat(MONEY_100_AUD.getAmount()).isEqualTo(new BigDecimal(AMT_100_12).setScale(2, RoundingMode.HALF_UP));
    assertThat(MONEY_100_13_AUD.getCurrency()).isEqualTo(CCY_AUD);
    assertThat(MONEY_100_13_AUD.getAmount()).isEqualTo(BigDecimal.valueOf(AMT_100_12).setScale(2, RoundingMode.HALF_UP)); //Testing the rounding from 3 to 2 decimals
    assertThat(MONEY_100_12_BHD.getCurrency()).isEqualTo(CCY_BHD);
    assertThat(MONEY_100_12_BHD.getAmount()).isEqualTo(BigDecimal.valueOf(AMT_100_12).setScale(3, RoundingMode.HALF_UP));
    assertThat(MONEY_100_125_BHD.getCurrency()).isEqualTo(CCY_BHD);
    assertThat(MONEY_100_125_BHD.getAmount()).isEqualTo(BigDecimal.valueOf(100.125)); //Testing the rounding from 4 to 3 decimals

  }

  @Test
  public void testOfCurrencyAmount() throws Exception {
    assertThat(MONEY_200_RON.getCurrency()).isEqualTo(CCY_RON);
    assertThat(MONEY_200_RON.getAmount()).isEqualTo(new BigDecimal(AMT_200_23).setScale(2, RoundingMode.HALF_EVEN));
  }

  @Test
  public void testConvertedToWithExplicitRate() throws Exception {
    assertThat(Money.of(Currency.RON, 200.23)).isEqualTo(MONEY_200_RON.convertedTo(CCY_RON, BigDecimal.valueOf(1)));
    assertThat(Money.of(Currency.RON, 260.31)).isEqualTo(MONEY_100_AUD.convertedTo(CCY_RON, BigDecimal.valueOf(2.6d)));
  }

  @Test
  public void testConvertedToWithExplicitRateForSameCurrency() throws Exception {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> MONEY_200_RON.convertedTo(CCY_RON, BigDecimal.valueOf(1.1)))
        .withMessage("FX rate must be 1 when no conversion required");
  }

  @Test
  public void testConvertedToWithRateProvider() throws Exception {
    FxRateProvider provider = (ccy1, ccy2) -> 2.5d;
    assertThat(Money.of(Currency.RON, 250.30)).isEqualTo(MONEY_100_AUD.convertedTo(CCY_RON, provider));
    assertThat(Money.of(Currency.RON, 200.23)).isEqualTo(MONEY_200_RON.convertedTo(CCY_RON, provider));
  }

  @Test
  public void testCompareTo() throws Exception {
    assertThat(-1).isEqualTo(MONEY_100_AUD.compareTo(MONEY_200_RON));
    assertThat(0).isEqualTo(MONEY_200_RON.compareTo(MONEY_200_RON_ALTERNATIVE));
  }

  @Test
  public void testEquals() throws Exception {
    assertThat(MONEY_200_RON.equals(MONEY_200_RON)).isTrue();
    assertThat(MONEY_200_RON.equals(null)).isFalse();
    assertThat(MONEY_200_RON.equals(MONEY_200_RON_ALTERNATIVE)).isTrue();
    assertThat(MONEY_100_AUD.equals(MONEY_200_RON)).isFalse();
  }

  @Test
  public void testHashCode() throws Exception {
    assertThat(MONEY_200_RON.hashCode() == MONEY_200_RON_ALTERNATIVE.hashCode()).isTrue();
    assertThat(MONEY_200_RON.hashCode() == MONEY_100_AUD.hashCode()).isFalse();
  }

  @Test
  public void testToString() throws Exception {
    assertThat("RON 200.23").isEqualTo(MONEY_200_RON.toString());
  }

  @Test
  public void testParse() throws Exception {
    assertThat(Money.parse("RON 200.23")).isEqualTo(MONEY_200_RON);
    assertThat(Money.parse("RON 200.2345")).isEqualTo(MONEY_200_RON);
  }

  @Test
  public void testParseWrongFormat() throws Exception {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Money.parse("200.23 RON"))
        .withMessage("Unable to parse amount: 200.23 RON");
  }

  @Test
  public void testParseWrongElementsNumber() throws Exception {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Money.parse("$100"))
        .withMessageMatching("Unable to parse amount, invalid format: [$]100");
  }

}
