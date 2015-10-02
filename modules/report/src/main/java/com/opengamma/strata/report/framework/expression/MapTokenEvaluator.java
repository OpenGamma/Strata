/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.opengamma.strata.collect.result.Result;

/**
 * Evaluates a token against a map.
 */
public class MapTokenEvaluator
    extends TokenEvaluator<Map<?, ?>> {

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
  public Result<?> evaluate(Map<?, ?> map, String token) {
    for (Object key : map.keySet()) {
      if (token.equals(key.toString().toLowerCase())) {
        return Result.success(map.get(key));
      }
    }
    return invalidTokenFailure(map, token);
  }

}
