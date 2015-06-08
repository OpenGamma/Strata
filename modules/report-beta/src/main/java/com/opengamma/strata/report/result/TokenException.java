/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.opengamma.strata.collect.Messages;

/**
 * Thrown to indicate that a token was not valid for a given target.
 */
public class TokenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new exception.
   * 
   * @param token  the invalid token
   * @param validTokens  the set of valid tokens
   */
  public TokenException(String token, TokenError tokenError, Set<String> validTokens) {
    super(getMessage(token, tokenError, validTokens));
  }

  private static String getMessage(String token, TokenError tokenError, Set<String> validTokens) {
    String errorMessage;
    switch (tokenError) {
      case AMBIGUOUS:
        errorMessage = "Ambiguous";
        break;
      default:
        errorMessage = "Invalid";
        break;
    }
    List<String> orderedValidTokens = new ArrayList<String>(validTokens);
    orderedValidTokens.sort(null);
    return Messages.format("{} field '{}'. Use one of: {}", 
        errorMessage, token, orderedValidTokens);
  }

}
