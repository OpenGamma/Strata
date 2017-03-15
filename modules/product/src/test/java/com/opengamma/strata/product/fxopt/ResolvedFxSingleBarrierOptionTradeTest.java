/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link ResolvedFxSingleBarrierOptionTrade}.
 */
@Test
public class ResolvedFxSingleBarrierOptionTradeTest {

  private static final ZonedDateTime EXPIRY_DATE_TIME = ZonedDateTime.of(2015, 2, 14, 12, 15, 0, 0, ZoneOffset.UTC);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final double STRIKE = 1.35;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * STRIKE);
  private static final ResolvedFxSingle FX = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);
  private static final ResolvedFxVanillaOption VANILLA_OPTION = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY_DATE_TIME)
      .underlying(FX)
      .build();
  private static final SimpleConstantContinuousBarrier BARRIER =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 1.2);
  private static final CurrencyAmount REBATE = CurrencyAmount.of(USD, 5.0e4);
  private static final ResolvedFxSingleBarrierOption PRODUCT =
      ResolvedFxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, REBATE);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 11, 12));
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(EUR, NOTIONAL * 0.05), date(2014, 11, 14));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxSingleBarrierOptionTrade test = ResolvedFxSingleBarrierOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getPremium(), PREMIUM);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedFxSingleBarrierOptionTrade test1 = ResolvedFxSingleBarrierOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
    ResolvedFxSingleBarrierOptionTrade test2 = ResolvedFxSingleBarrierOptionTrade.builder()
        .product(ResolvedFxSingleBarrierOption.of(VANILLA_OPTION, BARRIER))
        .premium(Payment.of(CurrencyAmount.of(EUR, NOTIONAL * 0.01), date(2014, 11, 13)))
        .build();
    coverImmutableBean(test1);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedFxSingleBarrierOptionTrade test = ResolvedFxSingleBarrierOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
    assertSerialization(test);
  }

}
