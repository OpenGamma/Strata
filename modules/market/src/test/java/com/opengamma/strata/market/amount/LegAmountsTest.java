/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.swap.SwapLegType;

/**
 * Test {@LegAmounts}.
 */
@Test
public class LegAmountsTest {

  private static final LegAmount LEG_AMOUNT_1 = SwapLegAmount.builder()
      .amount(CurrencyAmount.of(Currency.USD, 500))
      .payReceive(PayReceive.PAY)
      .legType(SwapLegType.FIXED)
      .legCurrency(Currency.USD)
      .build();
  
  private static final LegAmount LEG_AMOUNT_2 = SwapLegAmount.builder()
      .amount(CurrencyAmount.of(Currency.USD, 420))
      .payReceive(PayReceive.RECEIVE)
      .legType(SwapLegType.IBOR)
      .legCurrency(Currency.USD)
      .build();
  
  //-------------------------------------------------------------------------
  public void test_of_arrayAmounts() {
    LegAmounts la = LegAmounts.of(LEG_AMOUNT_1, LEG_AMOUNT_2);
    assertEquals(la.getAmounts().size(), 2);
    assertEquals(la.getAmounts().get(0), LEG_AMOUNT_1);
    assertEquals(la.getAmounts().get(1), LEG_AMOUNT_2);
  }
  
  public void test_of_list() {
    List<LegAmount> list = ImmutableList.of(LEG_AMOUNT_1, LEG_AMOUNT_2);
    LegAmounts la = LegAmounts.of(list);
    assertEquals(la.getAmounts().size(), 2);
    assertEquals(la.getAmounts().get(0), LEG_AMOUNT_1);
    assertEquals(la.getAmounts().get(1), LEG_AMOUNT_2);
  }
  
  //-------------------------------------------------------------------------
  public void coverage() {
    LegAmounts la1 = LegAmounts.of(LEG_AMOUNT_1, LEG_AMOUNT_2);
    coverImmutableBean(la1);
    
    LegAmount swapLeg = SwapLegAmount.builder()
        .amount(CurrencyAmount.of(Currency.GBP, 1557.445))
        .payReceive(PayReceive.PAY)
        .legType(SwapLegType.FIXED)
        .legCurrency(Currency.EUR)
        .build();
    LegAmounts la2 = LegAmounts.of(swapLeg);
    coverBeanEquals(la1, la2);
  }
  
  public void test_serialization() {
    LegAmounts la = LegAmounts.of(LEG_AMOUNT_1, LEG_AMOUNT_2);
    assertSerialization(la);
  }
  
}
