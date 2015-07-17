/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

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
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndices;

/**
 * Test {@link FxDigitalOption}.
 */
@Test
public class FxDigitalOptionTest {
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2015, 2, 14);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(12, 15);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final LongShort LONG = LongShort.LONG;
  private static final PutCall CALL = PutCall.CALL;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, 1.3);
  private static final double NOTIONAL = 1.0e6;
  private static final FxIndex INDEX = FxIndices.ECB_EUR_USD;

  public void test_builder() {
    FxDigitalOption test = FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .index(INDEX)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(USD)
        .putCall(CALL)
        .strike(STRIKE)
        .build();
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryDateTime(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getIndex(), INDEX);
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getPaymentDate(), INDEX.calculateMaturityFromFixing(EXPIRY_DATE));
    assertEquals(test.getPayoffCurrency(), USD);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getStrikeBaseCurrency(), STRIKE.getPair().getBase());
    assertEquals(test.getStrikeCounterCurrency(), STRIKE.getPair().getCounter());
  }

  public void test_builder_withPaymentDate() {
    LocalDate paymentDate = LocalDate.of(2015, 2, 15);
    FxDigitalOption test = FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .paymentDate(paymentDate)
        .index(INDEX)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(USD)
        .putCall(CALL)
        .strike(STRIKE.inverse())
        .build();
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryDateTime(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getIndex(), INDEX);
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getPaymentDate(), paymentDate);
    assertEquals(test.getPayoffCurrency(), USD);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrike(), STRIKE.inverse());
    assertEquals(test.getStrikeBaseCurrency(), STRIKE.inverse().getPair().getBase());
    assertEquals(test.getStrikeCounterCurrency(), STRIKE.inverse().getPair().getCounter());
  }

  public void test_builder_wrongPaymentCurrency() {
    assertThrowsIllegalArg(() -> FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .index(INDEX)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(GBP)
        .putCall(CALL)
        .strike(STRIKE)
        .build());
  }

  public void test_builder_wrongStrikePair() {
    FxRate strike = FxRate.of(USD, GBP, 0.8);
    assertThrowsIllegalArg(() -> FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .index(INDEX)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(GBP)
        .putCall(CALL)
        .strike(strike)
        .build());
  }

  public void test_builder_earlyPaymentDate() {
    LocalDate paymentDate = LocalDate.of(2014, 2, 15);
    assertThrowsIllegalArg(() -> FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .paymentDate(paymentDate)
        .index(INDEX)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(USD)
        .putCall(CALL)
        .strike(STRIKE)
        .build());
  }

  public void test_builder_null() {
    assertThrowsIllegalArg(() -> FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(USD)
        .putCall(CALL)
        .strike(STRIKE)
        .build());
    assertThrowsIllegalArg(() -> FxDigitalOption.builder()
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .index(INDEX)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(USD)
        .putCall(CALL)
        .strike(STRIKE)
        .build());
  }

  public void test_expand() {
    FxDigitalOption base = FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .index(INDEX)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(USD)
        .putCall(CALL)
        .strike(STRIKE)
        .build();
    assertEquals(base.expand(), base);
  }

  public void coverage() {
    FxDigitalOption test1 = FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .index(INDEX)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(USD)
        .putCall(CALL)
        .strike(STRIKE)
        .build();
    coverImmutableBean(test1);
    FxDigitalOption test2 = FxDigitalOption.builder()
        .expiryDate(LocalDate.of(2015, 2, 15))
        .expiryTime(LocalTime.of(12, 45))
        .expiryZone(ZoneId.of("GMT"))
        .index(FxIndices.ECB_EUR_GBP)
        .longShort(LongShort.SHORT)
        .notional(1000)
        .payoffCurrency(EUR)
        .paymentDate(EXPIRY_DATE.plusDays(2))
        .putCall(PutCall.PUT)
        .strike(FxRate.of(GBP, EUR, 1.1))
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxDigitalOption test = FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .index(INDEX)
        .longShort(LONG)
        .notional(NOTIONAL)
        .payoffCurrency(USD)
        .putCall(CALL)
        .strike(STRIKE)
        .build();
    assertSerialization(test);
  }

}
