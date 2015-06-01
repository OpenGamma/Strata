/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.swap.SwapLegType;

/**
 * Test {@link SwapLegAmount}.
 */
@Test
public class SwapLegAmountTest {

  private static final CurrencyAmount CURRENCY_AMOUNT = CurrencyAmount.of(Currency.USD, 123.45);
  
  public void coverage() {
    SwapLegAmount la1 = SwapLegAmount.builder()
        .amount(CURRENCY_AMOUNT)
        .payReceieve(PayReceive.PAY)
        .legCurrency(Currency.EUR)
        .legType(SwapLegType.FIXED)
        .build();
    coverImmutableBean(la1);
    SwapLegAmount la2 = SwapLegAmount.builder()
        .amount(CurrencyAmount.of(Currency.GBP, 10000))
        .payReceieve(PayReceive.RECEIVE)
        .legCurrency(Currency.GBP)
        .legType(SwapLegType.IBOR)
        .build();
    coverBeanEquals(la1, la2);
  }
  
  public void test_serialization() {
    SwapLegAmount la = SwapLegAmount.builder()
        .amount(CURRENCY_AMOUNT)
        .payReceieve(PayReceive.PAY)
        .legCurrency(Currency.EUR)
        .legType(SwapLegType.FIXED)
        .build();
    assertSerialization(la);
  }
  
}
