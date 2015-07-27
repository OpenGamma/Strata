/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;

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
    MetaBean metaBean = JodaBeanUtils.metaBean(trade.getClass());
    return Sets.union(metaBean.metaPropertyMap().keySet(), trade.getTradeInfo().propertyNames());
  }

  @Override
  public Result<?> evaluate(Trade trade, String token) {
    MetaBean metaBean = JodaBeanUtils.metaBean(trade.getClass());
    // trade
    Optional<String> propertyName1 = metaBean.metaPropertyMap().keySet().stream()
        .filter(p -> p.equalsIgnoreCase(token))
        .findFirst();
    if (propertyName1.isPresent()) {
      Object propertyValue = metaBean.metaProperty(propertyName1.get()).get((Bean) trade);
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
