/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.ResolvedBulletPaymentTrade;

/**
 * Test {@link ResolvedTradeParameterMetadata}.
 */
@Test
public class ResolvedTradeParameterMetadataTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ResolvedTrade TRADE = ResolvedBulletPaymentTrade.of(
      TradeInfo.empty(),
      BulletPayment.builder()
          .date(AdjustableDate.of(LocalDate.of(2017, 1, 3)))
          .value(CurrencyAmount.of(Currency.BHD, 100d))
          .payReceive(PayReceive.PAY)
          .build()
          .resolve(REF_DATA));

  public void test_of() {
    ResolvedTradeParameterMetadata test = ResolvedTradeParameterMetadata.of(TRADE, "Label");
    assertEquals(test.getLabel(), "Label");
    assertEquals(test.getIdentifier(), "Label");
    assertEquals(test.getTrade(), TRADE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedTradeParameterMetadata test1 = ResolvedTradeParameterMetadata.of(TRADE, "Label");
    coverImmutableBean(test1);
    ResolvedTrade trade = ResolvedBulletPaymentTrade.of(
        TradeInfo.empty(),
        BulletPayment.builder()
            .date(AdjustableDate.of(LocalDate.of(2017, 3, 3)))
            .value(CurrencyAmount.of(Currency.USD, 100d))
            .payReceive(PayReceive.PAY)
            .build()
            .resolve(REF_DATA));
    ResolvedTradeParameterMetadata test2 = ResolvedTradeParameterMetadata.builder().trade(trade).label("Label2").build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedTradeParameterMetadata test = ResolvedTradeParameterMetadata.of(TRADE, "Label");
    assertSerialization(test);
  }

}
