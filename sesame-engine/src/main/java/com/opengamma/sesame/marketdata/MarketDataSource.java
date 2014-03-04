/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;

/**
 * TODO should this be renamed because it isn't a source?
 */
public interface MarketDataSource {

  MarketDataItem<?> get(ExternalIdBundle idBundle, FieldName fieldName);
}
