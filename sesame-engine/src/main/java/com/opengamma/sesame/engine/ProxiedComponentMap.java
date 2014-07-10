/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import com.google.common.collect.Multimap;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueIdentifiable;
import com.opengamma.sesame.marketdata.HtsRequestKey;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;

/**
 * Provides a hook to allow listeners to be added to all
 * calls made to components of a component map or historical
 * time series.
 */
// TODO - name of this interface could be improved as there is no direct connection with ComponentMap
public interface ProxiedComponentMap {

  /**
   * Called when a component has a method called. If a single method
   * call returns multiple items, this method will be called for
   * each individual item returned.
   *
   * @param componentType the type of component called
   * @param item the item returned from the component
   */
  void receivedCall(Class<?> componentType, UniqueIdentifiable item);

  /**
   * Retrieve the set of components that were called and the data
   * that each of them returned.
   *
   * @return multimap of component -> data items
   */
  Multimap<Class<?>, UniqueIdentifiable> retrieveResults();

  /**
   * Called when a historical time series is requested.
   *
   * @param identifierBundle  identifier for the time series
   * @param dataSource  data source for the time series
   * @param dataProvider  provider for the time series, not required
   * @param dataField  data field for the time series
   * @param hts  the time series which was returned in response to the request
   */
  void receivedHtsCall(ExternalIdBundle identifierBundle, String dataSource, String dataProvider,
                       String dataField, LocalDateDoubleTimeSeries hts);

  /**
   * Retrieve the map of HTS requests made and the time series which
   * were returned in response.
   *
   * @return multimap of HTS requests -> Results
   */
  Multimap<HtsRequestKey, LocalDateDoubleTimeSeries> retrieveHtsResults();
}
