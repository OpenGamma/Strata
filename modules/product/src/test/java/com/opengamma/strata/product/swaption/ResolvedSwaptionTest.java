/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link ResolvedSwaption}.
 */
@Test
public class ResolvedSwaptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 6, 12); // starts on 2014/6/19
  private static final double FIXED_RATE = 0.015;
  private static final double NOTIONAL = 100000000d;
  private static final ResolvedSwap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .createTrade(TRADE_DATE, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE, REF_DATA).getProduct().resolve(REF_DATA);
  private static final ZoneId EUROPE_LONDON = ZoneId.of("Europe/London");
  private static final ZonedDateTime EXPIRY = ZonedDateTime.of(2014, 6, 13, 11, 0, 0, 0, EUROPE_LONDON);
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSwaptionSettlement.DEFAULT;
  private static final SwaptionSettlement CASH_SETTLE =
      CashSwaptionSettlement.of(SWAP.getLegs().get(0).getStartDate(), CashSwaptionSettlementMethod.PAR_YIELD);

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedSwaption test = sut();
    assertEquals(test.getExpiryDate(), EXPIRY.toLocalDate());
    assertEquals(test.getExpiry(), EXPIRY);
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getSwaptionSettlement(), PHYSICAL_SETTLE);
    assertEquals(test.getUnderlying(), SWAP);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
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
  static ResolvedSwaption sut() {
    return ResolvedSwaption.builder()
        .expiry(EXPIRY)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build();
  }

  static ResolvedSwaption sut2() {
    return ResolvedSwaption.builder()
        .expiry(EXPIRY.plusHours(1))
        .longShort(SHORT)
        .swaptionSettlement(CASH_SETTLE)
        .underlying(FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
            .createTrade(LocalDate.of(2014, 6, 10), Tenor.TENOR_10Y, BuySell.BUY, 1d, FIXED_RATE, REF_DATA)
            .getProduct().resolve(REF_DATA))
        .build();
  }

}
