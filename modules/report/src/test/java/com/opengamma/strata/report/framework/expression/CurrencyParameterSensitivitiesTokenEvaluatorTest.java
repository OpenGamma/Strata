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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.measure.StandardComponents;

/**
 * Test {@link CurrencyParameterSensitivitiesTokenEvaluator}.
 */
@Test
public class CurrencyParameterSensitivitiesTokenEvaluatorTest {

  private static final CalculationFunctions FUNCTIONS = StandardComponents.calculationFunctions();

  public void tokens() {
    CurrencyParameterSensitivity sensitivity1 =
        CurrencyParameterSensitivity.of(CurveName.of("curve1"), Currency.AUD, DoubleArray.EMPTY);

    CurrencyParameterSensitivity sensitivity2 =
        CurrencyParameterSensitivity.of(CurveName.of("curve2"), Currency.CHF, DoubleArray.EMPTY);

    CurrencyParameterSensitivities sensitivities = CurrencyParameterSensitivities.of(sensitivity1, sensitivity2);

    Set<String> expected = ImmutableSet.of("curve1", "curve2", "aud", "chf");
    CurrencyParameterSensitivitiesTokenEvaluator evaluator = new CurrencyParameterSensitivitiesTokenEvaluator();
    assertThat(evaluator.tokens(sensitivities)).isEqualTo(expected);
  }

  public void evaluate() {
    CurrencyParameterSensitivity sensitivity1 =
        CurrencyParameterSensitivity.of(CurveName.of("curve1"), Currency.AUD, DoubleArray.EMPTY);

    CurrencyParameterSensitivity sensitivity2 =
        CurrencyParameterSensitivity.of(CurveName.of("curve2"), Currency.CHF, DoubleArray.EMPTY);

    CurrencyParameterSensitivity sensitivity3 =
        CurrencyParameterSensitivity.of(CurveName.of("curve2"), Currency.AUD, DoubleArray.EMPTY);

    CurrencyParameterSensitivities sensitivities =
        CurrencyParameterSensitivities.of(sensitivity1, sensitivity2, sensitivity3);

    CurrencyParameterSensitivitiesTokenEvaluator evaluator = new CurrencyParameterSensitivitiesTokenEvaluator();

    CurrencyParameterSensitivities expected1 = CurrencyParameterSensitivities.of(sensitivity1, sensitivity3);
    EvaluationResult result1 = evaluator.evaluate(sensitivities, FUNCTIONS, "aud", ImmutableList.of());
    assertThat(result1.getResult()).isSuccess();
    CurrencyParameterSensitivities result1Value = (CurrencyParameterSensitivities) result1.getResult().getValue();
    assertThat(result1Value.getSensitivities()).containsAll(expected1.getSensitivities());

    CurrencyParameterSensitivities expected2 = CurrencyParameterSensitivities.of(sensitivity2, sensitivity3);
    EvaluationResult result2 = evaluator.evaluate(sensitivities, FUNCTIONS, "curve2", ImmutableList.of());
    assertThat(result2.getResult()).isSuccess();
    CurrencyParameterSensitivities result2Value = (CurrencyParameterSensitivities) result2.getResult().getValue();
    assertThat(result2Value.getSensitivities()).containsAll(expected2.getSensitivities());

    EvaluationResult result3 = evaluator.evaluate(sensitivities, FUNCTIONS, "chf", ImmutableList.of());
    assertThat(result3.getResult()).hasValue(sensitivity2);

    EvaluationResult result4 = evaluator.evaluate(sensitivities, FUNCTIONS, "usd", ImmutableList.of());
    assertThat(result4.getResult()).isFailure();
  }
}
