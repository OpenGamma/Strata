/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;

/**
 * Evaluates a token against currency parameter sensitivities.
 * <p>
 * Tokens are matched against the name and currency code of the sensitivities.
 * All strings are converted to lower case before matching.
 */
public class CurrencyParameterSensitivitiesTokenEvaluator
    extends TokenEvaluator<CurrencyParameterSensitivities> {

  @Override
  public Class<?> getTargetType() {
    return CurrencyParameterSensitivities.class;
  }

  @Override
  public Set<String> tokens(CurrencyParameterSensitivities sensitivities) {
    return sensitivities.getSensitivities().stream()
        .flatMap(this::tokensForSensitivity)
        .collect(toImmutableSet());
  }

  @Override
  public EvaluationResult evaluate(
      CurrencyParameterSensitivities sensitivities,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens) {

    List<CurrencyParameterSensitivity> matchingSensitivities = sensitivities.getSensitivities().stream()
        .filter(sensitivity -> matchesToken(sensitivity, firstToken))
        .collect(toImmutableList());

    switch (matchingSensitivities.size()) {
      case 0:
        return invalidTokenFailure(sensitivities, firstToken);
      case 1:
        return EvaluationResult.success(matchingSensitivities.get(0), remainingTokens);

      default:
        return EvaluationResult.success(CurrencyParameterSensitivities.of(matchingSensitivities), remainingTokens);
    }
  }

  private Stream<String> tokensForSensitivity(CurrencyParameterSensitivity sensitivity) {
    return ImmutableSet.of(
        sensitivity.getCurrency().getCode(),
        sensitivity.getMarketDataName().getName())
        .stream()
        .map(v -> v.toLowerCase(Locale.ENGLISH));
  }

  private boolean matchesToken(CurrencyParameterSensitivity sensitivity, String token) {
    return token.equalsIgnoreCase(sensitivity.getCurrency().getCode()) ||
        token.equalsIgnoreCase(sensitivity.getMarketDataName().getName());
  }

}
