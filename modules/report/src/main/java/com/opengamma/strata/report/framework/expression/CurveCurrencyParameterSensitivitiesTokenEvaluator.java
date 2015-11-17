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
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;

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
  public EvaluationResult evaluate(
      CurveCurrencyParameterSensitivities sensitivities,
      String firstToken,
      List<String> remainingTokens) {

    List<CurveCurrencyParameterSensitivity> matchingSensitivities = sensitivities.getSensitivities().stream()
        .filter(sensitivity -> matchesToken(sensitivity, firstToken))
        .collect(toImmutableList());

    switch (matchingSensitivities.size()) {
      case 0:
        return invalidTokenFailure(sensitivities, firstToken);
      case 1:
        return EvaluationResult.success(matchingSensitivities.get(0), remainingTokens);

      default:
        return EvaluationResult.success(CurveCurrencyParameterSensitivities.of(matchingSensitivities), remainingTokens);
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
    return token.equalsIgnoreCase(sensitivity.getCurrency().getCode()) ||
        token.equalsIgnoreCase(sensitivity.getCurveName().toString());
  }

}
