/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;

import com.google.common.collect.Sets;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;

/**
 * Evaluates a token against a trade to produce another object.
 * <p>
 * This merges the {@link Position} and {@link PositionInfo} objects, giving priority to {@code Position}.
 */
public class PositionTokenEvaluator extends TokenEvaluator<Position> {

  @Override
  public Class<Position> getTargetType() {
    return Position.class;
  }

  @Override
  public Set<String> tokens(Position position) {
    MetaBean metaBean = JodaBeanUtils.metaBean(position.getClass());
    return Sets.union(metaBean.metaPropertyMap().keySet(), position.getInfo().propertyNames());
  }

  @Override
  public EvaluationResult evaluate(
      Position position,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens) {

    MetaBean metaBean = JodaBeanUtils.metaBean(position.getClass());

    // position
    Optional<String> positionPropertyName = metaBean.metaPropertyMap().keySet().stream()
        .filter(p -> p.equalsIgnoreCase(firstToken))
        .findFirst();
    if (positionPropertyName.isPresent()) {
      Object propertyValue = metaBean.metaProperty(positionPropertyName.get()).get((Bean) position);
      return propertyValue != null ?
          EvaluationResult.success(propertyValue, remainingTokens) :
          EvaluationResult.failure("Property '{}' not set", firstToken);
    }

    // position info
    Optional<String> positionInfoPropertyName = position.getInfo().propertyNames().stream()
        .filter(p -> p.equalsIgnoreCase(firstToken))
        .findFirst();
    if (positionInfoPropertyName.isPresent()) {
      Object propertyValue = position.getInfo().property(positionInfoPropertyName.get()).get();
      return propertyValue != null ?
          EvaluationResult.success(propertyValue, remainingTokens) :
          EvaluationResult.failure("Property '{}' not set", firstToken);
    }

    // not found
    return invalidTokenFailure(position, firstToken);
  }

}
