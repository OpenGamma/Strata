/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;

/**
 * Test {@link SecurityTokenEvaluator}.
 */
@Test
public class SecurityTokenEvaluatorTest {

  private static final SecurityId ID = SecurityId.of("OG-Test", "1");

  public void tokens() {
    SecurityTokenEvaluator evaluator = new SecurityTokenEvaluator();
    Set<String> tokens = evaluator.tokens(security());
    Set<String> expected = ImmutableSet.of(
        "id",
        "info",
        "currency",
        "priceInfo",
        "contractSize",
        "tickSize",
        "tickValue",
        "attributes");
    assertThat(tokens).isEqualTo(expected);
  }

  public void evaluate() {
    SecurityTokenEvaluator evaluator = new SecurityTokenEvaluator();
    Security sec = security();

    EvaluationResult quantity = evaluator.evaluate(sec, "id", ImmutableList.of());
    assertThat(quantity.getResult()).hasValue(ID);

    EvaluationResult initialPrice = evaluator.evaluate(sec, "currency", ImmutableList.of());
    assertThat(initialPrice.getResult()).hasValue(USD);

    // Check that property name isn't case sensitive
    EvaluationResult initialPrice2 = evaluator.evaluate(sec, "Currency", ImmutableList.of());
    assertThat(initialPrice2.getResult()).hasValue(USD);

    // Unknown property
    EvaluationResult foo = evaluator.evaluate(sec, "foo", ImmutableList.of());
    assertThat(foo.getResult()).isFailure();
  }

  private static Security security() {
    SecurityInfo info = SecurityInfo.of(ID, 20, CurrencyAmount.of(USD, 10));
    return GenericSecurity.of(info);
  }

}
