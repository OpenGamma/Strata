/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesName;

/**
 * 
 */
public interface SwaptionVolatilitiesSpecification {

  public abstract SwaptionVolatilitiesName getName();

  public abstract IborIndex getIndex();

  public abstract ImmutableList<SwaptionVolatilitiesNode> getNodes();

  //-------------------------------------------------------------------------
  public abstract SwaptionVolatilities volatilities(
      ZonedDateTime valuationDateTime,
      DoubleArray parameters,
      RatesProvider ratesProvider,
      ReferenceData refData);


  public default ImmutableList<QuoteId> volatilitiesInputs() {
    return null;
//    return getNodes().stream()
//        .map(SwaptionVolatilitiesNode::getQuoteId)
//        .collect(toImmutableList());
  }

  public default int getParameterCount() {
    return getNodes().size();
  }

}
