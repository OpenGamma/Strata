/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test CharMatchers.
 */
public class CharMatchersTest {

  @Test
  public void test_upperLetters() {
    assertThat(CharMatchers.upperLetters().matchesAllOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    assertThat(CharMatchers.upperLetters().matchesNoneOf("abcdefghijklmnopqrstuvwxyz"));
    assertThat(CharMatchers.upperLetters().matchesNoneOf("0123456789"));
    assertThat(CharMatchers.upperLetters().matchesNoneOf(" -_!\"£$%^&*()\\|,<.>/?;:'@#~[{]}=+"));
  }

  @Test
  public void test_lowerLetters() {
    assertThat(CharMatchers.lowerLetters().matchesNoneOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    assertThat(CharMatchers.lowerLetters().matchesAllOf("abcdefghijklmnopqrstuvwxyz"));
    assertThat(CharMatchers.lowerLetters().matchesNoneOf("0123456789"));
    assertThat(CharMatchers.lowerLetters().matchesNoneOf(" -_!\"£$%^&*()\\|,<.>/?;:'@#~[{]}=+"));
  }

  @Test
  public void test_letters() {
    assertThat(CharMatchers.letters().matchesAllOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    assertThat(CharMatchers.letters().matchesAllOf("abcdefghijklmnopqrstuvwxyz"));
    assertThat(CharMatchers.letters().matchesNoneOf("0123456789"));
    assertThat(CharMatchers.letters().matchesNoneOf(" -_!\"£$%^&*()\\|,<.>/?;:'@#~[{]}=+"));
  }

  @Test
  public void test_digits() {
    assertThat(CharMatchers.digits().matchesNoneOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    assertThat(CharMatchers.digits().matchesNoneOf("abcdefghijklmnopqrstuvwxyz"));
    assertThat(CharMatchers.digits().matchesAllOf("0123456789"));
    assertThat(CharMatchers.digits().matchesNoneOf(" -_!\"£$%^&*()\\|,<.>/?;:'@#~[{]}=+"));
  }

  @Test
  public void test_lettersAndDigits() {
    assertThat(CharMatchers.lettersAndDigits().matchesAllOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    assertThat(CharMatchers.lettersAndDigits().matchesAllOf("abcdefghijklmnopqrstuvwxyz"));
    assertThat(CharMatchers.lettersAndDigits().matchesAllOf("0123456789"));
    assertThat(CharMatchers.lettersAndDigits().matchesNoneOf(" -_!\"£$%^&*()\\|,<.>/?;:'@#~[{]}=+"));
  }

  @Test
  public void test_upperHex() {
    assertThat(CharMatchers.upperHex().matchesAllOf("0123456789ABCDEF"));
    assertThat(CharMatchers.upperHex().matchesNoneOf("abcdef"));
    assertThat(CharMatchers.upperHex().matchesNoneOf(" -_!\"£$%^&*()\\|,<.>/?;:'@#~[{]}=+"));
  }

  @Test
  public void test_lowerHex() {
    assertThat(CharMatchers.lowerHex().matchesAllOf("0123456789abcdef"));
    assertThat(CharMatchers.lowerHex().matchesNoneOf("ABCDEF"));
    assertThat(CharMatchers.lowerHex().matchesNoneOf(" -_!\"£$%^&*()\\|,<.>/?;:'@#~[{]}=+"));
  }

  @Test
  public void test_hex() {
    assertThat(CharMatchers.hex().matchesAllOf("0123456789abcdefABCDEF"));
    assertThat(CharMatchers.hex().matchesNoneOf(" -_!\"£$%^&*()\\|,<.>/?;:'@#~[{]}=+"));
  }

}
