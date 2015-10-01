/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * Evaluates a token against a bean to produce another object.
 */
public class BeanTokenEvaluator
    extends TokenEvaluator<Bean> {

  @Override
  public Class<Bean> getTargetType() {
    return Bean.class;
  }

  @Override
  public Set<String> tokens(Bean bean) {
    return bean.propertyNames();
  }

  @Override
  public Result<?> evaluate(Bean bean, String token) {
    Optional<String> propertyName = bean.propertyNames().stream()
        .filter(p -> p.toLowerCase().equals(token))
        .findFirst();

    if (propertyName.isPresent()) {
      Object propertyValue = bean.property(propertyName.get()).get();
      return propertyValue != null ?
          Result.success(propertyValue) :
          Result.failure(FailureReason.INVALID_INPUT, Messages.format("No value available for property '{}'", token));
    }
    return invalidTokenFailure(bean, token);
  }

}
