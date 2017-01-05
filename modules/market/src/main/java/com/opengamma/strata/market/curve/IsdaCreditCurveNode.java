/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;

/**
 * A node specifying how to calibrate an ISDA compliant curve.
 * <p>
 * A curve node is associated with an instrument and provide the information of the instrument for pricing.
 */
public interface IsdaCreditCurveNode {

  /**
   * Gets the label to use for the node.
   * 
   * @return the label, not empty
   */
  public abstract String getLabel();

  /**
   * Get the observable ID. 
   * <p>
   * The observable ID is the identifier of the market data value.
   * 
   * @return the observable ID
   */
  public abstract ObservableId getObservableId();

  /**
   * Calculates the date associated with the node.
   * <p>
   * Each curve node has an associated date which defines the x-value in the curve. 
   * This is typically the adjusted end date of the instrument.
   * 
   * @param tradeDate  the trade date
   * @param refData  the reference data
   * @return the node date
   */
  public abstract LocalDate date(LocalDate tradeDate, ReferenceData refData);

  /**
   * Returns metadata for the node from the node date. 
   * <p>
   * The node date must be computed by {@link #date(LocalDate, ReferenceData)}.
   *
   * @param nodeDate  the node date used when calibrating the curve
   * @return metadata for the node
   */
  public default DatedParameterMetadata metadata(LocalDate nodeDate) {
    return LabelDateParameterMetadata.of(nodeDate, getLabel());
  }

}
