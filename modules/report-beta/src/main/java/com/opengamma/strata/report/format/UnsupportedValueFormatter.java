/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.report.result.TokenPathEvaluator;

/**
 * Catch-all formatter that outputs the type of the value in angular brackets,
 * e.g. {@literal <MyCustomType>}, along with details of the valid tokens that could be used.
 */
public class UnsupportedValueFormatter implements ValueFormatter<Object> {

  /** Singleton instance. */
  public static final UnsupportedValueFormatter INSTANCE = new UnsupportedValueFormatter();

  private final TokenPathEvaluator tokenPathEvaluator = new TokenPathEvaluator();
  
  @Override
  public String formatForCsv(Object object) {
    return Messages.format("<{}>", object.getClass().getSimpleName());
  }

  @Override
  public String formatForDisplay(Object object) {
    Set<String> validTokens = tokenPathEvaluator.tokens(object);
    if (validTokens.isEmpty()) {
      return Messages.format("<{}> - drilling into this type is not supported",
          object.getClass().getSimpleName());
    } else {
      List<String> orderedTokens = new ArrayList<String>(validTokens);
      orderedTokens.sort(null);
      return Messages.format("<{}> - drill down using a field: {}",
          object.getClass().getSimpleName(), orderedTokens);
    }

  }

}
