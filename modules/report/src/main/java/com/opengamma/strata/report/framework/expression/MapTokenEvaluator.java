/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.MapStream;

/**
 * Evaluates a token against a map.
 */
public class MapTokenEvaluator extends TokenEvaluator<Map<?, ?>> {

  @Override
  public Class<?> getTargetType() {
    return Map.class;
  }

  @Override
  public Set<String> tokens(Map<?, ?> map) {
    return map.keySet().stream()
        .map(k -> k.toString().toLowerCase(Locale.ENGLISH))
        .collect(Collectors.toSet());
  }

  @Override
  public EvaluationResult evaluate(
      Map<?, ?> map,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens) {

    return MapStream.of(map)
        .filterKeys(key -> firstToken.equalsIgnoreCase(key.toString()))
        .findFirst()
        .map(e -> EvaluationResult.success(e.getValue(), remainingTokens))
        .orElse(invalidTokenFailure(map, firstToken));
  }
}
