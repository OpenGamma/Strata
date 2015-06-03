/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import org.joda.beans.Bean;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.opengamma.strata.collect.Messages;

/**
 * Evaluates a path consisting of tokens against an object graph.
 */
public class TokenPathEvaluator {

  private final BeanTokenEvaluator beanTokenEvaluator = new BeanTokenEvaluator();

  private final ImmutableList<TokenEvaluator<?>> tokenEvaluators = ImmutableList.of(
      new CurrencyAmountTokenEvaluator(),
      new MapTokenEvaluator(),
      beanTokenEvaluator,
      new IterableTokenEvaluator());

  /**
   * Evaluates a token path against an object.
   * 
   * @param object  the object against which to evaluate the token path
   * @param tokenPath  the token path
   * @return the result of evaluating the token path against the object
   */
  public Object evaluate(Object object, List<String> tokenPath) {
    Object resultObject = object;
    Queue<String> tokenQueue = new LinkedList<String>(tokenPath);
    while (!tokenQueue.isEmpty()) {
      resultObject = evaluate(resultObject, tokenQueue);
    }
    return resultObject;
  }
  
  /**
   * Gets the supported tokens on the given object.
   * 
   * @param object  the object for which to return the valid tokens
   * @return the tokens
   */
  public Set<String> tokens(Object object) {
    // This must mirror the main evaluate method implementation
    Object evalObject = object;
    Set<String> tokens = new HashSet<String>();
    if (evalObject instanceof Bean) {
      Bean bean = (Bean) evalObject;
      if (bean.propertyNames().size() == 1) {
        String onlyProperty = Iterables.getOnlyElement(bean.propertyNames());
        tokens.add(onlyProperty);
        evalObject = bean.property(onlyProperty).get();
      }
    }
    Optional<TokenEvaluator<Object>> evaluator = getEvaluator(evalObject.getClass());
    if (evaluator.isPresent()) {
      tokens.addAll(evaluator.get().tokens(evalObject));
    }
    return tokens;
  }

  //-------------------------------------------------------------------------
  private Object evaluate(Object object, Queue<String> tokenPath) {
    if (object instanceof Bean) {
      Bean bean = (Bean) object;
      if (bean.propertyNames().size() == 1 && !beanTokenEvaluator.tokens(bean).contains(tokenPath.peek())) {
        // Pre-empt failure - allow the single property to be skipped over without consuming a token
        String singlePropertyName = Iterables.getOnlyElement(bean.propertyNames());
        return bean.property(singlePropertyName).get();
      }
    }
    Optional<TokenEvaluator<Object>> evaluator = getEvaluator(object.getClass());
    if (evaluator.isPresent()) {
      return evaluator.get().evaluate(object, tokenPath.remove().toLowerCase());
    }
    throw new UnsupportedOperationException(
        Messages.format("Unable to traverse type {}", object.getClass().getSimpleName()));
  }

  @SuppressWarnings("unchecked")
  private Optional<TokenEvaluator<Object>> getEvaluator(Class<?> targetClazz) {
    return tokenEvaluators.stream()
        .filter(e -> e.getTargetType().isAssignableFrom(targetClazz))
        .map(e -> (TokenEvaluator<Object>) e)
        .findFirst();
  }

}
