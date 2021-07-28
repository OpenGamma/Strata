/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Test {@link BigMoney}.
 */
public class BigMoneyTest {

  private static final Currency CCY_AUD = Currency.AUD;
  private static final double AMT_100 = 100;
  private static final double AMT_100_12 = 100.12;
  private static final double AMT_100_1249 = 100.1249;
  private static final Currency CCY_RON = Currency.RON;
  private static final double AMT_200_2345 = 200.2345;
  private static final Currency CCY_BHD = Currency.BHD; //3 decimals
  private static final CurrencyAmount CCYAMT = CurrencyAmount.of(CCY_RON, AMT_200_2345);
  private static final BigMoney MONEY_100_RON = BigMoney.of(CCY_RON, 100);
  private static final BigMoney MONEY_200_2345_RON = BigMoney.of(CCYAMT);
  private static final BigMoney MONEY_100_AUD = BigMoney.of(CCY_AUD, AMT_100);
  private static final BigMoney MONEY_100_1249_AUD = BigMoney.of(CCY_AUD, AMT_100_1249);
  private static final BigMoney MONEY_200_AUD = BigMoney.of(CCY_AUD, 200);
  private static final BigMoney MONEY_200_RON_ALTERNATIVE = BigMoney.of(CCY_RON, AMT_200_2345);
  private static final BigMoney MONEY_100_12_BHD = BigMoney.of(CCY_BHD, AMT_100_12);
  private static final BigMoney MONEY_100_1249_BHD = BigMoney.of(CCY_BHD, AMT_100_1249);

  //-------------------------------------------------------------------------
  @Test
  public void testOfCurrencyAndAmount() throws Exception {
    assertThat(MONEY_100_AUD.getCurrency()).isEqualTo(CCY_AUD);
    assertThat(MONEY_100_AUD.getAmount()).isEqualTo(new BigDecimal("100.00"));
    assertThat(MONEY_100_1249_AUD.getCurrency()).isEqualTo(CCY_AUD);
    assertThat(MONEY_100_1249_AUD.getAmount()).isEqualTo(new BigDecimal("100.1249"));
    assertThat(MONEY_100_12_BHD.getCurrency()).isEqualTo(CCY_BHD);
    assertThat(MONEY_100_12_BHD.getAmount()).isEqualTo(new BigDecimal("100.120"));
    assertThat(MONEY_100_1249_BHD.getCurrency()).isEqualTo(CCY_BHD);
    assertThat(MONEY_100_1249_BHD.getAmount()).isEqualTo(new BigDecimal("100.1249"));

  }

  @Test
  public void testOfCurrencyAmount() throws Exception {
    assertThat(MONEY_200_2345_RON.getCurrency()).isEqualTo(CCY_RON);
    assertThat(MONEY_200_2345_RON.getAmount()).isEqualTo(BigDecimal.valueOf(AMT_200_2345));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testPlus() throws Exception {
    BigMoney a = BigMoney.of(GBP, 1.23);
    BigMoney b = BigMoney.of(GBP, 2.34);
    assertThat(a.plus(b)).isEqualTo(BigMoney.of(GBP, 3.57));
    assertThat(b.plus(a)).isEqualTo(BigMoney.of(GBP, 3.57));
  }

  @Test
  public void testMinus() throws Exception {
    BigMoney a = BigMoney.of(GBP, 1.23);
    BigMoney b = BigMoney.of(GBP, 0.34);
    assertThat(a.minus(b)).isEqualTo(BigMoney.of(GBP, 0.89));
    assertThat(b.minus(a)).isEqualTo(BigMoney.of(GBP, -0.89));
  }

  @Test
  public void testMultipliedBy() throws Exception {
    BigMoney a = BigMoney.of(GBP, 1.23);
    assertThat(a.multipliedBy(2)).isEqualTo(BigMoney.of(GBP, 2.46));
  }

  @Test
  public void testMapAmount() throws Exception {
    BigMoney a = BigMoney.of(GBP, 1.23);
    assertThat(a.mapAmount(amount -> amount.multiply(BigDecimal.TEN)))
        .isEqualTo(BigMoney.of(GBP, new BigDecimal("12.30")));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testZeroPositiveNegative() throws Exception {
    BigMoney zero = BigMoney.zero(GBP);
    BigMoney positive = BigMoney.of(GBP, 200.23);
    BigMoney negative = BigMoney.of(GBP, -200.23);

    assertThat(zero.isZero()).isTrue();
    assertThat(zero.isPositive()).isFalse();
    assertThat(zero.isNegative()).isFalse();
    assertThat(zero.negated()).isEqualTo(zero);
    assertThat(zero.positive()).isEqualTo(zero);
    assertThat(zero.negated()).isEqualTo(zero);

    assertThat(positive.isZero()).isFalse();
    assertThat(positive.isPositive()).isTrue();
    assertThat(positive.isNegative()).isFalse();
    assertThat(positive.negated()).isEqualTo(negative);
    assertThat(positive.positive()).isEqualTo(positive);
    assertThat(positive.negative()).isEqualTo(negative);

    assertThat(negative.isZero()).isFalse();
    assertThat(negative.isPositive()).isFalse();
    assertThat(negative.isNegative()).isTrue();
    assertThat(negative.negated()).isEqualTo(positive);
    assertThat(negative.positive()).isEqualTo(positive);
    assertThat(negative.negative()).isEqualTo(negative);
  }

  //-------------------------------------------------------------------------
  @Test
  public void testToCurrencyAmount() throws Exception {
    BigMoney base = BigMoney.of(GBP, 200.23);
    assertThat(base.toCurrencyAmount()).isEqualTo(CurrencyAmount.of(GBP, 200.23));
    assertThat(base.toCurrencyAmount().toBigMoney()).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void testConvertedToWithExplicitRate() throws Exception {
    assertThat(MONEY_200_2345_RON.convertedTo(CCY_RON, BigDecimal.valueOf(1)))
        .isEqualTo(BigMoney.of(Currency.RON, 200.2345));
    assertThat(MONEY_100_AUD.convertedTo(CCY_RON, new BigDecimal("2.6031")))
        .isEqualTo(BigMoney.of(Currency.RON, 260.31));
  }

  @Test
  public void testConvertedToWithExplicitRateForSameCurrency() throws Exception {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> MONEY_200_2345_RON.convertedTo(CCY_RON, BigDecimal.valueOf(1.1)))
        .withMessage("FX rate must be 1 when no conversion required");
  }

  @Test
  public void testConvertedToWithRateProvider() throws Exception {
    FxRateProvider provider = (ccy1, ccy2) -> 2.5d;
    assertThat(MONEY_100_AUD.convertedTo(CCY_RON, provider)).isEqualTo(BigMoney.of(Currency.RON, 250.00));
    assertThat(MONEY_200_2345_RON.convertedTo(CCY_RON, provider)).isEqualTo(BigMoney.of(Currency.RON, 200.2345));
  }

  @Test
  public void testTo() throws Exception {
    assertThat(MONEY_100_AUD.toMoney().toBigMoney()).isEqualTo(MONEY_100_AUD);
  }

  @Test
  public void testCompareTo() throws Exception {
    assertThat(MONEY_100_AUD.compareTo(MONEY_200_2345_RON)).isEqualTo(-1);
    assertThat(MONEY_200_2345_RON.compareTo(MONEY_200_RON_ALTERNATIVE)).isEqualTo(0);
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    assertThat(MONEY_200_2345_RON)
        .isEqualTo(MONEY_200_2345_RON)
        .isEqualTo(MONEY_200_RON_ALTERNATIVE)
        .isNotEqualTo(MONEY_100_AUD)
        .isNotEqualTo(MONEY_200_AUD)
        .isNotEqualTo(MONEY_100_RON)
        .isNotEqualTo(MONEY_100_12_BHD)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(MONEY_200_RON_ALTERNATIVE);
  }

  @Test
  public void testToString() throws Exception {
    assertThat(MONEY_200_2345_RON.toString()).isEqualTo("RON 200.2345");
  }

  @Test
  public void testParse() throws Exception {
    assertThat(BigMoney.parse("RON 200.2345")).isEqualTo(MONEY_200_2345_RON);
    assertThat(BigMoney.parse("AUD 1.123456789012345")).isEqualTo(BigMoney.parse("AUD 1.123456789012"));
  }

  @Test
  public void testParseWrongFormat() throws Exception {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BigMoney.parse("200.23 RON"))
        .withMessage("Unable to parse amount: 200.23 RON");
  }

  @Test
  public void testParseWrongElementsNumber() throws Exception {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BigMoney.parse("$100"))
        .withMessageMatching("Unable to parse amount, invalid format: [$]100");
  }

}
