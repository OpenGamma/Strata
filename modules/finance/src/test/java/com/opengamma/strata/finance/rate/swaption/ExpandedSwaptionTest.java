/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swaption;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.LongShort.SHORT;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConventions;

/**
 * Test {@link ExpandedSwaption}.
 */
@Test
public class ExpandedSwaptionTest {
  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 6, 12); // starts on 2014/6/19
  private static final double FIXED_RATE = 0.015;
  private static final double NOTIONAL = 100000000d;
  private static final ExpandedSwap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .toTrade(TRADE_DATE, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE).getProduct().expand();
  private static final BusinessDayAdjustment ADJUSTMENT =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, GBLO.combineWith(USNY));
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 6, 13);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final SwaptionSettlementMethod PHYSICAL_SETTLE = new SwaptionSettlementMethod() {
    @Override
    public SettlementType getSettlementType() {
      return SettlementType.PHYSICAL;
    }
  };

  public void test_builder() {
    ExpandedSwaption test = ExpandedSwaption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .settlementMethod(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build();
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), ZONE);
    assertEquals(test.getExpiryDateTime(), EXPIRY_DATE.atTime(EXPIRY_TIME).atZone(ZONE));
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getSettlementMethod(), PHYSICAL_SETTLE);
    assertEquals(test.getUnderlying(), SWAP);
  }

  public void test_expand() {
    ExpandedSwaption base = ExpandedSwaption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .settlementMethod(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build();
    assertSame(base.expand(), base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedSwaption test1 = ExpandedSwaption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .settlementMethod(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build();
    coverImmutableBean(test1);
    SwaptionSettlementMethod cash = new SwaptionSettlementMethod() {
      @Override
      public SettlementType getSettlementType() {
        return SettlementType.CASH;
      }
    };
    ExpandedSwaption test2 = ExpandedSwaption.builder()
        .expiryDate(LocalDate.of(2014, 6, 12))
        .expiryTime(LocalTime.of(14, 0))
        .expiryZone(ZoneId.of("GMT"))
        .longShort(SHORT)
        .settlementMethod(cash)
        .underlying(FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
            .toTrade(LocalDate.of(2014, 6, 10), Tenor.TENOR_10Y, BuySell.BUY, 1d, FIXED_RATE).getProduct().expand())
        .build();
    coverBeanEquals(test1, test2);
  }
}
