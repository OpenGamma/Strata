/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import static org.testng.Assert.assertEquals;

import java.util.regex.Pattern;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Test {@link FpmlPartySelector}.
 */
@Test
public class FpmlPartySelectorTest {

  private static final ListMultimap<String, String> MAP =
      ImmutableListMultimap.of("A", "a1", "A", "a2", "B", "b", "C1", "c1", "C2", "c2");

  //-------------------------------------------------------------------------
  public void test_any() {
    assertEquals(FpmlPartySelector.any().selectParties(MAP), ImmutableList.of());
  }

  public void test_matching() {
    assertEquals(FpmlPartySelector.matching("a1").selectParties(MAP), ImmutableList.of("A"));
    assertEquals(FpmlPartySelector.matching("a2").selectParties(MAP), ImmutableList.of("A"));
    assertEquals(FpmlPartySelector.matching("b").selectParties(MAP), ImmutableList.of("B"));
    assertEquals(FpmlPartySelector.matching("c").selectParties(MAP), ImmutableList.of());
  }

  public void test_matchingRegex() {
    assertEquals(FpmlPartySelector.matchingRegex(Pattern.compile("a[12]")).selectParties(MAP), ImmutableList.of("A"));
    assertEquals(FpmlPartySelector.matchingRegex(Pattern.compile("b")).selectParties(MAP), ImmutableList.of("B"));
    assertEquals(FpmlPartySelector.matchingRegex(Pattern.compile("c[0-9]")).selectParties(MAP), ImmutableList.of("C1", "C2"));
    assertEquals(FpmlPartySelector.matchingRegex(Pattern.compile("d")).selectParties(MAP), ImmutableList.of());
  }

}
