/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance;

import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test.
 */
@Test
public class AttributableTest {

  public void test_of() {
    Attributable test = new Attributable() {
      @Override
      public ImmutableMap<String, String> getAttributes() {
        return ImmutableMap.of("A", "B", "X", "Y");
      }
    };
    assertEquals(test.getAttributes(), ImmutableMap.of("A", "B", "X", "Y"));
    assertEquals(test.findAttribute("A"), Optional.of("B"));
    assertEquals(test.findAttribute("X"), Optional.of("Y"));
    assertEquals(test.findAttribute("M"), Optional.empty());
  }

}
