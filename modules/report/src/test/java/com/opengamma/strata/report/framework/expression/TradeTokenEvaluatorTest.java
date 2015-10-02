/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.future.GenericFuture;
import com.opengamma.strata.finance.future.GenericFutureTrade;

/**
 * Test {@link TradeTokenEvaluator}.
 */
@Test
public class TradeTokenEvaluatorTest {

  public void tokens() {
    TradeTokenEvaluator evaluator = new TradeTokenEvaluator();
    Set<String> tokens = evaluator.tokens(trade());
    Set<String> expected = ImmutableSet.of(
        "quantity",
        "initialPrice",
        "securityLink",
        "id",
        "counterparty",
        "tradeDate",
        "tradeTime",
        "zone",
        "settlementDate",
        "attributes",
        "tradeInfo");
    assertThat(tokens).isEqualTo(expected);
  }

  public void evaluate() {
    TradeTokenEvaluator evaluator = new TradeTokenEvaluator();
    Trade trade = trade();

    Result<?> quantity = evaluator.evaluate(trade, "quantity");
    assertThat(quantity).hasValue(123L);

    Result<?> initialPrice = evaluator.evaluate(trade, "initialPrice");
    assertThat(initialPrice).hasValue(456d);

    // Check that property name isn't case sensitive
    Result<?> initialPrice2 = evaluator.evaluate(trade, "initialprice");
    assertThat(initialPrice2).hasValue(456d);

    Result<?> counterparty = evaluator.evaluate(trade, "counterparty");
    assertThat(counterparty).hasValue(StandardId.of("cpty", "a"));

    // Optional property with no value
    Result<?> tradeTime = evaluator.evaluate(trade, "tradeTime");
    assertThat(tradeTime).isFailure();

    // Unknown property
    Result<?> foo = evaluator.evaluate(trade, "foo");
    assertThat(foo).isFailure();
  }

  private static Trade trade() {
    SecurityLink<GenericFuture> securityLink = SecurityLink.resolvable(StandardId.of("foo", "1"), GenericFuture.class);
    TradeInfo tradeInfo = TradeInfo.builder()
        .counterparty(StandardId.of("cpty", "a"))
        .build();
    return GenericFutureTrade.builder()
        .securityLink(securityLink)
        .quantity(123)
        .initialPrice(456)
        .tradeInfo(tradeInfo)
        .build();
  }

}
