/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

import java.time.LocalDate;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.param.ParameterizedData;

/**
 * Recovery rates.
 * <p>
 * This represents the recovery rates of a legal entity.
 */
public interface RecoveryRates
    extends MarketDataView, ParameterizedData {

  /**
   * Gets the valuation date. 
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

  /**
   * Gets the standard identifier of a legal entity.
   * 
   * @return the legal entity ID
   */
  public abstract StandardId getLegalEntityId();

  /**
   * Gets the recovery rate for the specified date. 
   * 
   * @param date  the date
   * @return the recovery rate
   */
  public abstract double recoveryRate(LocalDate date);

}
