/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;

/**
 * TODO rename?
 */
@Deprecated
public interface RawMarketDataSource {

  MarketDataItem get(ExternalIdBundle idBundle, String dataField);
}
