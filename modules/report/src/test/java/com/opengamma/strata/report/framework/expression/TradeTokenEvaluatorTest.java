/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link TradeTokenEvaluator}.
 */
@Test
public class TradeTokenEvaluatorTest {

  private static final CalculationFunctions FUNCTIONS = StandardComponents.calculationFunctions();

  public void tokens() {
    TradeTokenEvaluator evaluator = new TradeTokenEvaluator();
    Set<String> tokens = evaluator.tokens(trade());
    Set<String> expected = ImmutableSet.of(
        "quantity",
        "price",
        "security",
        "id",
        "counterparty",
        "tradeDate",
        "tradeTime",
        "zone",
        "settlementDate",
        "attributes",
        "info");
    assertThat(tokens).isEqualTo(expected);
  }

  public void evaluate() {
    TradeTokenEvaluator evaluator = new TradeTokenEvaluator();
    Trade trade = trade();

    EvaluationResult quantity = evaluator.evaluate(trade, FUNCTIONS, "quantity", ImmutableList.of());
    assertThat(quantity.getResult()).hasValue(123d);

    EvaluationResult initialPrice = evaluator.evaluate(trade, FUNCTIONS, "price", ImmutableList.of());
    assertThat(initialPrice.getResult()).hasValue(456d);

    // Check that property name isn't case sensitive
    EvaluationResult initialPrice2 = evaluator.evaluate(trade, FUNCTIONS, "price", ImmutableList.of());
    assertThat(initialPrice2.getResult()).hasValue(456d);

    EvaluationResult counterparty = evaluator.evaluate(trade, FUNCTIONS, "counterparty", ImmutableList.of());
    assertThat(counterparty.getResult()).hasValue(StandardId.of("cpty", "a"));

    // Optional property with no value
    EvaluationResult tradeTime = evaluator.evaluate(trade, FUNCTIONS, "tradeTime", ImmutableList.of());
    assertThat(tradeTime.getResult()).isFailure();

    // Unknown property
    EvaluationResult foo = evaluator.evaluate(trade, FUNCTIONS, "foo", ImmutableList.of());
    assertThat(foo.getResult()).isFailure();
  }

  private static Trade trade() {
    SecurityInfo info = SecurityInfo.of(SecurityId.of("OG-Test", "1"), 20, CurrencyAmount.of(USD, 10));
    GenericSecurity security = GenericSecurity.of(info);
    TradeInfo tradeInfo = TradeInfo.builder()
        .counterparty(StandardId.of("cpty", "a"))
        .build();
    return GenericSecurityTrade.builder()
        .info(tradeInfo)
        .security(security)
        .quantity(123)
        .price(456)
        .build();
  }

}
