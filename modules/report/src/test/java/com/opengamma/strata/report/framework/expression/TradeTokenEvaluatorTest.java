/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.future.GenericFuture;
import com.opengamma.strata.product.future.GenericFutureTrade;

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

    EvaluationResult quantity = evaluator.evaluate(trade, "quantity", ImmutableList.of());
    assertThat(quantity.getResult()).hasValue(123L);

    EvaluationResult initialPrice = evaluator.evaluate(trade, "initialPrice", ImmutableList.of());
    assertThat(initialPrice.getResult()).hasValue(456d);

    // Check that property name isn't case sensitive
    EvaluationResult initialPrice2 = evaluator.evaluate(trade, "initialprice", ImmutableList.of());
    assertThat(initialPrice2.getResult()).hasValue(456d);

    EvaluationResult counterparty = evaluator.evaluate(trade, "counterparty", ImmutableList.of());
    assertThat(counterparty.getResult()).hasValue(StandardId.of("cpty", "a"));

    // Optional property with no value
    EvaluationResult tradeTime = evaluator.evaluate(trade, "tradeTime", ImmutableList.of());
    assertThat(tradeTime.getResult()).isFailure();

    // Unknown property
    EvaluationResult foo = evaluator.evaluate(trade, "foo", ImmutableList.of());
    assertThat(foo.getResult()).isFailure();
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
