/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.basics.StandardSchemes.LEI_SCHEME;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.location.Country;

/**
 * Test {@link SimpleLegalEntity}.
 */
public class SimpleLegalEntityTest {

  private static final LegalEntityId LEI = LegalEntityId.of(LEI_SCHEME, "A");
  private static final LegalEntityId LEI2 = LegalEntityId.of(LEI_SCHEME, "B");

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SimpleLegalEntity test = SimpleLegalEntity.of(LEI, "US GOV", Country.US);
    coverImmutableBean(test);
    SimpleLegalEntity test2 = SimpleLegalEntity.of(LEI2, "GB GOV", Country.GB);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SimpleLegalEntity test = SimpleLegalEntity.of(LEI, "US GOV", Country.US);
    assertSerialization(test);
  }

}
