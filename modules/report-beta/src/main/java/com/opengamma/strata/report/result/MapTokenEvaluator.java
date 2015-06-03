/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 
 */
public class MapTokenEvaluator implements TokenEvaluator<Map<?, ?>> {

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
  public Object evaluate(Map<?, ?> map, String token) {
    for (Object key : map.keySet()) {
      if (token.equals(key.toString().toLowerCase())) {
        return map.get(key);
      }
    }
    throw new TokenException(token, TokenError.INVALID, tokens(map));
  }

}
