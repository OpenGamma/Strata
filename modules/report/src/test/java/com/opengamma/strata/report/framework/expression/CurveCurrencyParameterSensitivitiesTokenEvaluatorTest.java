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
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;

/**
 * Test {@link CurveCurrencyParameterSensitivitiesTokenEvaluator}.
 */
@Test
public class CurveCurrencyParameterSensitivitiesTokenEvaluatorTest {

  public void tokens() {
    CurveCurrencyParameterSensitivity sensitivity1 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve1"), Currency.AUD, DoubleArray.EMPTY);

    CurveCurrencyParameterSensitivity sensitivity2 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve2"), Currency.CHF, DoubleArray.EMPTY);

    CurveCurrencyParameterSensitivities sensitivities = CurveCurrencyParameterSensitivities.of(sensitivity1, sensitivity2);

    Set<String> expected = ImmutableSet.of("curve1", "curve2", "aud", "chf");
    CurveCurrencyParameterSensitivitiesTokenEvaluator evaluator = new CurveCurrencyParameterSensitivitiesTokenEvaluator();
    assertThat(evaluator.tokens(sensitivities)).isEqualTo(expected);
  }

  public void evaluate() {
    CurveCurrencyParameterSensitivity sensitivity1 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve1"), Currency.AUD, DoubleArray.EMPTY);

    CurveCurrencyParameterSensitivity sensitivity2 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve2"), Currency.CHF, DoubleArray.EMPTY);

    CurveCurrencyParameterSensitivity sensitivity3 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve2"), Currency.AUD, DoubleArray.EMPTY);

    CurveCurrencyParameterSensitivities sensitivities =
        CurveCurrencyParameterSensitivities.of(sensitivity1, sensitivity2, sensitivity3);

    CurveCurrencyParameterSensitivitiesTokenEvaluator evaluator = new CurveCurrencyParameterSensitivitiesTokenEvaluator();

    CurveCurrencyParameterSensitivities expected1 = CurveCurrencyParameterSensitivities.of(sensitivity1, sensitivity3);
    EvaluationResult result1 = evaluator.evaluate(sensitivities, "aud", ImmutableList.of());
    assertThat(result1.getResult()).isSuccess();
    CurveCurrencyParameterSensitivities result1Value = (CurveCurrencyParameterSensitivities) result1.getResult().getValue();
    assertThat(result1Value.getSensitivities()).containsAll(expected1.getSensitivities());

    CurveCurrencyParameterSensitivities expected2 = CurveCurrencyParameterSensitivities.of(sensitivity2, sensitivity3);
    EvaluationResult result2 = evaluator.evaluate(sensitivities, "curve2", ImmutableList.of());
    assertThat(result2.getResult()).isSuccess();
    CurveCurrencyParameterSensitivities result2Value = (CurveCurrencyParameterSensitivities) result2.getResult().getValue();
    assertThat(result2Value.getSensitivities()).containsAll(expected2.getSensitivities());

    EvaluationResult result3 = evaluator.evaluate(sensitivities, "chf", ImmutableList.of());
    assertThat(result3.getResult()).hasValue(sensitivity2);

    EvaluationResult result4 = evaluator.evaluate(sensitivities, "usd", ImmutableList.of());
    assertThat(result4.getResult()).isFailure();
  }
}
