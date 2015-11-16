/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.collect.CollectProjectAssertions;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.amount.LegAmounts;
import com.opengamma.strata.market.amount.SwapLegAmount;
import com.opengamma.strata.pricer.rate.swap.SwapDummyData;
import com.opengamma.strata.product.rate.swap.KnownAmountSwapLeg;
import com.opengamma.strata.product.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.product.rate.swap.Swap;
import com.opengamma.strata.product.rate.swap.SwapLeg;
import com.opengamma.strata.product.rate.swap.SwapTrade;

/**
 * Test {@link SwapLegNotionalFunction}.
 */
@Test
public class SwapLegNotionalFunctionTest {

  private static final SwapTrade SWAP_TRADE = SwapDummyData.SWAP_TRADE;

  public void test_bothLegsPreExpanded() {
    SwapLeg firstLeg = SWAP_TRADE.getProduct().getLegs().get(0);
    SwapLeg secondLeg = SWAP_TRADE.getProduct().getLegs().get(1);
    Currency ccy = firstLeg.getCurrency();
    LocalDate valDate = SWAP_TRADE.getProduct().getEndDate().plusDays(7);
    RatePaymentPeriod firstPaymentPeriod = (RatePaymentPeriod) firstLeg.expand().getPaymentPeriods().get(0);
    double notional = firstPaymentPeriod.getNotional();

    SwapLegNotionalFunction test = new SwapLegNotionalFunction();
    FunctionRequirements reqs = test.requirements(SWAP_TRADE);
    assertThat(reqs.getOutputCurrencies()).isEmpty();
    assertThat(reqs.getSingleValueRequirements()).isEmpty();
    assertThat(reqs.getTimeSeriesRequirements()).isEmpty();
    CollectProjectAssertions.assertThat(test.defaultReportingCurrency(SWAP_TRADE)).hasValue(ccy);
    TestMarketDataMap md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());

    LegAmounts expected = LegAmounts.of(
        SwapLegAmount.of(firstLeg, CurrencyAmount.of(ccy, notional)),
        SwapLegAmount.of(secondLeg, CurrencyAmount.of(ccy, notional)));

    Object execute = test.execute(SWAP_TRADE, md);
    assertThat(execute).isEqualTo(FxConvertibleList.of(ImmutableList.of(expected)));
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
    LocalDate valDate = trade.getProduct().getEndDate().plusDays(7);
    TestMarketDataMap md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());

    double notional = secondLeg.getNotionalSchedule().getAmount().getInitialValue();
    LegAmounts expected = LegAmounts.of(
        SwapLegAmount.of(firstLeg, CurrencyAmount.of(GBP, notional)),
        SwapLegAmount.of(secondLeg, CurrencyAmount.of(GBP, notional)));

    SwapLegNotionalFunction test = new SwapLegNotionalFunction();
    Object execute = test.execute(trade, md);
    assertThat(execute).isEqualTo(FxConvertibleList.of(ImmutableList.of(expected)));
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
    LocalDate valDate = trade.getProduct().getEndDate().plusDays(7);
    TestMarketDataMap md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());

    SwapLegNotionalFunction test = new SwapLegNotionalFunction();
    assertThrowsIllegalArg(() -> test.execute(trade, md));
  }

}
