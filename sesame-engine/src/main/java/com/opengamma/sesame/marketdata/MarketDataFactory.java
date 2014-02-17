/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.sesame.engine.ComponentMap;

/**
 * TODO need to ensure impls can be compared, will need to know if data source changes for cache invalidation logic
 * TODO this probably needs to be stateful and keep track of changing data
 * for live sources need to know the IDs of individual data items that change
 * for snapshots need to know if the snapshot has been updated in the DB
 * TODO probably needs a different name, 'factory' implies it's only used to create the fn and isn't connected to it
 * or should MarketDataFn support listeners?
 */
public interface MarketDataFactory {

  MarketDataFn create(ComponentMap components);
}
