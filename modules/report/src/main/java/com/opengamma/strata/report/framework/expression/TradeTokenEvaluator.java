/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;

import com.google.common.collect.Sets;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.finance.FinanceTrade;
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
    if (trade instanceof FinanceTrade) {
      FinanceTrade ftrade = (FinanceTrade) trade;
      return Sets.union(metaBean.metaPropertyMap().keySet(), ftrade.getTradeInfo().propertyNames());
    }
    return metaBean.metaPropertyMap().keySet();
  }

  @Override
  public EvaluationResult evaluate(Trade trade, String firstToken, List<String> remainingTokens) {
    MetaBean metaBean = JodaBeanUtils.metaBean(trade.getClass());

    // trade
    Optional<String> tradePropertyName = metaBean.metaPropertyMap().keySet().stream()
        .filter(p -> p.equalsIgnoreCase(firstToken))
        .findFirst();

    if (tradePropertyName.isPresent()) {
      Object propertyValue = metaBean.metaProperty(tradePropertyName.get()).get((Bean) trade);

      return propertyValue != null ?
          EvaluationResult.success(propertyValue, remainingTokens) :
          EvaluationResult.failure("Property '{}' not set", firstToken);
    }

    // trade info
    if (trade instanceof FinanceTrade) {
      FinanceTrade ftrade = (FinanceTrade) trade;
      Optional<String> tradeInfoPropertyName = ftrade.getTradeInfo().propertyNames().stream()
          .filter(p -> p.equalsIgnoreCase(firstToken))
          .findFirst();

      if (tradeInfoPropertyName.isPresent()) {
        Object propertyValue = ftrade.getTradeInfo().property(tradeInfoPropertyName.get()).get();

        return propertyValue != null ?
            EvaluationResult.success(propertyValue, remainingTokens) :
            EvaluationResult.failure("Property '{}' not set", firstToken);
      }
    }
    return invalidTokenFailure(trade, firstToken);
  }

}
