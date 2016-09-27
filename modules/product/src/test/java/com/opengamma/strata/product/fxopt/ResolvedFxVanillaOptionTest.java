/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.fx.ResolvedFxSingle;

/**
 * Test {@link ResolvedFxVanillaOption}.
 */
@Test
public class ResolvedFxVanillaOptionTest {

  private static final ZonedDateTime EXPIRY_DATE_TIME = ZonedDateTime.of(2015, 2, 14, 12, 15, 0, 0, ZoneOffset.UTC);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final double STRIKE = 1.35;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * STRIKE);
  private static final ResolvedFxSingle FX = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);
  private static final double STRIKE_RE = 0.9;
  private static final ResolvedFxSingle FX_RE = ResolvedFxSingle.of(
      CurrencyAmount.of(EUR, -NOTIONAL), CurrencyAmount.of(GBP, NOTIONAL * STRIKE_RE), PAYMENT_DATE);

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxVanillaOption test = sut();
    assertEquals(test.getExpiry(), EXPIRY_DATE_TIME);
    assertEquals(test.getExpiryDate(), EXPIRY_DATE_TIME.toLocalDate());
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getCounterCurrency(), USD);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getUnderlying(), FX);
    assertEquals(test.getCurrencyPair(), FX.getCurrencyPair());
  }

  public void test_builder_inverseFx() {
    ResolvedFxVanillaOption test = sut2();
    assertEquals(test.getExpiry(), EXPIRY_DATE_TIME.plusSeconds(1));
    assertEquals(test.getExpiryDate(), EXPIRY_DATE_TIME.toLocalDate());
    assertEquals(test.getLongShort(), SHORT);
    assertEquals(test.getCounterCurrency(), GBP);
    assertEquals(test.getPutCall(), PUT);
    assertEquals(test.getStrike(), STRIKE_RE);
    assertEquals(test.getUnderlying(), FX_RE);
  }

  public void test_builder_earlyPaymentDate() {
    assertThrowsIllegalArg(() -> ResolvedFxVanillaOption.builder()
        .longShort(LONG)
        .expiry(LocalDate.of(2015, 2, 21).atStartOfDay(ZoneOffset.UTC))
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
        .longShort(LONG)
        .expiry(EXPIRY_DATE_TIME)
        .underlying(FX)
        .build();
  }

  static ResolvedFxVanillaOption sut2() {
    ;
    return ResolvedFxVanillaOption.builder()
        .longShort(SHORT)
        .expiry(EXPIRY_DATE_TIME.plusSeconds(1))
        .underlying(FX_RE)
        .build();
  }

}
