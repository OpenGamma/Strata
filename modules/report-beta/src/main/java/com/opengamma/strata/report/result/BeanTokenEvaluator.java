/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;

/**
 * Evaluates a token against a bean to produce another object.
 */
public class BeanTokenEvaluator implements TokenEvaluator<Bean> {

  @Override
  public Class<Bean> getTargetType() {
    return Bean.class;
  }

  @Override
  public Set<String> tokens(Bean bean) {
    return bean.propertyNames();
  }

  @Override
  public Object evaluate(Bean bean, String token) {
    Optional<String> propertyName = bean.propertyNames().stream()
        .filter(p -> p.toLowerCase().equals(token))
        .findFirst();
    if (propertyName.isPresent()) {
      return bean.property(propertyName.get()).get();
    }
    throw new TokenException(token, TokenError.INVALID, tokens(bean));
  }

}
