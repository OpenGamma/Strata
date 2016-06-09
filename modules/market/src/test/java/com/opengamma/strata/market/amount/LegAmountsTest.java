/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 * Test {@LegAmounts}.
 */
@Test
public class LegAmountsTest {

  private static final LegAmount LEG_AMOUNT_1 = SwapLegAmount.builder()
      .amount(CurrencyAmount.of(Currency.USD, 500))
      .payReceive(PayReceive.PAY)
      .type(SwapLegType.FIXED)
      .currency(Currency.USD)
      .build();
  private static final LegAmount LEG_AMOUNT_2 = SwapLegAmount.builder()
      .amount(CurrencyAmount.of(Currency.USD, 420))
      .payReceive(PayReceive.RECEIVE)
      .type(SwapLegType.IBOR)
      .currency(Currency.USD)
      .build();

  //-------------------------------------------------------------------------
  public void test_of_arrayAmounts() {
    LegAmounts test = LegAmounts.of(LEG_AMOUNT_1, LEG_AMOUNT_2);
    assertEquals(test.getAmounts().size(), 2);
    assertEquals(test.getAmounts().get(0), LEG_AMOUNT_1);
    assertEquals(test.getAmounts().get(1), LEG_AMOUNT_2);
  }

  public void test_of_list() {
    List<LegAmount> list = ImmutableList.of(LEG_AMOUNT_1, LEG_AMOUNT_2);
    LegAmounts test = LegAmounts.of(list);
    assertEquals(test.getAmounts().size(), 2);
    assertEquals(test.getAmounts().get(0), LEG_AMOUNT_1);
    assertEquals(test.getAmounts().get(1), LEG_AMOUNT_2);
  }

  //-------------------------------------------------------------------------
  public void convertedTo() {
    LegAmounts base = LegAmounts.of(LEG_AMOUNT_1, LEG_AMOUNT_2);
    LegAmounts test = base.convertedTo(Currency.GBP, FxRate.of(Currency.USD, Currency.GBP, 0.7));

    assertThat(test.getAmounts().get(0).getAmount().getCurrency()).isEqualTo(Currency.GBP);
    assertThat(test.getAmounts().get(0).getAmount().getAmount()).isEqualTo(500d * 0.7d);
    assertThat(test.getAmounts().get(1).getAmount().getCurrency()).isEqualTo(Currency.GBP);
    assertThat(test.getAmounts().get(1).getAmount().getAmount()).isEqualTo(420d * 0.7d);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    LegAmounts test1 = LegAmounts.of(LEG_AMOUNT_1, LEG_AMOUNT_2);
    coverImmutableBean(test1);

    LegAmount swapLeg = SwapLegAmount.builder()
        .amount(CurrencyAmount.of(Currency.GBP, 1557.445))
        .payReceive(PayReceive.PAY)
        .type(SwapLegType.FIXED)
        .currency(Currency.EUR)
        .build();
    LegAmounts test2 = LegAmounts.of(swapLeg);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    LegAmounts test = LegAmounts.of(LEG_AMOUNT_1, LEG_AMOUNT_2);
    assertSerialization(test);
  }

}
