/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import com.google.common.base.CharMatcher;

/**
 * Useful Guava character matchers.
 */
public final class CharMatchers {

  private static final CharMatcher UPPER_LETTERS = CharMatcher.inRange('A', 'Z');
  private static final CharMatcher LOWER_LETTERS = CharMatcher.inRange('a', 'z');
  private static final CharMatcher LETTERS = UPPER_LETTERS.or(LOWER_LETTERS);
  private static final CharMatcher DIGITS = CharMatcher.inRange('0', '9');
  private static final CharMatcher LETTERS_DIGITS = LETTERS.or(DIGITS);
  private static final CharMatcher UPPER_HEX = DIGITS.or(CharMatcher.inRange('A', 'F'));
  private static final CharMatcher LOWER_HEX = DIGITS.or(CharMatcher.inRange('a', 'f'));
  private static final CharMatcher HEX = UPPER_HEX.or(LOWER_HEX);

  /**
   * Restricted constructor.
   */
  private CharMatchers() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the matcher for ASCII upper case letters.
   *
   * @return the matcher for ASCII upper case letters
   */
  public static CharMatcher upperLetters() {
    return UPPER_LETTERS;
  }

  /**
   * Returns the matcher for ASCII upper case letters.
   *
   * @return the matcher for ASCII upper case letters
   */
  public static CharMatcher lowerLetters() {
    return LOWER_LETTERS;
  }

  /**
   * Returns the matcher for ASCII letters.
   *
   * @return the matcher for ASCII letters
   */
  public static CharMatcher letters() {
    return LETTERS;
  }

  /**
   * Returns the matcher for ASCII digits.
   *
   * @return the matcher for ASCII digits
   */
  public static CharMatcher digits() {
    return DIGITS;
  }

  /**
   * Returns the matcher for ASCII letters and digits.
   *
   * @return the matcher for ASCII letters and digits
   */
  public static CharMatcher lettersAndDigits() {
    return LETTERS_DIGITS;
  }

  /**
   * Returns the matcher for ASCII upper case hex characters.
   *
   * @return the matcher for ASCII upper case hex characters
   */
  public static CharMatcher upperHex() {
    return UPPER_HEX;
  }

  /**
   * Returns the matcher for ASCII upper case hex characters.
   *
   * @return the matcher for ASCII upper case hex characters
   */
  public static CharMatcher lowerHex() {
    return LOWER_HEX;
  }

  /**
   * Returns the matcher for ASCII hex characters.
   *
   * @return the matcher for ASCII hex characters
   */
  public static CharMatcher hex() {
    return HEX;
  }

}
