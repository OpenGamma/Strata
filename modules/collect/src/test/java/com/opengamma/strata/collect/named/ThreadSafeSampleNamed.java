/*
 * Copyright (C) 2025 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object for thread safety testing.
 * <p>
 * This is a separate type from {@link SampleNamed} to ensure we can test
 * fresh initialization in different threading contexts without interference
 * from other tests.
 */
public interface ThreadSafeSampleNamed extends Named {

  /**
   * Gets the extended enum helper for thread safety testing.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<ThreadSafeSampleNamed> extendedEnum() {
    return ExtendedEnum.of(ThreadSafeSampleNamed.class);
  }

}

