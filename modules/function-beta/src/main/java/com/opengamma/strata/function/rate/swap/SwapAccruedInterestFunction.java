/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import static com.opengamma.strata.engine.calculations.function.FunctionUtils.toFxConvertibleList;

import java.util.Optional;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.range.LocalDateRange;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculations.function.result.FxConvertibleList;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.FunctionRequirements;
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
public class SwapAccruedInterestFunction
    implements CalculationSingleFunction<SwapTrade, FxConvertibleList> {
  // TODO: implementation needs more work to handle edge cases

  @Override
  public FunctionRequirements requirements(SwapTrade target) {
    return FunctionRequirements.empty();
  }

  @Override
  public FxConvertibleList execute(SwapTrade trade, CalculationMarketData marketData) {
    ExpandedSwap expandedSwap = trade.getProduct().expand();
    
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(env -> accruedInterest(env, expandedSwap))
        .collect(toFxConvertibleList());
  }

  /**
   * Returns the currency of the first leg of the swap.
   *
   * @param target  the swap that is the target of the calculation
   * @return the currency of the first leg of the swap
   */
  @Override
  public Optional<Currency> defaultReportingCurrency(SwapTrade target) {
    return Optional.of(target.getProduct().getLegs().get(0).getCurrency());
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
