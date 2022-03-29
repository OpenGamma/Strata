/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * A described instance.
 * <p>
 * This simple interface is used to define objects that have a human-readable description.
 * This can be used alongside {@link Named} to provide a way to decode names that are codified.
 */
public interface Described {

  /**
   * Gets the human-readable described of the instance.
   * 
   * @return the description
   */
  public abstract String getDescription();

}
