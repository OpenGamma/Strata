/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import java.time.LocalDate;

/**
 * Definition of barrier event of option instruments.
 * <p>
 * The barrier type, knock type, barrier level and other relevant information are specified in this class.
 * <p>
 * The barrier level can be date dependent. <br>
 * For forward starting barrier, the barrier level can be set to very high or low level in the initial period. <br>
 * The barrier is continuously monitored.
 */
public interface Barrier {

  /**
   * Obtains the barrier type.
   * 
   * @return the barrier type
   */
  public abstract BarrierType getBarrierType();

  /**
   * Obtains the knock type.
   * 
   * @return the knock type
   */
  public abstract KnockType getKnockType();

  /**
   * Obtains the barrier level for a given observation date.
   * 
   * @param date  the observation date
   * @return the barrier level
   */
  public abstract double getBarrierLevel(LocalDate date);

  /**
   * Obtains an instance with knock type inverted.
   * 
   * @return the instance
   */
  public abstract Barrier inverseKnockType();

}
