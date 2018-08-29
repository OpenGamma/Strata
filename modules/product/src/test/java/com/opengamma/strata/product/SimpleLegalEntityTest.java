/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.location.Country;

/**
 * Test {@link SimpleLegalEntity}.
 */
@Test
public class SimpleLegalEntityTest {

  private static final LegalEntityId LEI = LegalEntityId.of("LEI", "A");
  private static final LegalEntityId LEI2 = LegalEntityId.of("LEI", "B");

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleLegalEntity test = SimpleLegalEntity.of(LEI, "US GOV", Country.US);
    coverImmutableBean(test);
    SimpleLegalEntity test2 = SimpleLegalEntity.of(LEI2, "GB GOV", Country.GB);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SimpleLegalEntity test = SimpleLegalEntity.of(LEI, "US GOV", Country.US);
    assertSerialization(test);
  }

}
