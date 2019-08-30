/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.product.common.SettlementType;

/**
 * Test {@link PhysicalSwaptionSettlement}.
 */
public class PhysicalSwaptionSettlementTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_DEFAULT() {
    PhysicalSwaptionSettlement test = PhysicalSwaptionSettlement.DEFAULT;
    assertThat(test.getSettlementType()).isEqualTo(SettlementType.PHYSICAL);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    PhysicalSwaptionSettlement test = PhysicalSwaptionSettlement.DEFAULT;
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    PhysicalSwaptionSettlement test = PhysicalSwaptionSettlement.DEFAULT;
    assertSerialization(test);
  }

}
