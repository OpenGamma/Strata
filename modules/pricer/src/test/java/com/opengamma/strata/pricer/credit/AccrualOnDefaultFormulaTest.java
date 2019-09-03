/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link AccrualOnDefaultFormula}.
 */
public class AccrualOnDefaultFormulaTest {

  public static Object[][] data_name() {
    return new Object[][] {
        {AccrualOnDefaultFormula.ORIGINAL_ISDA, "OriginalISDA"},
        {AccrualOnDefaultFormula.MARKIT_FIX, "MarkitFix"},
        {AccrualOnDefaultFormula.CORRECT, "Correct"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(AccrualOnDefaultFormula convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(AccrualOnDefaultFormula convention, String name) {
    assertThat(AccrualOnDefaultFormula.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(AccrualOnDefaultFormula convention, String name) {
    assertThat(AccrualOnDefaultFormula.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(AccrualOnDefaultFormula convention, String name) {
    assertThat(AccrualOnDefaultFormula.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> AccrualOnDefaultFormula.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> AccrualOnDefaultFormula.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(AccrualOnDefaultFormula.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(AccrualOnDefaultFormula.ORIGINAL_ISDA);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(AccrualOnDefaultFormula.class, AccrualOnDefaultFormula.MARKIT_FIX);
  }

}
