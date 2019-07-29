/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link CombinedExtendedEnum}.
 */
public class CombinedExtendedEnumTest {

  @Test
  public void test_lookup() {
    assertThat(UberNamed.of("Standard")).isEqualTo(SampleNameds.STANDARD);
    assertThat(UberNamed.of("More")).isEqualTo(MoreSampleNameds.MORE);
    CombinedExtendedEnum<UberNamed> combined = CombinedExtendedEnum.of(UberNamed.class);
    assertThat(combined.find("Rubbish")).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> combined.lookup("Rubbish"));
    assertThat(combined.toString()).isEqualTo("CombinedExtendedEnum[UberNamed]");
  }

}
