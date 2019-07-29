/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link FieldName}.
 */
public class FieldNameTest {

  //-----------------------------------------------------------------------
  @Test
  public void coverage() {
    FieldName test = FieldName.of("Foo");
    assertThat(test.toString()).isEqualTo("Foo");
    assertSerialization(test);
    assertJodaConvert(FieldName.class, test);
  }

}
