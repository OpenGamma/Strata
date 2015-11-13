/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.swap;

import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATECALC_SWAP_LEG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.collect.CollectProjectAssertions;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.function.marketdata.curve.MarketDataMap;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.pricer.impl.rate.swap.DiscountingRatePaymentPeriodPricer;
import com.opengamma.strata.product.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.rate.swap.RateAccrualPeriod;
import com.opengamma.strata.product.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.product.rate.swap.Swap;
import com.opengamma.strata.product.rate.swap.SwapTrade;

/**
 * Test {@link SwapAccruedInterestFunction}.
 */
@Test
public class SwapAccruedInterestFunctionTest {

  public void test_simple() {
    Currency ccy = FIXED_RATECALC_SWAP_LEG.getCurrency();
    LocalDate valDate = FIXED_RATECALC_SWAP_LEG.getEndDate().minusDays(7);
    SwapTrade trade = SwapTrade.builder()
        .product(Swap.of(FIXED_RATECALC_SWAP_LEG))
        .build();

    SwapAccruedInterestFunction test = new SwapAccruedInterestFunction();
    FunctionRequirements reqs = test.requirements(trade);
    assertThat(reqs.getOutputCurrencies()).containsOnly(ccy);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(DiscountFactorsKey.of(ccy)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    CollectProjectAssertions.assertThat(test.defaultReportingCurrency(trade)).hasValue(ccy);
    MarketDataMap md = new MarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    MarketDataRatesProvider prov = new MarketDataRatesProvider(new DefaultSingleCalculationMarketData(md, 0));

    ExpandedSwapLeg expandedLeg = FIXED_RATECALC_SWAP_LEG.expand();
    RatePaymentPeriod rpp = (RatePaymentPeriod) expandedLeg.getPaymentPeriods().get(expandedLeg.getPaymentPeriods().size() - 1);
    RateAccrualPeriod rap = rpp.getAccrualPeriods().get(0);
    RateAccrualPeriod rap2 = rap.toBuilder()
        .endDate(valDate)
        .unadjustedEndDate(valDate)
        .yearFraction(DayCounts.ACT_365F.yearFraction(rap.getStartDate(), valDate))
        .build();
    RatePaymentPeriod rpp2 = rpp.toBuilder().accrualPeriods(rap2).build();
    CurrencyAmount expected = CurrencyAmount.of(ccy, DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(rpp2, prov));

    Object execute = test.execute(trade, md);
    assertThat(execute).isEqualTo(FxConvertibleList.of(ImmutableList.of(MultiCurrencyAmount.of(expected))));
  }

}
