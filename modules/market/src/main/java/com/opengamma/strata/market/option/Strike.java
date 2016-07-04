/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.option;

/**
 * The strike of an option, describing both type and value.
 * <p>
 * The strike of option instruments is represented in different ways.
 * For example, the strike types include delta, moneyness, log-moneyness, and strike itself.
 */
public interface Strike {

  /**
   * Gets the type of the strike.
   * 
   * @return the strike type
   */
  public abstract StrikeType getType();

  /**
   * Gets the value of the strike.
   * 
   * @return the value
   */
  public abstract double getValue();

  /**
   * Gets a label describing the strike.
   * 
   * @return the label
   */
  public default String getLabel() {
    return getType() + "=" + getValue();
  }

  /**
   * Creates an new instance of the same strike type with value.
   * 
   * @param value  the new value
   * @return the new strike instance
   */
  public abstract Strike withValue(double value);

}
