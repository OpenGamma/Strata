/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link EtdContractSpecId}.
 */
public class EtdContractSpecIdTest {

  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_of() {
    EtdContractSpecId test = EtdContractSpecId.of(StandardId.of("A", "B"));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("A", "B"));
    assertThat(test.getReferenceDataType()).isEqualTo(EtdContractSpec.class);
    assertThat(test.toString()).isEqualTo("A~B");
  }

  @Test
  public void test_parse() {
    EtdContractSpecId test = EtdContractSpecId.parse("A~B");
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("A", "B"));
    assertThat(test.toString()).isEqualTo("A~B");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    EtdContractSpecId a = EtdContractSpecId.of(StandardId.of("A", "B"));
    EtdContractSpecId a2 = EtdContractSpecId.of(StandardId.of("A", "B"));
    EtdContractSpecId b = EtdContractSpecId.of(StandardId.of("C", "D"));
    assertThat(a.hashCode()).isEqualTo(a2.hashCode());
    assertThat(a.equals(a)).isTrue();
    assertThat(a.equals(a2)).isTrue();
    assertThat(a.equals(b)).isFalse();
    assertThat(a.equals(null)).isFalse();
    assertThat(a.equals(ANOTHER_TYPE)).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    EtdContractSpecId test = EtdContractSpecId.of("A", "B");
    assertSerialization(test);
  }

}
