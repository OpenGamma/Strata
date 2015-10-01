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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

/**
 * Test {@link CurveCurrencyParameterSensitivitiesTokenEvaluator}.
 */
@Test
public class CurveCurrencyParameterSensitivitiesTokenEvaluatorTest {

  public void tokens() {
    CurveCurrencyParameterSensitivity sensitivity1 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve1"), Currency.AUD, new double[0]);

    CurveCurrencyParameterSensitivity sensitivity2 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve2"), Currency.CHF, new double[0]);

    CurveCurrencyParameterSensitivities sensitivities = CurveCurrencyParameterSensitivities.of(sensitivity1, sensitivity2);

    Set<String> expected = ImmutableSet.of("curve1", "curve2", "aud", "chf");
    CurveCurrencyParameterSensitivitiesTokenEvaluator evaluator = new CurveCurrencyParameterSensitivitiesTokenEvaluator();
    assertThat(evaluator.tokens(sensitivities)).isEqualTo(expected);
  }

  public void evaluate() {
    CurveCurrencyParameterSensitivity sensitivity1 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve1"), Currency.AUD, new double[0]);

    CurveCurrencyParameterSensitivity sensitivity2 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve2"), Currency.CHF, new double[0]);

    CurveCurrencyParameterSensitivity sensitivity3 =
        CurveCurrencyParameterSensitivity.of(DefaultCurveMetadata.of("Curve2"), Currency.AUD, new double[0]);

    CurveCurrencyParameterSensitivities sensitivities =
        CurveCurrencyParameterSensitivities.of(sensitivity1, sensitivity2, sensitivity3);

    CurveCurrencyParameterSensitivitiesTokenEvaluator evaluator = new CurveCurrencyParameterSensitivitiesTokenEvaluator();

    CurveCurrencyParameterSensitivities expected1 = CurveCurrencyParameterSensitivities.of(sensitivity1, sensitivity3);
    Result<?> result1 = evaluator.evaluate(sensitivities, "aud");
    assertThat(result1).isSuccess();
    CurveCurrencyParameterSensitivities result1Value = (CurveCurrencyParameterSensitivities) result1.getValue();
    assertThat(result1Value.getSensitivities()).containsAll(expected1.getSensitivities());

    CurveCurrencyParameterSensitivities expected2 = CurveCurrencyParameterSensitivities.of(sensitivity2, sensitivity3);
    Result<?> result2 = evaluator.evaluate(sensitivities, "curve2");
    assertThat(result2).isSuccess();
    CurveCurrencyParameterSensitivities result2Value = (CurveCurrencyParameterSensitivities) result2.getValue();
    assertThat(result2Value.getSensitivities()).containsAll(expected2.getSensitivities());

    Result<?> result3 = evaluator.evaluate(sensitivities, "chf");
    assertThat(result3).hasValue(sensitivity2);

    Result<?> result4 = evaluator.evaluate(sensitivities, "usd");
    assertThat(result4).isFailure();
  }
}
