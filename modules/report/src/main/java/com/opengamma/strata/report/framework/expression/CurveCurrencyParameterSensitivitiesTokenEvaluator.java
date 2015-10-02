/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

/**
 * Evaluates a token against curve currency parameter sensitivities.
 * <p>
 * Tokens are matched against the curve name and currency code of the sensitivities. All strings are converted to
 * lower case before matching.
 */
public class CurveCurrencyParameterSensitivitiesTokenEvaluator
    extends TokenEvaluator<CurveCurrencyParameterSensitivities> {

  @Override
  public Class<?> getTargetType() {
    return CurveCurrencyParameterSensitivities.class;
  }

  @Override
  public Set<String> tokens(CurveCurrencyParameterSensitivities sensitivities) {
    return sensitivities.getSensitivities().stream()
        .flatMap(this::tokensForSensitivity)
        .collect(toImmutableSet());
  }

  @Override
  public Result<?> evaluate(CurveCurrencyParameterSensitivities sensitivities, String token) {
    List<CurveCurrencyParameterSensitivity> matchingSensitivities = sensitivities.getSensitivities().stream()
        .filter(sensitivity -> matchesToken(sensitivity, token))
        .collect(toImmutableList());

    switch (matchingSensitivities.size()) {
      case 0:
        return invalidTokenFailure(sensitivities, token);
      case 1:
        return Result.success(matchingSensitivities.get(0));
      default:
        return Result.success(CurveCurrencyParameterSensitivities.of(matchingSensitivities));
    }
  }

  private Stream<String> tokensForSensitivity(CurveCurrencyParameterSensitivity sensitivity) {
    return ImmutableSet.of(
        sensitivity.getCurrency().getCode(),
        sensitivity.getCurveName().toString())
        .stream()
        .map(String::toLowerCase);
  }

  private boolean matchesToken(CurveCurrencyParameterSensitivity sensitivity, String token) {
    return token.equals(sensitivity.getCurrency().getCode().toLowerCase()) ||
        token.equals(sensitivity.getCurveName().toString().toLowerCase());
  }
}
