/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.util.Optional;

import org.testng.annotations.Test;

/**
 * Test {@link CombinedExtendedEnum}.
 */
@Test
public class CombinedExtendedEnumTest {

  public void test_lookup() {
    assertEquals(UberNamed.of("Standard"), SampleNameds.STANDARD);
    assertEquals(UberNamed.of("More"), MoreSampleNameds.MORE);
    CombinedExtendedEnum<UberNamed> combined = CombinedExtendedEnum.of(UberNamed.class);
    assertEquals(combined.find("Rubbish"), Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> combined.lookup("Rubbish"));
    assertEquals(combined.toString(), "CombinedExtendedEnum[UberNamed]");
  }

}
