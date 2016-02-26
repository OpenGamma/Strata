/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.PutCall.PUT;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Test {@link ResolvedIborFutureOption}. 
 */
@Test
public class ResolvedIborFutureOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_1 = 1_000d;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 9, 16);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);
  private static final ResolvedIborFuture IBOR_FUTURE_1 = ResolvedIborFuture.builder()
      .currency(GBP)
      .notional(NOTIONAL_1)
      .observation(IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1, REF_DATA))
      .rounding(ROUNDING)
      .build();

  private static final ZoneId EUROPE_LONDON = ZoneId.of("Europe/London");
  private static final ZonedDateTime EXPIRY = ZonedDateTime.of(2015, 5, 20, 11, 0, 0, 0, EUROPE_LONDON);
  private static final double STRIKE_PRICE = 1.075;

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborFutureOption test = ResolvedIborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiry(EXPIRY)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .rounding(ROUNDING)
        .underlying(IBOR_FUTURE_1)
        .build();
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getExpiry(), EXPIRY);
    assertEquals(test.getExpiryDate(), EXPIRY.toLocalDate());
    assertEquals(test.getRounding(), ROUNDING);
    assertEquals(test.getUnderlying(), IBOR_FUTURE_1);
  }

  public void test_builder_expiryNotAfterTradeDate() {
    assertThrowsIllegalArg(() -> ResolvedIborFutureOption.builder()
        .putCall(CALL)
        .expiry(LAST_TRADE_DATE_2.atTime(LocalTime.MIDNIGHT).atZone(EUROPE_LONDON))
        .strikePrice(STRIKE_PRICE)
        .underlying(IBOR_FUTURE_1)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedIborFutureOption test = ResolvedIborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiry(EXPIRY)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .rounding(ROUNDING)
        .underlying(IBOR_FUTURE_1)
        .build();
    coverImmutableBean(test);
    ResolvedIborFutureOption test2 = ResolvedIborFutureOption.builder()
        .putCall(PUT)
        .strikePrice(STRIKE_PRICE + 1)
        .expiry(EXPIRY.plusSeconds(1))
        .premiumStyle(FutureOptionPremiumStyle.UPFRONT_PREMIUM)
        .rounding(ROUNDING)
        .underlying(IBOR_FUTURE_1)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedIborFutureOption test = ResolvedIborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiry(EXPIRY)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .rounding(ROUNDING)
        .underlying(IBOR_FUTURE_1)
        .build();
    assertSerialization(test);
  }

}
