/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;

/**
 * Test {@link ResolvedFxVanillaOption}.
 */
@Test
public class ResolvedFxVanillaOptionTest {

  private static final ZonedDateTime EXPIRY_DATE_TIME = ZonedDateTime.of(2015, 2, 14, 12, 15, 0, 0, ZoneOffset.UTC);
  private static final LongShort LONG = LongShort.LONG;
  private static final PutCall CALL = PutCall.CALL;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, 1.3);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * 1.35);
  private static final ResolvedFxSingle FX = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxVanillaOption test = sut();
    assertEquals(test.getExpiry(), EXPIRY_DATE_TIME);
    assertEquals(test.getExpiryDate(), EXPIRY_DATE_TIME.toLocalDate());
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getPayoffCurrency(), USD);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getUnderlying(), FX);
  }

  public void test_builder_inverseStrike() {
    FxRate strike = FxRate.of(USD, EUR, 1.0 / 1.3);
    ResolvedFxVanillaOption test = ResolvedFxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiry(EXPIRY_DATE_TIME)
        .strike(strike)
        .underlying(FX)
        .build();
    assertEquals(test.getExpiry(), EXPIRY_DATE_TIME);
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getPayoffCurrency(), USD);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getUnderlying(), FX);
  }

  public void test_of_inverseFx() {
    CurrencyAmount eurAmount = CurrencyAmount.of(EUR, -NOTIONAL);
    CurrencyAmount usdAmount = CurrencyAmount.of(USD, NOTIONAL * 1.35);
    ResolvedFxSingle fxProduct = ResolvedFxSingle.of(eurAmount, usdAmount, PAYMENT_DATE);
    ResolvedFxVanillaOption test = ResolvedFxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiry(EXPIRY_DATE_TIME)
        .strike(STRIKE)
        .underlying(fxProduct)
        .build();
    assertEquals(test.getExpiry(), EXPIRY_DATE_TIME);
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getPayoffCurrency(), EUR);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrike(), STRIKE.inverse());
    assertEquals(test.getUnderlying(), fxProduct);
  }

  public void test_builder_wrongCurrency() {
    FxRate strike = FxRate.of(USD, GBP, 0.8);
    assertThrowsIllegalArg(() -> ResolvedFxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiry(EXPIRY_DATE_TIME)
        .strike(strike)
        .underlying(FX)
        .build());
  }

  public void test_builder_earlyPaymentDate() {
    assertThrowsIllegalArg(() -> ResolvedFxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiry(LocalDate.of(2015, 2, 21).atStartOfDay(ZoneOffset.UTC))
        .strike(STRIKE)
        .underlying(FX)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedFxVanillaOption sut() {
    return ResolvedFxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiry(EXPIRY_DATE_TIME)
        .strike(STRIKE)
        .underlying(FX)
        .build();
  }

  static ResolvedFxVanillaOption sut2() {
    ResolvedFxSingle fxProduct = ResolvedFxSingle.of(
        CurrencyAmount.of(EUR, NOTIONAL), CurrencyAmount.of(GBP, -NOTIONAL * 0.9), PAYMENT_DATE);
    return ResolvedFxVanillaOption.builder()
        .putCall(PutCall.PUT)
        .longShort(LongShort.SHORT)
        .expiry(EXPIRY_DATE_TIME.plusSeconds(1))
        .strike(FxRate.of(EUR, GBP, 0.9))
        .underlying(fxProduct)
        .build();
  }

}
