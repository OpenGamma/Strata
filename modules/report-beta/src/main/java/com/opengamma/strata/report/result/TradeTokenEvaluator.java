/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;

/**
 * Evaluates a token against a trade to produce another object.
 * <p>
 * This merges the {@link Trade} and {@link TradeInfo} objects, giving priority to {@code Trade}.
 */
public class TradeTokenEvaluator extends TokenEvaluator<Trade> {

  @Override
  public Class<Trade> getTargetType() {
    return Trade.class;
  }

  @Override
  public Set<String> tokens(Trade trade) {
    return Sets.union(trade.propertyNames(), trade.getTradeInfo().propertyNames());
  }

  @Override
  public Result<?> evaluate(Trade trade, String token) {
    // trade
    Optional<String> propertyName1 = trade.propertyNames().stream()
        .filter(p -> p.equalsIgnoreCase(token))
        .findFirst();
    if (propertyName1.isPresent()) {
      Object propertyValue = trade.property(propertyName1.get()).get();
      return propertyValue != null ? Result.success(propertyValue) : Result.failure(FailureReason.INVALID_INPUT,
          Messages.format("Property '{}' not set", token));
    }

    // trade info
    Optional<String> propertyName2 = trade.getTradeInfo().propertyNames().stream()
        .filter(p -> p.equalsIgnoreCase(token))
        .findFirst();
    if (propertyName2.isPresent()) {
      Object propertyValue = trade.getTradeInfo().property(propertyName2.get()).get();
      return propertyValue != null ? Result.success(propertyValue) : Result.failure(FailureReason.INVALID_INPUT,
          Messages.format("Property '{}' not set", token));
    }

    // no match
    return invalidTokenFailure(trade, token);
  }

}
