/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import static com.opengamma.strata.engine.calculations.function.FunctionUtils.toScenarioResult;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculations.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.market.amount.LegAmount;
import com.opengamma.strata.market.amount.LegAmounts;
import com.opengamma.strata.market.amount.SwapLegAmount;

/**
 * Returns the notional amount of the legs of a {@code SwapTrade}.
 */
public class SwapLegNotionalFunction
    implements CalculationSingleFunction<SwapTrade, ScenarioResult<LegAmounts>> {

  @Override
  public CalculationRequirements requirements(SwapTrade target) {
    return CalculationRequirements.empty();
  }

  @Override
  public ScenarioResult<LegAmounts> execute(SwapTrade input, CalculationMarketData marketData) {
    LegAmounts notional = getNotional(input);
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(i -> notional)
        .collect(toScenarioResult());
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

  private LegAmounts getNotional(SwapTrade input) {
    List<LegAmount> legAmounts = input.getProduct().getLegs().stream()
        .filter(RateCalculationSwapLeg.class::isInstance)
        .map(RateCalculationSwapLeg.class::cast)
        .map(this::getLegAmount)
        .collect(toList());
    return LegAmounts.of(legAmounts);
  }
  
  private SwapLegAmount getLegAmount(RateCalculationSwapLeg leg) {
    Currency legCurrency = leg.getNotionalSchedule().getCurrency();
    CurrencyAmount amount = CurrencyAmount.of(legCurrency, leg.getNotionalSchedule().getAmount().getInitialValue());
    return SwapLegAmount.builder()
        .amount(amount)
        .payReceive(leg.getPayReceive())
        .legType(leg.getType())
        .legCurrency(legCurrency)
        .build();
  }

}
