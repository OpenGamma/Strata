/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        .map(k -> k.toString().toLowerCase())
        .collect(Collectors.toSet());
  }

  @Override
  public EvaluationResult evaluate(Map<?, ?> map, String firstToken, List<String> remainingTokens) {
    return map.entrySet().stream()
        .filter(e -> firstToken.equalsIgnoreCase(e.getKey().toString()))
        .findFirst()
        .map(e -> EvaluationResult.success(e.getValue(), remainingTokens))
        .orElse(invalidTokenFailure(map, firstToken));
  }
}
