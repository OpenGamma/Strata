/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import static org.testng.Assert.assertEquals;

import java.util.stream.Stream;

import org.testng.annotations.Test;

/**
 * Test {@link Array}.
 */
@Test
public class ArrayTest {

  public void test_notEmpty() {
    Array<String> array = new Array<String>() {

      @Override
      public int size() {
        return 1;
      }

      @Override
      public String get(int index) {
        return null;
      }

      @Override
      public Stream<String> stream() {
        return null;
      }
    };
    assertEquals(array.isEmpty(), false);
  }

  public void test_empty() {
    Array<String> array = new Array<String>() {

      @Override
      public int size() {
        return 0;
      }

      @Override
      public String get(int index) {
        return null;
      }

      @Override
      public Stream<String> stream() {
        return null;
      }
    };
    assertEquals(array.isEmpty(), true);
  }

}
