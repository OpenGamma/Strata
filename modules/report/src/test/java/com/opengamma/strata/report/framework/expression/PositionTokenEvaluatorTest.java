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
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityPosition;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;

/**
 * Test {@link PositionTokenEvaluator}.
 */
@Test
public class PositionTokenEvaluatorTest {

  private static final CalculationFunctions FUNCTIONS = StandardComponents.calculationFunctions();

  private static final GenericSecurity SECURITY = GenericSecurity.of(
      SecurityInfo.of(SecurityId.of("OG-Test", "1"), 20, CurrencyAmount.of(USD, 10)));

  public void tokens() {
    PositionTokenEvaluator evaluator = new PositionTokenEvaluator();
    Set<String> tokens = evaluator.tokens(trade());
    Set<String> expected = ImmutableSet.of(
        "longQuantity",
        "shortQuantity",
        "quantity",
        "security",
        "info",
        "id",
        "attributes");
    assertThat(tokens).isEqualTo(expected);
  }

  public void evaluate() {
    PositionTokenEvaluator evaluator = new PositionTokenEvaluator();
    Position pos = trade();

    EvaluationResult quantity = evaluator.evaluate(pos, FUNCTIONS, "quantity", ImmutableList.of());
    assertThat(quantity.getResult()).hasValue(6d);

    EvaluationResult initialPrice = evaluator.evaluate(pos, FUNCTIONS, "security", ImmutableList.of());
    assertThat(initialPrice.getResult()).hasValue(SECURITY);

    // Check that property name isn't case sensitive
    EvaluationResult initialPrice2 = evaluator.evaluate(pos, FUNCTIONS, "Security", ImmutableList.of());
    assertThat(initialPrice2.getResult()).hasValue(SECURITY);

    // Unknown property
    EvaluationResult foo = evaluator.evaluate(pos, FUNCTIONS, "foo", ImmutableList.of());
    assertThat(foo.getResult()).isFailure();
  }

  private static Position trade() {
    PositionInfo info = PositionInfo.of(StandardId.of("OG-Position", "1"));
    return GenericSecurityPosition.ofNet(info, SECURITY, 6);
  }

}
