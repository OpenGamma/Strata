/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.joda.beans.Bean;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.finance.rate.swap.SwapLegType;

/**
 * 
 */
public class IterableTraverser implements TokenEvaluator<Iterable<?>> {

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
    Stream<String> listIndexStream = IntStream.range(0, Iterables.size(iterable)).mapToObj(i -> String.valueOf(i));
    Stream<String> listItemValuesStream = StreamSupport.stream(iterable.spliterator(), false)
        .flatMap(i -> fieldValues(i).stream());
    return Stream.concat(listIndexStream, listItemValuesStream).collect(Collectors.toSet());
  }

  @Override
  public Object evaluate(Iterable<?> iterable, String token) {
    for (Object item : iterable) {
      if (fieldValues(item).contains(token)) {
        return item;
      }
    }
    throw new InvalidTokenException(token, iterable.getClass());
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
