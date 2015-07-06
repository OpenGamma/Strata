/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.report.result.ValuePathEvaluator;

/**
 * Catch-all formatter that outputs the type of the value in angular brackets,
 * e.g. {@literal <MyCustomType>}, along with details of the valid tokens that could be used.
 */
public class UnsupportedValueFormatter implements ValueFormatter<Object> {

  /** Singleton instance. */
  public static final UnsupportedValueFormatter INSTANCE = new UnsupportedValueFormatter();

  private final ValuePathEvaluator valuePathEvaluator = new ValuePathEvaluator();

  @Override
  public String formatForCsv(Object object) {
    if (object instanceof double[]) {
      return Messages.format("<{}>: {}", object.getClass().getSimpleName(), formatDoubleArray(object));
    } else {
      return Messages.format("<{}>", object.getClass().getSimpleName());
    }

  }

  @Override
  public String formatForDisplay(Object object) {
    Set<String> validTokens = valuePathEvaluator.tokens(object);
    if (validTokens.isEmpty()) {
      if (object instanceof double[]) {
        return Messages.format("<{}> - {}",
            object.getClass().getSimpleName(),
            formatDoubleArray(object));
      } else {
        return Messages.format("<{}> - drilling into this type is not supported",
            object.getClass().getSimpleName());
      }
    } else {
      List<String> orderedTokens = new ArrayList<String>(validTokens);
      orderedTokens.sort(null);
      return Messages.format("<{}> - drill down using a field: {}",
          object.getClass().getSimpleName(), orderedTokens);
    }
  }

  private String formatDoubleArray(Object object) {
    // prepare a better error message for case where we have a vector of doubles
    // this is a common case and we want a descriptive error message where the
    // user can see the data in the array
    double[] data = (double[]) object;
    return Lists
        .newArrayList(DoubleArrayMath.toObject(data))
        .stream()
        .map(d -> String.valueOf(d))
        .collect(Collectors.joining(" ", "[", "]"));
  }

}
