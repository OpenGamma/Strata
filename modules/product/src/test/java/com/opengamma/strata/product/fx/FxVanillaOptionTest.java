/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;

/**
 * Test {@link FxVanillaOption}.
 */
@Test
public class FxVanillaOptionTest {

  private static final LocalDate EXPIRY_DATE = LocalDate.of(2015, 2, 14);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(12, 15);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final LongShort LONG = LongShort.LONG;
  private static final PutCall CALL = PutCall.CALL;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, 1.3);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * 1.35);
  private static final FxSingle FX = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);

  public void test_builder() {
    FxVanillaOption test = FxVanillaOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .longShort(LONG)
        .putCall(CALL)
        .strike(STRIKE)
        .underlying(FX)
        .build();
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryDateTime(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getPayoffCurrency(), USD);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getUnderlying(), FX);
  }

  public void test_builder_inverseStrike() {
    FxRate strike = FxRate.of(USD, EUR, 1.0 / 1.3);
    FxVanillaOption test = FxVanillaOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .longShort(LONG)
        .putCall(CALL)
        .strike(strike)
        .underlying(FX)
        .build();
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryDateTime(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getPayoffCurrency(), USD);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getUnderlying(), FX);
  }

  public void test_of_inverseFx() {
    CurrencyAmount eurAmount = CurrencyAmount.of(EUR, -NOTIONAL);
    CurrencyAmount usdAmount = CurrencyAmount.of(USD, NOTIONAL * 1.35);
    FxSingle fxProduct = FxSingle.of(eurAmount, usdAmount, PAYMENT_DATE);
    FxVanillaOption test = FxVanillaOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .longShort(LONG)
        .putCall(CALL)
        .strike(STRIKE)
        .underlying(fxProduct)
        .build();
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryDateTime(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getPayoffCurrency(), EUR);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrike(), STRIKE.inverse());
    assertEquals(test.getUnderlying(), fxProduct);
  }

  public void test_builder_wrongCurrency() {
    FxRate strike = FxRate.of(USD, GBP, 0.8);
    assertThrowsIllegalArg(() -> FxVanillaOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .longShort(LONG)
        .putCall(CALL)
        .strike(strike)
        .underlying(FX)
        .build());
  }

  public void test_builder_earlyPaymentDate() {
    assertThrowsIllegalArg(() -> FxVanillaOption.builder()
        .expiryDate(LocalDate.of(2015, 2, 21))
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .longShort(LONG)
        .putCall(CALL)
        .strike(STRIKE)
        .underlying(FX)
        .build());
  }

  public void test_expand() {
    FxVanillaOption base = FxVanillaOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .longShort(LONG)
        .putCall(CALL)
        .strike(STRIKE)
        .underlying(FX)
        .build();
    assertEquals(base.expand(), base);
  }

  public void coverage() {
    FxVanillaOption test1 = FxVanillaOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .longShort(LONG)
        .putCall(CALL)
        .strike(STRIKE)
        .underlying(FX)
        .build();
    coverImmutableBean(test1);
    FxSingle fxProduct = FxSingle.of(CurrencyAmount.of(EUR, NOTIONAL), CurrencyAmount.of(GBP, -NOTIONAL * 0.9), PAYMENT_DATE);
    FxVanillaOption test2 = FxVanillaOption.builder()
        .expiryDate(LocalDate.of(2015, 2, 15))
        .expiryTime(LocalTime.of(12, 45))
        .expiryZone(ZoneId.of("GMT"))
        .longShort(LongShort.SHORT)
        .putCall(PutCall.PUT)
        .strike(FxRate.of(EUR, GBP, 0.9))
        .underlying(fxProduct)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxVanillaOption test = FxVanillaOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .longShort(LONG)
        .putCall(CALL)
        .strike(STRIKE)
        .underlying(FX)
        .build();
    assertSerialization(test);
  }

}
