/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swaption;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.rate.swap.Swap;
import com.opengamma.strata.product.rate.swap.type.FixedIborSwapConventions;

/**
 * Test {@code SwaptionTrade}.
 */
@Test
public class SwaptionTradeTest {

  private static final double FIXED_RATE = 0.015;
  private static final double NOTIONAL = 100000000d;
  private static final Swap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .toTrade(LocalDate.of(2014, 6, 12), Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE).getProduct();
  private static final BusinessDayAdjustment ADJUSTMENT =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, GBLO.combineWith(USNY));
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 6, 14);
  private static final AdjustableDate ADJUSTABLE_EXPIRY_DATE = AdjustableDate.of(EXPIRY_DATE, ADJUSTMENT);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSettlement.DEFAULT;
  private static final Swaption SWAPTION = Swaption.builder()
      .expiryDate(ADJUSTABLE_EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .longShort(LONG)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(LocalDate.of(2014, 3, 14)).build();
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(Currency.USD, -3150000d), LocalDate.of(2014, 3, 17));

  public void test_builder() {
    SwaptionTrade test = SwaptionTrade.builder().premium(PREMIUM).product(SWAPTION).tradeInfo(TRADE_INFO).build();
    assertEquals(test.getPremium(), PREMIUM);
    assertEquals(test.getProduct(), SWAPTION);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionTrade test1 = SwaptionTrade.builder().premium(PREMIUM).product(SWAPTION).tradeInfo(TRADE_INFO).build();
    coverImmutableBean(test1);
    SwaptionTrade test2 = SwaptionTrade.builder()
        .premium(Payment.of(CurrencyAmount.of(Currency.USD, -3050000d), LocalDate.of(2014, 3, 17)))
        .product(Swaption.builder()
            .expiryDate(AdjustableDate.of(LocalDate.of(2014, 6, 13), ADJUSTMENT))
            .expiryTime(EXPIRY_TIME)
            .expiryZone(ZONE)
            .longShort(LONG)
            .swaptionSettlement(PHYSICAL_SETTLE)
            .underlying(SWAP)
            .build())
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SwaptionTrade test = SwaptionTrade.builder().premium(PREMIUM).product(SWAPTION).tradeInfo(TRADE_INFO).build();
    assertSerialization(test);
  }

}
