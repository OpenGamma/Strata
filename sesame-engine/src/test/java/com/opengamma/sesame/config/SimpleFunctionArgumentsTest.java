package com.opengamma.sesame.config;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class SimpleFunctionArgumentsTest {

  @Test
  public void mergeWith() {
    SimpleFunctionArguments args1 =
        new SimpleFunctionArguments(ImmutableMap.<String, Object>of("foo", "FOO", "bar", "BAR"));
    SimpleFunctionArguments args2 =
        new SimpleFunctionArguments(ImmutableMap.<String, Object>of("bar", "BAR2", "baz", "BAZ"));

    SimpleFunctionArguments expected =
        new SimpleFunctionArguments(ImmutableMap.<String, Object>of("foo", "FOO", "bar", "BAR", "baz", "BAZ"));
    assertEquals(expected, args1.mergeWith(args2));
  }
}
