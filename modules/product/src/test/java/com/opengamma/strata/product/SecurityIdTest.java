/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link SecurityId}.
 */
public class SecurityIdTest {

  private static final StandardId STANDARD_ID = StandardId.of("A", "1");
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @Test
  public void test_of_strings() {
    SecurityId test = SecurityId.of("A", "1");
    assertThat(test.getStandardId()).isEqualTo(STANDARD_ID);
    assertThat(test.getReferenceDataType()).isEqualTo(Security.class);
    assertThat(test.toString()).isEqualTo(STANDARD_ID.toString());
  }

  @Test
  public void test_of_standardId() {
    SecurityId test = SecurityId.of(STANDARD_ID);
    assertThat(test.getStandardId()).isEqualTo(STANDARD_ID);
    assertThat(test.getReferenceDataType()).isEqualTo(Security.class);
    assertThat(test.toString()).isEqualTo(STANDARD_ID.toString());
  }

  @Test
  public void test_parse() {
    SecurityId test = SecurityId.parse(STANDARD_ID.toString());
    assertThat(test.getStandardId()).isEqualTo(STANDARD_ID);
    assertThat(test.getReferenceDataType()).isEqualTo(Security.class);
    assertThat(test.toString()).isEqualTo(STANDARD_ID.toString());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    SecurityId a = SecurityId.of("A", "1");
    SecurityId a2 = SecurityId.of("A", "1");
    SecurityId b = SecurityId.of("B", "1");
    assertThat(a.equals(a)).isTrue();
    assertThat(a.equals(a2)).isTrue();
    assertThat(a.equals(b)).isFalse();
    assertThat(a.equals(null)).isFalse();
    assertThat(a.equals(ANOTHER_TYPE)).isFalse();
  }

}
