/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link LegalEntityId}.
 */
public class LegalEntityIdTest {

  private static final StandardId STANDARD_ID = StandardId.of("A", "1");
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @Test
  public void test_of_strings() {
    LegalEntityId test = LegalEntityId.of("A", "1");
    assertThat(test.getStandardId()).isEqualTo(STANDARD_ID);
    assertThat(test.getReferenceDataType()).isEqualTo(LegalEntity.class);
    assertThat(test.toString()).isEqualTo(STANDARD_ID.toString());
  }

  @Test
  public void test_of_standardId() {
    LegalEntityId test = LegalEntityId.of(STANDARD_ID);
    assertThat(test.getStandardId()).isEqualTo(STANDARD_ID);
    assertThat(test.getReferenceDataType()).isEqualTo(LegalEntity.class);
    assertThat(test.toString()).isEqualTo(STANDARD_ID.toString());
  }

  @Test
  public void test_parse() {
    LegalEntityId test = LegalEntityId.parse(STANDARD_ID.toString());
    assertThat(test.getStandardId()).isEqualTo(STANDARD_ID);
    assertThat(test.getReferenceDataType()).isEqualTo(LegalEntity.class);
    assertThat(test.toString()).isEqualTo(STANDARD_ID.toString());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    LegalEntityId a = LegalEntityId.of("A", "1");
    LegalEntityId a2 = LegalEntityId.of("A", "1");
    LegalEntityId b = LegalEntityId.of("B", "1");
    assertThat(a.equals(a)).isTrue();
    assertThat(a.equals(a2)).isTrue();
    assertThat(a.equals(b)).isFalse();
    assertThat(a.equals(null)).isFalse();
    assertThat(a.equals(ANOTHER_TYPE)).isFalse();
  }

}
