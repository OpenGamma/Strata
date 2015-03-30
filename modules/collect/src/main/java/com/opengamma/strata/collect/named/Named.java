/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * A named instance.
 * <p>
 * This simple interface is used to define objects that can be identified by a unique name.
 * The name contains enough information to be able to recreate the instance.
 */
public interface Named {

  /**
   * Gets the unique name of the instance.
   * <p>
   * The name contains enough information to be able to recreate the instance.
   * 
   * @return the unique name
   */
  public abstract String getName();

}
