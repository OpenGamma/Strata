/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.Set;
import java.util.stream.Collectors;

import org.joda.beans.Bean;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.finance.rate.swap.SwapLegType;

/**
 * 
 */
public class IterableTokenEvaluator implements TokenEvaluator<Iterable<?>> {

  private static final Set<Class<?>> SUPPORTED_FIELD_TYPES = ImmutableSet.of(
      Currency.class,
      SwapLegType.class,
      PayReceive.class);

  @Override
  public Class<?> getTargetType() {
    return Iterable.class;
  }

  @Override
  public Set<String> tokens(Iterable<?> iterable) {
    Multiset<String> tokens = HashMultiset.create();
    int index = 1;
    for (Object item : iterable) {
      tokens.add(String.valueOf(index++));
      tokens.addAll(fieldValues(item));
    }
    return tokens.stream()
        .filter(token -> tokens.count(token) == 1)
        .collect(Collectors.toSet());
  }

  @Override
  public Object evaluate(Iterable<?> iterable, String token) {
    for (Object item : iterable) {
      if (!fieldValues(item).contains(token)) {
        continue;
      }
      if (!tokens(iterable).contains(token)) {
        throw new TokenException(token, TokenError.AMBIGUOUS, tokens(iterable));
      }
      return item;
    }
    throw new TokenException(token, TokenError.INVALID, tokens(iterable));
  }

  //-------------------------------------------------------------------------
  private Set<String> fieldValues(Object object) {
    if (!(object instanceof Bean)) {
      return ImmutableSet.of();
    }
    Bean bean = (Bean) object;
    return bean.propertyNames().stream()
        .map(name -> bean.property(name))
        .filter(p -> SUPPORTED_FIELD_TYPES.contains(p.metaProperty().propertyType()))
        .map(p -> p.get())
        .filter(v -> v != null)
        .map(v -> v.toString().toLowerCase())
        .collect(Collectors.toSet());
  }

}
