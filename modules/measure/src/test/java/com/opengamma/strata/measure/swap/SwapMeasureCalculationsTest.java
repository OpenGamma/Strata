/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swap;

import static com.opengamma.strata.pricer.swap.SwapDummyData.KNOWN_AMOUNT_SWAP_LEG;
import static com.opengamma.strata.pricer.swap.SwapDummyData.SWAP_TRADE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.amount.LegAmounts;
import com.opengamma.strata.market.amount.SwapLegAmount;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Test {@link SwapMeasureCalculations}.
 */
public class SwapMeasureCalculationsTest {

  @Test
  public void test_legInitialNotional() {
    ResolvedSwapLeg firstLeg = SWAP_TRADE.getProduct().getLegs().get(0);
    ResolvedSwapLeg secondLeg = SWAP_TRADE.getProduct().getLegs().get(1);
    Currency ccy = firstLeg.getCurrency();
    RatePaymentPeriod firstPaymentPeriod = (RatePaymentPeriod) firstLeg.getPaymentPeriods().get(0);
    double notional = firstPaymentPeriod.getNotional();

    LegAmounts expected = LegAmounts.of(
        SwapLegAmount.of(firstLeg, CurrencyAmount.of(ccy, notional)),
        SwapLegAmount.of(secondLeg, CurrencyAmount.of(ccy, notional)));

    assertThat(SwapMeasureCalculations.DEFAULT.legInitialNotional(SWAP_TRADE)).isEqualTo(expected);
  }

  @Test
  public void test_legInitialNotionalWithoutNotional() {
    ResolvedSwapTrade trade = ResolvedSwapTrade.builder()
        .product(ResolvedSwap.of(KNOWN_AMOUNT_SWAP_LEG, KNOWN_AMOUNT_SWAP_LEG))
        .build();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> SwapMeasureCalculations.DEFAULT.legInitialNotional(trade));
  }

}
