/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.Arrays;
import java.util.List;

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
   * Refers to the security on the trade.
   */
  SECURITY("Security"),
  /**
   * Refers to the trade.
   */
  TRADE("Trade"),
  /**
   * Refers to the position.
   */
  POSITION("Position"),
  /**
   * Refers to the target (trade or position).
   */
  TARGET("Target");

  //-------------------------------------------------------------------------
  /**
   * The name of the token.
   */
  private final String token;

  /**
   * The complete set of valid roots.
   */
  private static final List<String> VALID_ROOTS = Arrays.stream(values())
      .map(r -> r.token)
      .collect(toImmutableList());

  /**
   * Creates an instance.
   * 
   * @param token  the root token name
   */
  ValueRootType(String token) {
    this.token = token;
  }

  //-------------------------------------------------------------------------
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
    return Arrays.stream(values())
        .filter(val -> val.token.equalsIgnoreCase(rootString))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            Messages.format("Invalid root: {}. Value path must start with one of: {}", rootString, VALID_ROOTS)));
  }

}
