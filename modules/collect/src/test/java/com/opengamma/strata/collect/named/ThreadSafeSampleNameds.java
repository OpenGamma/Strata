/*
 * Copyright (C) 2025 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object provider for thread safety testing.
 */
public class ThreadSafeSampleNameds implements ThreadSafeSampleNamed {

  /**
   * Standard instance.
   */
  public static final ThreadSafeSampleNameds STANDARD = new ThreadSafeSampleNameds();

  @Override
  public String getName() {
    return "ThreadStandard";
  }

}

