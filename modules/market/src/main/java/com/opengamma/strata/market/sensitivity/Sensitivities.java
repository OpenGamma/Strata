/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.io.Serializable;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.product.PortfolioItem;
import com.opengamma.strata.product.PortfolioItemInfo;

/**
 * Risk expressed as a set of sensitivities.
 * <p>
 * Sometimes it is useful to pass in a representation of risk rather than explicitly
 * listing the current portfolio of trades and/or positions.
 * This target is designed to allow this.
 * <p>
 * The most common implementation is {@link CurveSensitivities}, which allows delta and gamma
 * sensitivity to curves to be expressed.
 * <p>
 * Implementations may express the risk in any way they see fit.
 * Where risk is grouped, such as by trade, it is intended that one instance exists for each grouping.
 * 
 * @see CurveSensitivities
 */
public interface Sensitivities
    extends PortfolioItem, ImmutableBean, Serializable {

  /**
   * Gets the additional information.
   * <p>
   * All sensitivity instances contain this standard set of information.
   * One use is to represent the grouping criteria for this instance.
   * 
   * @return the additional information
   */
  @Override
  public abstract PortfolioItemInfo getInfo();

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified info.
   * 
   * @param info  the new info
   * @return the instance with the specified info
   */
  public abstract Sensitivities withInfo(PortfolioItemInfo info);

}
