/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.Decimal;
import com.opengamma.strata.collect.FixedScaleDecimal;

/**
 * Test {@link Money}.
 */
public class MoneyTest {

  private static final Currency CCY_AUD = Currency.AUD;
  private static final Currency CCY_RON = Currency.RON;
  private static final Currency CCY_BHD = Currency.BHD; //3 decimals

  private static final double AMT_100_12 = 100.12;
  private static final double AMT_100_13 = 100.13;
  private static final double AMT_100_1249 = 100.1249;
  private static final double AMT_100_125 = 100.125;
  private static final double AMT_200_23 = 200.23;
  private static final CurrencyAmount CCYAMT = CurrencyAmount.of(CCY_RON, AMT_200_23);
  private static final Money MONEY_100_RON = Money.of(CCY_RON, BigDecimal.valueOf(100));
  private static final Money MONEY_200_23_RON = Money.of(CCYAMT);
  private static final Money MONEY_100_12_AUD = Money.of(CCY_AUD, AMT_100_12);
  private static final Money MONEY_200_AUD = Money.of(CCY_AUD, 200);
  private static final Money MONEY_200_23_RON_ALTERNATIVE = Money.of(CCY_RON, AMT_200_23);
  private static final Money MONEY_100_120_BHD = Money.of(CCY_BHD, AMT_100_12);
  private static final Money MONEY_100_125_BHD = Money.of(CCY_BHD, AMT_100_125);

  //-------------------------------------------------------------------------
  @Test
  @SuppressWarnings("deprecation")
  public void testOfCurrencyAndAmount() throws Exception {
    assertThat(MONEY_200_AUD.getCurrency()).isEqualTo(CCY_AUD);
    assertThat(MONEY_200_AUD.getValue()).isEqualTo(FixedScaleDecimal.of(Decimal.of(200), 2));
    assertThat(MONEY_200_AUD.getAmount()).isEqualTo(BigDecimal.valueOf(20000, 2));
    assertThat(MONEY_100_12_AUD.getCurrency()).isEqualTo(CCY_AUD);
    assertThat(MONEY_100_12_AUD.getValue()).isEqualTo(FixedScaleDecimal.of(Decimal.of(AMT_100_12), 2));
    assertThat(MONEY_100_12_AUD.getAmount()).isEqualTo(BigDecimal.valueOf(10012, 2));
    assertThat(MONEY_100_120_BHD.getCurrency()).isEqualTo(CCY_BHD);
    assertThat(MONEY_100_120_BHD.getValue()).isEqualTo(FixedScaleDecimal.of(Decimal.of(AMT_100_12), 3));
    assertThat(MONEY_100_120_BHD.getAmount()).isEqualTo(BigDecimal.valueOf(100120, 3));
    assertThat(MONEY_100_125_BHD.getCurrency()).isEqualTo(CCY_BHD);
    assertThat(MONEY_100_125_BHD.getValue()).isEqualTo(FixedScaleDecimal.of(Decimal.of(100.125), 3)); //Testing the rounding from 4 to 3 decimals
    assertThat(MONEY_100_125_BHD.getAmount()).isEqualTo(BigDecimal.valueOf(100125, 3));

    // test rounding
    assertThat(Money.of(CCY_AUD, AMT_100_1249).getValue()).isEqualTo(FixedScaleDecimal.of(Decimal.of(AMT_100_12), 2));
    assertThat(Money.of(CCY_AUD, AMT_100_125).getValue()).isEqualTo(FixedScaleDecimal.of(Decimal.of(AMT_100_13), 2));
    assertThat(Money.of(CCY_BHD, AMT_100_1249).getValue()).isEqualTo(FixedScaleDecimal.of(Decimal.of(AMT_100_125), 3));
    assertThat(Money.of(CCY_BHD, AMT_100_125).getValue()).isEqualTo(FixedScaleDecimal.of(Decimal.of(AMT_100_125), 3));
  }

  @Test
  public void testOfCurrencyAmount() throws Exception {
    assertThat(MONEY_200_23_RON.getCurrency()).isEqualTo(CCY_RON);
    assertThat(MONEY_200_23_RON.getValue()).isEqualTo(FixedScaleDecimal.of(Decimal.of(AMT_200_23), 2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testPlus() throws Exception {
    Money a = Money.of(GBP, 1.23);
    Money b = Money.of(GBP, 2.34);
    assertThat(a.plus(b)).isEqualTo(Money.of(GBP, 3.57));
    assertThat(b.plus(a)).isEqualTo(Money.of(GBP, 3.57));
  }

  @Test
  public void testMinus() throws Exception {
    Money a = Money.of(GBP, 1.23);
    Money b = Money.of(GBP, 0.34);
    assertThat(a.minus(b)).isEqualTo(Money.of(GBP, 0.89));
    assertThat(b.minus(a)).isEqualTo(Money.of(GBP, -0.89));
  }

  @Test
  public void testMultipliedBy() throws Exception {
    Money a = Money.of(GBP, 1.23);
    assertThat(a.multipliedBy(2)).isEqualTo(Money.of(GBP, 2.46));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testMapAmount() throws Exception {
    Money a = Money.of(GBP, 1.23);
    assertThat(a.map(amount -> amount.multipliedBy(Decimal.of(10)))).isEqualTo(Money.of(GBP, 12.3));
    assertThat(a.mapAmount(amount -> amount.multiply(BigDecimal.TEN))).isEqualTo(Money.of(GBP, 12.3));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testZeroPositiveNegative() throws Exception {
    Money zero = Money.zero(GBP);
    Money positive = Money.of(GBP, 200.23);
    Money negative = Money.of(GBP, -200.23);

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
    Money base = Money.of(GBP, 200.23);
    assertThat(base.toCurrencyAmount()).isEqualTo(CurrencyAmount.of(GBP, 200.23));
    assertThat(base.toCurrencyAmount().toMoney()).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void testConvertedToWithExplicitRate() throws Exception {
    assertThat(Money.of(Currency.RON, 200.23))
        .isEqualTo(MONEY_200_23_RON.convertedTo(CCY_RON, Decimal.of(1)));
    assertThat(Money.of(Currency.RON, 260.31))
        .isEqualTo(MONEY_100_12_AUD.convertedTo(CCY_RON, Decimal.of(2.6d)));
    assertThat(Money.of(Currency.RON, 260.31))
        .isEqualTo(MONEY_100_12_AUD.convertedTo(CCY_RON, Decimal.of(2.6d).toBigDecimal()));
  }

  @Test
  public void testConvertedToWithExplicitRateForSameCurrency() throws Exception {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> MONEY_200_23_RON.convertedTo(CCY_RON, Decimal.of(1.1)))
        .withMessage("FX rate must be 1 when no conversion required");
  }

  @Test
  public void testConvertedToWithRateProvider() throws Exception {
    FxRateProvider provider = (ccy1, ccy2) -> 2.5d;
    assertThat(Money.of(Currency.RON, 250.30)).isEqualTo(MONEY_100_12_AUD.convertedTo(CCY_RON, provider));
    assertThat(Money.of(Currency.RON, 200.23)).isEqualTo(MONEY_200_23_RON.convertedTo(CCY_RON, provider));
  }

  @Test
  public void testCompareTo() throws Exception {
    assertThat(-1).isEqualTo(MONEY_100_12_AUD.compareTo(MONEY_200_23_RON));
    assertThat(0).isEqualTo(MONEY_200_23_RON.compareTo(MONEY_200_23_RON_ALTERNATIVE));
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    assertThat(MONEY_200_23_RON)
        .isEqualTo(MONEY_200_23_RON)
        .isEqualTo(MONEY_200_23_RON_ALTERNATIVE)
        .isNotEqualTo(MONEY_100_12_AUD)
        .isNotEqualTo(MONEY_200_AUD)
        .isNotEqualTo(MONEY_100_RON)
        .isNotEqualTo(MONEY_100_120_BHD)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(MONEY_200_23_RON_ALTERNATIVE);
  }

  @Test
  public void testToString() throws Exception {
    assertThat(MONEY_200_AUD).hasToString("AUD 200.00");
    assertThat(MONEY_200_23_RON).hasToString("RON 200.23");
    assertThat(MONEY_100_125_BHD).hasToString("BHD 100.125");
  }

  @Test
  public void testParse() throws Exception {
    assertThat(Money.parse("RON 200.23")).isEqualTo(MONEY_200_23_RON);
    assertThat(Money.parse("RON 200.2345")).isEqualTo(MONEY_200_23_RON);
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
