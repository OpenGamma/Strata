/*
 * Copyright (C) 2025 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Additional mock named object provider for thread safety testing.
 */
public class ThreadMoreSampleNameds implements ThreadSafeSampleNamed {

  /**
   * Another instance.
   */
  public static final ThreadMoreSampleNameds MORE = new ThreadMoreSampleNameds();

  @Override
  public String getName() {
    return "ThreadMore";
  }

}
