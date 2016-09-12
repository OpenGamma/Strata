/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import static org.testng.Assert.assertEquals;

import java.util.Optional;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Test {@link FpmlPartySelector}.
 */
@Test
public class FpmlPartySelectorTest {

  private static final ListMultimap<String, String> MAP = ImmutableListMultimap.of("A", "a1", "A", "a2", "B", "b");

  //-------------------------------------------------------------------------
  public void test_auto() {
    assertEquals(FpmlPartySelector.any().selectParty(MAP), Optional.empty());
  }

  public void test_matching() {
    assertEquals(FpmlPartySelector.matching("a1").selectParty(MAP), Optional.of("A"));
    assertEquals(FpmlPartySelector.matching("a2").selectParty(MAP), Optional.of("A"));
    assertEquals(FpmlPartySelector.matching("b").selectParty(MAP), Optional.of("B"));
    assertEquals(FpmlPartySelector.matching("c").selectParty(MAP), Optional.empty());
  }

  public void test_matchingRegex() {
    assertEquals(FpmlPartySelector.matchingRegex(Pattern.compile("a[12]")).selectParty(MAP), Optional.of("A"));
    assertEquals(FpmlPartySelector.matchingRegex(Pattern.compile("b")).selectParty(MAP), Optional.of("B"));
    assertEquals(FpmlPartySelector.matchingRegex(Pattern.compile("c")).selectParty(MAP), Optional.empty());
  }

}
