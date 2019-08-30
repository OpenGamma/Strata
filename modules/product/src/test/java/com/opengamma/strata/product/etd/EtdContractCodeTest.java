/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link EtdContractCode}.
 */
public class EtdContractCodeTest {

  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_of() {
    EtdContractCode test = EtdContractCode.of("test");
    assertThat(test.toString()).isEqualTo("test");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    EtdContractCode a = EtdContractCode.of("test");
    EtdContractCode a2 = EtdContractCode.of("test");
    EtdContractCode b = EtdContractCode.of("test2");
    assertThat(a.equals(a)).isTrue();
    assertThat(a.equals(a2)).isTrue();
    assertThat(a.equals(b)).isFalse();
    assertThat(a.equals(null)).isFalse();
    assertThat(a.equals(ANOTHER_TYPE)).isFalse();
  }

}
