/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.swap;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.market.amount.LegAmount;
import com.opengamma.strata.market.amount.LegAmounts;
import com.opengamma.strata.market.amount.SwapLegAmount;
import com.opengamma.strata.product.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.rate.swap.NotionalPaymentPeriod;
import com.opengamma.strata.product.rate.swap.PaymentPeriod;
import com.opengamma.strata.product.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.rate.swap.SwapLeg;
import com.opengamma.strata.product.rate.swap.SwapTrade;

/**
 * Calculates the initial notional of each leg of an interest rate swap.
 * <p>
 * The resulting {@linkplain LegAmounts leg amounts} contains of a value for each leg,
 * each expressed as a {@linkplain SwapLegAmount swap leg amount} with leg details.
 * <p>
 * The default reporting currency is determined from the first leg.
 */
public class SwapLegNotionalFunction
    extends AbstractCalculationFunction<SwapTrade, ScenarioResult<LegAmounts>> {

  /**
   * Special marker value used in place of null.
   */
  CurrencyAmount NOT_FOUND = CurrencyAmount.zero(Currency.XXX);

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(SwapTrade target) {
    return FunctionRequirements.empty();
  }

  @Override
  public ScenarioResult<LegAmounts> execute(SwapTrade input, CalculationMarketData marketData) {
    LegAmounts notional = buildNotional(input);
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(i -> notional)
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(SwapTrade target) {
    return Optional.of(target.getProduct().getLegs().get(0).getCurrency());
  }

  //-------------------------------------------------------------------------
  // find the notional
  private LegAmounts buildNotional(SwapTrade input) {
    List<Pair<SwapLeg, CurrencyAmount>> notionals = input.getProduct().getLegs().stream()
        .map(leg -> Pair.of(leg, buildLegNotional(leg)))
        .collect(toList());
    CurrencyAmount firstNotional = notionals.stream()
        .filter(pair -> pair.getSecond() != NOT_FOUND)
        .map(pair -> pair.getSecond())
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No notional found on any swap leg"));
    notionals = notionals.stream()
        .map(pair -> pair.getSecond() != NOT_FOUND ? pair : Pair.of(pair.getFirst(), firstNotional))
        .collect(toList());
    ImmutableList<LegAmount> legAmounts = notionals.stream()
        .map(pair -> SwapLegAmount.of(pair.getFirst(), pair.getSecond()))
        .collect(toImmutableList());
    return LegAmounts.of(legAmounts);
  }

  // find the notional
  private CurrencyAmount buildLegNotional(SwapLeg leg) {
    // try RateCalculationSwapLeg first to avoid expand
    if (leg instanceof RateCalculationSwapLeg) {
      RateCalculationSwapLeg rcleg = (RateCalculationSwapLeg) leg;
      return CurrencyAmount.of(leg.getCurrency(), Math.abs(rcleg.getNotionalSchedule().getAmount().getInitialValue()));
    }
    // expand and check for NotionalPaymentPeriod
    ExpandedSwapLeg expanded = leg.expand();
    PaymentPeriod firstPaymentPeriod = expanded.getPaymentPeriods().get(0);
    if (firstPaymentPeriod instanceof NotionalPaymentPeriod) {
      NotionalPaymentPeriod pp = (NotionalPaymentPeriod) firstPaymentPeriod;
      return pp.getNotionalAmount().positive();
    } else {
      return NOT_FOUND;
    }
  }

}
