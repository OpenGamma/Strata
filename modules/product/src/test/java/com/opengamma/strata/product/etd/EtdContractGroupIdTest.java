/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.product.common.ExchangeIds;

/**
 * Test {@link EtdContractGroupId}.
 */
public class EtdContractGroupIdTest {

  @Test
  public void test_of_objects() {
    EtdContractGroupId test = EtdContractGroupId.of(ExchangeIds.ECAG, EtdContractGroupCode.of("ABC"));
    assertThat(test.getExchangeId()).isEqualTo(ExchangeIds.ECAG);
    assertThat(test.getCode()).isEqualTo(EtdContractGroupCode.of("ABC"));
    assertThat(test).hasToString("ECAG::ABC");
  }

  @Test
  public void test_of_strings() {
    EtdContractGroupId test = EtdContractGroupId.of("ECAG", "ABC");
    assertThat(test.getExchangeId()).isEqualTo(ExchangeIds.ECAG);
    assertThat(test.getCode()).isEqualTo(EtdContractGroupCode.of("ABC"));
    assertThat(test).hasToString("ECAG::ABC");
  }

  @Test
  public void test_parse() {
    EtdContractGroupId test = EtdContractGroupId.parse("ECAG::ABC");
    assertThat(test.getExchangeId()).isEqualTo(ExchangeIds.ECAG);
    assertThat(test.getCode()).isEqualTo(EtdContractGroupCode.of("ABC"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    EtdContractGroupId a = EtdContractGroupId.of(ExchangeIds.ECAG, EtdContractGroupCode.of("ABC"));
    EtdContractGroupId a2 = EtdContractGroupId.of(ExchangeIds.ECAG, EtdContractGroupCode.of("ABC"));
    EtdContractGroupId b = EtdContractGroupId.of(ExchangeIds.IFEN, EtdContractGroupCode.of("ABC"));
    EtdContractGroupId c = EtdContractGroupId.of(ExchangeIds.ECAG, EtdContractGroupCode.of("DEF"));
    assertThat(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(a2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    EtdContractGroupId test = EtdContractGroupId.of("A", "B");
    assertSerialization(test);
  }

}
