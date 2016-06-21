/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.joda.beans.Bean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.market.amount.LegAmounts;
import com.opengamma.strata.market.amount.SwapLegAmount;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.swap.SwapLegType;

@Test
public class BeanTokenEvaluatorTest {

  private static final CalculationFunctions FUNCTIONS = StandardComponents.calculationFunctions();

  public void evaluate() {
    Bean bean = bean();
    BeanTokenEvaluator evaluator = new BeanTokenEvaluator();

    EvaluationResult notional1 = evaluator.evaluate(bean, FUNCTIONS, "notional", ImmutableList.of());
    assertThat(notional1.getResult()).hasValue(1_000_000d);

    EvaluationResult notional2 = evaluator.evaluate(bean, FUNCTIONS, "Notional", ImmutableList.of());
    assertThat(notional2.getResult()).hasValue(1_000_000d);
  }

  public void tokens() {
    Bean bean = bean();
    BeanTokenEvaluator evaluator = new BeanTokenEvaluator();

    Set<String> tokens = evaluator.tokens(bean);
    ImmutableSet<String> expectedTokens = ImmutableSet.of(
        "buySell",
        "currency",
        "notional",
        "startDate",
        "endDate",
        "businessDayAdjustment",
        "paymentDate",
        "fixedRate",
        "index",
        "indexInterpolated",
        "fixingDateOffset",
        "dayCount",
        "discounting");

    assertThat(tokens).isEqualTo(expectedTokens);
  }

  /**
   * Tests evaluating a bean with a single property. There are 2 different expected behaviours:
   *
   * 1) If the token matches the property, the property value is returned and the token is consumed. This is the same
   *    as the normal bean behaviour.
   * 2) If the token doesn't match the property it is assumed to match something on the property's value. In this
   *    case the property value is returned and no tokens are consumed.
   */
  public void evaluateSingleProperty() {
    SwapLegAmount amount = SwapLegAmount.builder()
        .amount(CurrencyAmount.of(Currency.AUD, 7))
        .payReceive(PayReceive.PAY)
        .type(SwapLegType.FIXED)
        .currency(Currency.AUD)
        .build();
    LegAmounts amounts = LegAmounts.of(amount);
    BeanTokenEvaluator evaluator = new BeanTokenEvaluator();

    EvaluationResult result1 = evaluator.evaluate(amounts, FUNCTIONS, "amounts", ImmutableList.of("foo", "bar"));
    assertThat(result1.getResult()).hasValue(ImmutableList.of(amount));
    assertThat(result1.getRemainingTokens()).isEqualTo(ImmutableList.of("foo", "bar"));

    EvaluationResult result2 = evaluator.evaluate(amounts, FUNCTIONS, "baz", ImmutableList.of("foo", "bar"));
    assertThat(result2.getResult()).hasValue(ImmutableList.of(amount));
    assertThat(result2.getRemainingTokens()).isEqualTo(ImmutableList.of("baz", "foo", "bar"));
  }

  /**
   * Tests the tokens() method when the bean has a single property. The tokens should include the single property
   * name plus the tokens of the property value.
   */
  public void tokensSingleProperty() {
    SwapLegAmount amount = SwapLegAmount.builder()
        .amount(CurrencyAmount.of(Currency.AUD, 7))
        .payReceive(PayReceive.PAY)
        .type(SwapLegType.FIXED)
        .currency(Currency.AUD)
        .build();
    LegAmounts amounts = LegAmounts.of(amount);
    BeanTokenEvaluator evaluator = new BeanTokenEvaluator();

    Set<String> tokens = evaluator.tokens(amounts);
    assertThat(tokens).isEqualTo(ImmutableSet.of("amounts", "0", "aud", "pay", "fixed"));
  }

  private static Bean bean() {
    return Fra.builder()
        .buySell(BUY)
        .notional(1_000_000)
        .startDate(date(2015, 8, 5))
        .endDate(date(2015, 11, 5))
        .paymentDate(AdjustableDate.of(date(2015, 8, 7)))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
  }
}
