/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.LongShort.SHORT;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions;
import com.opengamma.strata.product.swap.type.IborIborSwapConventions;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConventions;

/**
 * Test {@link Swaption}.
 */
@Test
public class SwaptionTest {

  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 6, 12); // starts on 2014/6/19
  private static final double FIXED_RATE = 0.015;
  private static final double NOTIONAL = 100000000d;
  private static final Swap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .createTrade(TRADE_DATE, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE).getProduct();
  private static final BusinessDayAdjustment ADJUSTMENT =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, GBLO.combineWith(USNY));
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 6, 14);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final AdjustableDate ADJUSTABLE_EXPIRY_DATE = AdjustableDate.of(EXPIRY_DATE, ADJUSTMENT);
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSettlement.DEFAULT;
  private static final SwaptionSettlement CASH_SETTLE = CashSettlement.builder()
      .cashSettlementMethod(CashSettlementMethod.PAR_YIELD)
      .settlementDate(SWAP.getStartDate())
      .build();
  private static final Swap SWAP_OIS = FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS
      .createTrade(TRADE_DATE, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE).getProduct();
  private static final Swap SWAP_BASIS = IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M
      .createTrade(TRADE_DATE, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE).getProduct();
  private static final Swap SWAP_XCCY = XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M
      .createTrade(TRADE_DATE, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, NOTIONAL * 1.1, FIXED_RATE).getProduct();

  //-------------------------------------------------------------------------
  public void test_builder() {
    Swaption test = Swaption.builder()
        .expiryDate(ADJUSTABLE_EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build();
    assertEquals(test.getExpiryDate(), ADJUSTABLE_EXPIRY_DATE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), ZONE);
    assertEquals(test.getExpiryDateTime(), EXPIRY_DATE.atTime(EXPIRY_TIME).atZone(ZONE));
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getSwaptionSettlement(), PHYSICAL_SETTLE);
    assertEquals(test.getUnderlying(), SWAP);
    assertEquals(test.getCurrency(), Currency.USD);
    assertEquals(test.getIndex(), IborIndices.USD_LIBOR_3M);
  }

  public void test_builder_expiryAfterStart() {
    assertThrowsIllegalArg(() -> Swaption.builder()
        .expiryDate(AdjustableDate.of(LocalDate.of(2014, 6, 17), ADJUSTMENT))
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build());
  }

  public void test_builder_invalidSwapOis() {
    assertThrowsIllegalArg(() -> Swaption.builder()
        .expiryDate(ADJUSTABLE_EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP_OIS)
        .build());
  }

  public void test_builder_invalidSwapBasis() {
    assertThrowsIllegalArg(() -> Swaption.builder()
        .expiryDate(ADJUSTABLE_EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP_BASIS)
        .build());
  }

  public void test_builder_invalidSwapXCcy() {
    assertThrowsIllegalArg(() -> Swaption.builder()
        .expiryDate(ADJUSTABLE_EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP_XCCY)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    Swaption base = Swaption.builder()
        .expiryDate(ADJUSTABLE_EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build();
    ExpandedSwaption test = base.expand();
    assertEquals(test.getExpiryDate(), ADJUSTMENT.adjust(EXPIRY_DATE));
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), ZONE);
    assertEquals(test.getExpiryDateTime(), ADJUSTMENT.adjust(EXPIRY_DATE).atTime(EXPIRY_TIME).atZone(ZONE));
    assertEquals(test.getLongShort(), LONG);
    assertEquals(test.getSwaptionSettlement(), PHYSICAL_SETTLE);
    assertEquals(test.getUnderlying(), SWAP.expand());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Swaption test1 = Swaption.builder()
        .expiryDate(ADJUSTABLE_EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build();
    coverImmutableBean(test1);
    Swaption test2 = Swaption.builder()
        .expiryDate(AdjustableDate.of(LocalDate.of(2014, 6, 10), ADJUSTMENT))
        .expiryTime(LocalTime.of(14, 0))
        .expiryZone(ZoneId.of("GMT"))
        .longShort(SHORT)
        .swaptionSettlement(CASH_SETTLE)
        .underlying(FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
            .createTrade(LocalDate.of(2014, 6, 10), Tenor.TENOR_10Y, BuySell.BUY, 1d, FIXED_RATE).getProduct())
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    Swaption test = Swaption.builder()
        .expiryDate(ADJUSTABLE_EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build();
    assertSerialization(test);
  }

}
