/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.Property;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Ints;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 * Evaluates a token against an iterable object and returns a value.
 * <p>
 * The token can be the index of the item in the iterable (zero based). For example, this expression selects
 * the start date of the first leg of a swap:
 * <pre>
 *   Product.legs.0.startDate
 * </pre>
 * It is also possible to select items based on the value of their properties. For example, {@link SwapLeg} has
 * a property {@code payReceive} whose value can be {@code PAY} or {@code RECEIVE}. It is possible to select
 * a leg based on the value of this property:
 * <pre>
 *   Product.legs.pay.startDate     // Pay leg start date
 *   Product.legs.receive.startDate // Receive leg start date
 * </pre>
 * The comparison between property values and expression values is case-insensitive.
 * <p>
 * This works for any property where each item has a unique value. For example, consider a cross-currency swap where
 * one leg has the currency USD and the other has the currency GBP:
 * <pre>
 *   Product.legs.USD.startDate // USD leg start date
 *   Product.legs.GBP.startDate // GBP leg start date
 * </pre>
 * If both legs have the same currency it would obviously not be possible to use the currency to select a leg.
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
        .collect(toImmutableSet());
  }

  @Override
  public EvaluationResult evaluate(
      Iterable<?> iterable,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens) {

    String token = firstToken.toLowerCase(Locale.ENGLISH);
    Integer index = Ints.tryParse(token);

    if (index != null) {
      try {
        return EvaluationResult.success(Iterables.get(iterable, index), remainingTokens);
      } catch (IndexOutOfBoundsException e) {
        return invalidTokenFailure(iterable, token);
      }
    }
    Set<String> tokens = tokens(iterable);

    for (Object item : iterable) {
      if (!fieldValues(item).contains(token)) {
        continue;
      }
      if (!tokens.contains(token)) {
        return ambiguousTokenFailure(iterable, token);
      }
      return EvaluationResult.success(item, remainingTokens);
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
        .map(bean::property)
        .filter(p -> SUPPORTED_FIELD_TYPES.contains(p.metaProperty().propertyType()))
        .map(Property::get)
        .filter(v -> v != null)
        .map(Object::toString)
        .map(v -> v.toLowerCase(Locale.ENGLISH))
        .collect(toImmutableSet());
  }

}
