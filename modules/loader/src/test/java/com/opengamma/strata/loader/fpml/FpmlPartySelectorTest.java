/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Test {@link FpmlPartySelector}.
 */
public class FpmlPartySelectorTest {

  private static final ListMultimap<String, String> MAP =
      ImmutableListMultimap.of("A", "a1", "A", "a2", "B", "b", "C1", "c1", "C2", "c2");

  //-------------------------------------------------------------------------
  @Test
  public void test_any() {
    assertThat(FpmlPartySelector.any().selectParties(MAP)).isEmpty();
  }

  @Test
  public void test_matching() {
    assertThat(FpmlPartySelector.matching("a1").selectParties(MAP)).containsExactly("A");
    assertThat(FpmlPartySelector.matching("a2").selectParties(MAP)).containsExactly("A");
    assertThat(FpmlPartySelector.matching("b").selectParties(MAP)).containsExactly("B");
    assertThat(FpmlPartySelector.matching("c").selectParties(MAP)).isEmpty();
  }

  @Test
  public void test_matchingRegex() {
    assertThat(FpmlPartySelector.matchingRegex(Pattern.compile("a[12]")).selectParties(MAP)).containsExactly("A");
    assertThat(FpmlPartySelector.matchingRegex(Pattern.compile("b")).selectParties(MAP)).containsExactly("B");
    assertThat(FpmlPartySelector.matchingRegex(Pattern.compile("c[0-9]")).selectParties(MAP)).containsExactly("C1", "C2");
    assertThat(FpmlPartySelector.matchingRegex(Pattern.compile("d")).selectParties(MAP)).isEmpty();
  }

}
