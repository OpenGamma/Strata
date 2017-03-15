/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

/**
 * Allows exceptions to be tested inline.
 */
@FunctionalInterface
public interface AssertRunnable {

  /**
   * Used to wrap code that is expected to throw an exception.
   * 
   * @throws Throwable the expected result
   */
  void run() throws Throwable;

}
