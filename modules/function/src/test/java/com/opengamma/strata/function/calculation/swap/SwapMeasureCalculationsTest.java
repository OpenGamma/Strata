/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.market.amount.LegAmounts;
import com.opengamma.strata.market.amount.SwapLegAmount;
import com.opengamma.strata.pricer.swap.SwapDummyData;
import com.opengamma.strata.product.swap.KnownAmountSwapLeg;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link SwapMeasureCalculations}.
 */
@Test
public class SwapMeasureCalculationsTest {

  private static final SwapTrade SWAP_TRADE = SwapDummyData.SWAP_TRADE;

  public void test_bothLegsPreExpanded() {
    SwapLeg firstLeg = SWAP_TRADE.getProduct().getLegs().get(0);
    SwapLeg secondLeg = SWAP_TRADE.getProduct().getLegs().get(1);
    Currency ccy = firstLeg.getCurrency();
    RatePaymentPeriod firstPaymentPeriod = (RatePaymentPeriod) firstLeg.expand().getPaymentPeriods().get(0);
    double notional = firstPaymentPeriod.getNotional();

    LegAmounts expected = LegAmounts.of(
        SwapLegAmount.of(firstLeg, CurrencyAmount.of(ccy, notional)),
        SwapLegAmount.of(secondLeg, CurrencyAmount.of(ccy, notional)));

    assertThat(SwapMeasureCalculations.calculateLegInitialNotional(SWAP_TRADE)).isEqualTo(expected);
  }

  public void test_bothLegsParameterized() {
    SwapLeg firstLeg = KnownAmountSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(SwapDummyData.IBOR_RATECALC_SWAP_LEG.getAccrualSchedule())
        .paymentSchedule(SwapDummyData.IBOR_RATECALC_SWAP_LEG.getPaymentSchedule())
        .currency(GBP)
        .amount(ValueSchedule.of(1000d))
        .build();
    RateCalculationSwapLeg secondLeg = SwapDummyData.IBOR_RATECALC_SWAP_LEG;
    SwapTrade trade = SwapTrade.builder().product(Swap.of(firstLeg, secondLeg)).build();

    double notional = secondLeg.getNotionalSchedule().getAmount().getInitialValue();
    LegAmounts expected = LegAmounts.of(
        SwapLegAmount.of(firstLeg, CurrencyAmount.of(GBP, notional)),
        SwapLegAmount.of(secondLeg, CurrencyAmount.of(GBP, notional)));

    assertThat(SwapMeasureCalculations.calculateLegInitialNotional(trade)).isEqualTo(expected);
  }

  public void test_bothLegsWithoutNotional() {
    SwapLeg leg = KnownAmountSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(SwapDummyData.IBOR_RATECALC_SWAP_LEG.getAccrualSchedule())
        .paymentSchedule(SwapDummyData.IBOR_RATECALC_SWAP_LEG.getPaymentSchedule())
        .currency(GBP)
        .amount(ValueSchedule.of(1000d))
        .build();
    SwapTrade trade = SwapTrade.builder().product(Swap.of(leg, leg)).build();

    assertThrowsIllegalArg(() -> SwapMeasureCalculations.calculateLegInitialNotional(trade));
  }

}
