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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;

/**
 * Test {@link SwapLegAmount}.
 */
@Test
public class SwapLegAmountTest {

  private static final CurrencyAmount CURRENCY_AMOUNT = CurrencyAmount.of(Currency.USD, 123.45);

  public void test_of() {
    SwapPaymentPeriod pp = mock(SwapPaymentPeriod.class);
    when(pp.getCurrency()).thenReturn(Currency.GBP);
    ResolvedSwapLeg leg = ResolvedSwapLeg.builder()
        .type(SwapLegType.FIXED)
        .payReceive(PayReceive.PAY)
        .paymentPeriods(pp)
        .build();
    SwapLegAmount legAmount = SwapLegAmount.of(leg, CurrencyAmount.of(Currency.GBP, 10));
    SwapLegAmount test = legAmount.convertedTo(Currency.USD, FxRate.of(Currency.GBP, Currency.USD, 1.6));

    assertThat(test.getAmount().getCurrency()).isEqualTo(Currency.USD);
    assertThat(test.getAmount().getAmount()).isEqualTo(16.0);
    assertThat(test.getPayReceive()).isEqualTo(legAmount.getPayReceive());
    assertThat(test.getType()).isEqualTo(legAmount.getType());
    assertThat(test.getCurrency()).isEqualTo(legAmount.getCurrency());
  }

  //-------------------------------------------------------------------------
  public void convertedTo() {
    SwapLegAmount legAmount = SwapLegAmount.builder()
        .amount(CurrencyAmount.of(Currency.GBP, 10))
        .payReceive(PayReceive.PAY)
        .type(SwapLegType.FIXED)
        .currency(Currency.GBP)
        .build();
    SwapLegAmount convertedAmount = legAmount.convertedTo(Currency.USD, FxRate.of(Currency.GBP, Currency.USD, 1.6));

    assertThat(convertedAmount.getAmount().getCurrency()).isEqualTo(Currency.USD);
    assertThat(convertedAmount.getAmount().getAmount()).isEqualTo(16.0);
    assertThat(convertedAmount.getPayReceive()).isEqualTo(legAmount.getPayReceive());
    assertThat(convertedAmount.getType()).isEqualTo(legAmount.getType());
    assertThat(convertedAmount.getCurrency()).isEqualTo(legAmount.getCurrency());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwapLegAmount la1 = SwapLegAmount.builder()
        .amount(CURRENCY_AMOUNT)
        .payReceive(PayReceive.PAY)
        .type(SwapLegType.FIXED)
        .currency(Currency.EUR)
        .build();
    coverImmutableBean(la1);
    SwapLegAmount la2 = SwapLegAmount.builder()
        .amount(CurrencyAmount.of(Currency.GBP, 10000))
        .payReceive(PayReceive.RECEIVE)
        .type(SwapLegType.IBOR)
        .currency(Currency.GBP)
        .build();
    coverBeanEquals(la1, la2);
  }

  public void test_serialization() {
    SwapLegAmount la = SwapLegAmount.builder()
        .amount(CURRENCY_AMOUNT)
        .payReceive(PayReceive.PAY)
        .type(SwapLegType.FIXED)
        .currency(Currency.EUR)
        .build();
    assertSerialization(la);
  }

}
