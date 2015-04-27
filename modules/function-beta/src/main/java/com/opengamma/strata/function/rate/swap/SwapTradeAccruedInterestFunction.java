/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.range.LocalDateRange;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RateAccrualPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapLegPricer;

/**
 * Calculates the accrued interest for a {@code SwapTrade} for each of a set of scenarios.
 */
public class SwapTradeAccruedInterestFunction
    implements EngineSingleFunction<SwapTrade, List<MultiCurrencyAmount>> {
  // TODO: implementation needs more work to handle edge cases

  @Override
  public CalculationRequirements requirements(SwapTrade target) {
    return CalculationRequirements.EMPTY;
  }

  @Override
  public List<MultiCurrencyAmount> execute(SwapTrade trade, CalculationMarketData marketData) {
    ExpandedSwap expandedSwap = trade.getProduct().expand();
    
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(env -> accruedInterest(env, expandedSwap))
        .collect(toList());
  }
  
  private MultiCurrencyAmount accruedInterest(MarketDataRatesProvider env, ExpandedSwap expandedSwap) {
    MultiCurrencyAmount result = MultiCurrencyAmount.empty();
    for (ExpandedSwapLeg leg : expandedSwap.getLegs()) {
      for (PaymentPeriod period : leg.getPaymentPeriods()) {
        RatePaymentPeriod ratePeriod = (RatePaymentPeriod) period;
        if (LocalDateRange.of(ratePeriod.getStartDate(), ratePeriod.getEndDate()).contains(env.getValuationDate())) {
          RateAccrualPeriod accrualPeriod = Iterables.getOnlyElement(ratePeriod.getAccrualPeriods());
          RateAccrualPeriod adjustedAccrualPeriod = accrualPeriod.toBuilder()
              .endDate(env.getValuationDate())
              .unadjustedEndDate(env.getValuationDate())
              .yearFraction(DayCounts.ACT_360.yearFraction(accrualPeriod.getStartDate(), env.getValuationDate()))
              .build();
          RatePaymentPeriod adjustedPaymentPeriod = ratePeriod.toBuilder()
              .accrualPeriods(adjustedAccrualPeriod)
              .build();
          ExpandedSwapLeg adjustedLeg = ExpandedSwapLeg.builder()
              .type(leg.getType())
              .payReceive(leg.getPayReceive())
              .paymentPeriods(adjustedPaymentPeriod)
              .build();
          CurrencyAmount accrual = DiscountingSwapLegPricer.DEFAULT.futureValue(adjustedLeg, env);
          result = result.plus(accrual);
          break;
        }
      }
    }
    return result;
  }

}
