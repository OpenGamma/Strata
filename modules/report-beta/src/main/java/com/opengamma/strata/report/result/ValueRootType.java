/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.opengamma.strata.collect.Messages;

/**
 * Enumerates the possible value path roots.
 */
public enum ValueRootType {
  
  /**
   * Refers to the set of possible calculated measures.
   */
  MEASURES("Measures"),
  /**
   * Refers to the product on the trade.
   */
  PRODUCT("Product"),
  /**
   * Refers to the trade.
   */
  TRADE("Trade");
  
  //-------------------------------------------------------------------------
  private final String token;
  
  private ValueRootType(String token) {
    this.token = token;
  }
  
  /**
   * Gets the token that the root type corresponds to.
   * 
   * @return the token
   */
  public String token() {
    return token;
  }
  
  //-------------------------------------------------------------------------
  /**
   * Parses a string into the corresponding root type.
   * 
   * @param rootString  the token
   * @return the root type corresponding to the given string
   */
  public static ValueRootType parseToken(String rootString) {
    for (ValueRootType rootType : ValueRootType.values()) {
      if (rootType.token.toLowerCase().equals(rootString.toLowerCase())) {
        return rootType;
      }
    }
    List<String> validRoots = Arrays.stream(values())
        .map(r -> r.token)
        .collect(Collectors.toList());
    throw new IllegalArgumentException(
        Messages.format("Invalid root: {}. Value path must start with one of: {}", rootString, validRoots));
  }
  
}
