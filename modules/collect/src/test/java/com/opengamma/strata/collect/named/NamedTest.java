/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link Named}.
 */
public class NamedTest {

  @Test
  public void test_of() {
    SampleNamed test = Named.of(SampleNamed.class, "Standard");
    assertThat(test).isEqualTo(SampleNameds.STANDARD);
    assertThatIllegalArgumentException().isThrownBy(() -> Named.of(SampleNamed.class, "Rubbish"));
  }

}
