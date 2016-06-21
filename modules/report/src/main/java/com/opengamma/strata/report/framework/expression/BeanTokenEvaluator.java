/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.market.amount.LegAmount;
import com.opengamma.strata.market.amount.LegAmounts;

/**
 * Evaluates a token against a bean to produce another object.
 * <p>
 * The token must be the name of one of the properties of the bean and the result is the value of the property.
 * <p>
 * There is special handling of beans with a single property. The name of the property can be omitted from
 * the expression if the bean only has one property.
 * <p>
 * For example, the bean {@link LegAmounts} has a single property named {@code amounts} containing a list of
 * {@link LegAmount} instances. The following expressions are equivalent and both return the first amount in the
 * list. {@code LegInitialNotional} is a measure that produces {@code LegAmounts}.
 * <pre>
 *   Measures.LegInitialNotional.0
 *   Measures.LegInitialNotional.amounts.0
 * </pre>
 * <p>
 * If the token matches the property then the default behaviour applies; the property value is returned and
 * the remaining tokens do not include the property token. If the token doesn't match the property, the property value
 * is returned but the token isn't consumed. i.e. the remaining tokens returned from {@link #evaluate} include
 * the first token.
 */
public class BeanTokenEvaluator extends TokenEvaluator<Bean> {

  @Override
  public Class<Bean> getTargetType() {
    return Bean.class;
  }

  @Override
  public Set<String> tokens(Bean bean) {
    if (bean.propertyNames().size() == 1) {
      String singlePropertyName = Iterables.getOnlyElement(bean.propertyNames());
      Object propertyValue = bean.property(singlePropertyName).get();
      Set<String> valueTokens = ValuePathEvaluator.tokens(propertyValue);

      return ImmutableSet.<String>builder()
          .add(singlePropertyName)
          .addAll(valueTokens)
          .build();
    } else {
      return bean.propertyNames();
    }
  }

  @Override
  public EvaluationResult evaluate(
      Bean bean,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens) {

    Optional<String> propertyName = bean.propertyNames().stream()
        .filter(p -> p.equalsIgnoreCase(firstToken))
        .findFirst();

    if (propertyName.isPresent()) {
      Object propertyValue = bean.property(propertyName.get()).get();

      return propertyValue != null ?
          EvaluationResult.success(propertyValue, remainingTokens) :
          EvaluationResult.failure("No value available for property '{}'", firstToken);
    }
    // The bean has a single property which doesn't match the token.
    // Return the property value without consuming any tokens.
    // This allows skipping over properties when the bean only has a single property.
    if (bean.propertyNames().size() == 1) {
      String singlePropertyName = Iterables.getOnlyElement(bean.propertyNames());
      Object propertyValue = bean.property(singlePropertyName).get();
      List<String> tokens = ImmutableList.<String>builder().add(firstToken).addAll(remainingTokens).build();

      return propertyValue != null ?
          EvaluationResult.success(propertyValue, tokens) :
          EvaluationResult.failure("No value available for property '{}'", firstToken);
    }
    return invalidTokenFailure(bean, firstToken);
  }

}
