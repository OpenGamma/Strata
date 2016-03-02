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
import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Test {@link FxVanillaOption}.
 */
@Test
public class FxVanillaOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
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

  //-------------------------------------------------------------------------
  public void test_builder() {
    FxVanillaOption test = sut();
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiry(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
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
        .putCall(CALL)
        .longShort(LONG)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .strike(strike)
        .underlying(FX)
        .build();
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiry(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
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
        .putCall(CALL)
        .longShort(LONG)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .strike(STRIKE)
        .underlying(fxProduct)
        .build();
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiry(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
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
        .putCall(CALL)
        .longShort(LONG)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .strike(strike)
        .underlying(FX)
        .build());
  }

  public void test_builder_earlyPaymentDate() {
    assertThrowsIllegalArg(() -> FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiryDate(LocalDate.of(2015, 2, 21))
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .strike(STRIKE)
        .underlying(FX)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxVanillaOption base = sut();
    ResolvedFxVanillaOption expected = ResolvedFxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiry(EXPIRY_DATE.atTime(EXPIRY_TIME).atZone(EXPIRY_ZONE))
        .strike(STRIKE)
        .underlying(FX.resolve(REF_DATA))
        .build();
    assertEquals(base.resolve(REF_DATA), expected);
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
  static FxVanillaOption sut() {
    return FxVanillaOption.builder()
        .putCall(CALL)
        .longShort(LONG)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .strike(STRIKE)
        .underlying(FX)
        .build();
  }

  static FxVanillaOption sut2() {
    FxSingle fxProduct =
        FxSingle.of(CurrencyAmount.of(EUR, NOTIONAL), CurrencyAmount.of(GBP, -NOTIONAL * 0.9), PAYMENT_DATE);
    return FxVanillaOption.builder()
        .putCall(PutCall.PUT)
        .longShort(LongShort.SHORT)
        .expiryDate(LocalDate.of(2015, 2, 15))
        .expiryTime(LocalTime.of(12, 45))
        .expiryZone(ZoneId.of("GMT"))
        .strike(FxRate.of(EUR, GBP, 0.9))
        .underlying(fxProduct)
        .build();
  }

}
