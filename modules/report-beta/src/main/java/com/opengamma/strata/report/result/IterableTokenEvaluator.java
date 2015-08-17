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
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Ints;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.finance.rate.swap.SwapLegType;

/**
 * 
 */
public class IterableTokenEvaluator extends TokenEvaluator<Iterable<?>> {

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
    int index = 0;
    for (Object item : iterable) {
      tokens.add(String.valueOf(index++));
      tokens.addAll(fieldValues(item));
    }
    return tokens.stream()
        .filter(token -> tokens.count(token) == 1)
        .collect(Collectors.toSet());
  }

  @Override
  public Result<?> evaluate(Iterable<?> iterable, String token) {
    Integer index = Ints.tryParse(token);
    if (index != null) {
      try {
        return Result.success(Iterables.get(iterable, index));
      } catch (IndexOutOfBoundsException e) {
        return invalidTokenFailure(iterable, token);
      }
    }
    for (Object item : iterable) {
      if (!fieldValues(item).contains(token)) {
        continue;
      }
      if (!tokens(iterable).contains(token)) {
        return ambiguousTokenFailure(iterable, token);
      }
      return Result.success(item);
    }
    return invalidTokenFailure(iterable, token);
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
