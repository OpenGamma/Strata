/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.testng.annotations.Test;

/**
 * Test {@link EtdContractGroupCode}.
 */
@Test
public class EtdContractGroupCodeTest {

  public void test_of() {
    EtdContractGroupCode test = EtdContractGroupCode.of("ABC");
    assertThat(test.getName()).isEqualTo("ABC");
    assertThat(test).hasToString("ABC");
    assertThatIllegalArgumentException().isThrownBy(() -> EtdContractGroupCode.of(""));
    assertThatIllegalArgumentException().isThrownBy(() -> EtdContractGroupCode.of("\n"));
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    EtdContractGroupCode a = EtdContractGroupCode.of("ABC");
    EtdContractGroupCode a2 = EtdContractGroupCode.of("ABC");
    EtdContractGroupCode b = EtdContractGroupCode.of("DEF");
    assertThat(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(a2);
  }

  //-------------------------------------------------------------------------
  public void test_serialization() {
    EtdContractGroupCode test = EtdContractGroupCode.of("ABC");
    assertSerialization(test);
  }

}
