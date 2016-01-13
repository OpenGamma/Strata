/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swaption;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.function.calculation.swaption.SwaptionFunctionGroups;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swaption.PhysicalSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Test {@link SwaptionFunctionGroups}.
 */
@Test
public class SwaptionFunctionGroupsTest {

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

  private static final SwaptionTrade TRADE = SwaptionTrade.builder()
      .premium(PREMIUM)
      .product(SWAPTION)
      .tradeInfo(TRADE_INFO)
      .build();

  public void test_discounting() {
    FunctionGroup<SwaptionTrade> test = SwaptionFunctionGroups.standard();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PRESENT_VALUE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(SwaptionFunctionGroups.class);
  }

}
