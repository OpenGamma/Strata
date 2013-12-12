/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link;

import java.util.Map;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries2.HistoricalDataRequest;
import com.opengamma.core.security.Security;
import com.opengamma.service.ServiceContext;

/**
 * 
 * @param <T> the type of the underlying security
 */
public class FixedSecurityLink<T extends Security> extends SecurityLink<T> {
  private T _security;
  private Map<HistoricalDataRequest, HistoricalTimeSeries> _timeSeriesMap;
  private HistoricalTimeSeries _timeSeries;
  private Map<String, MarketDataResult> _marketDataResultMap;
  private MarketDataResult _marketDataResult;
    
  protected FixedSecurityLink(T security, Map<HistoricalDataRequest, HistoricalTimeSeries> timeSeriesMap, Map<String, MarketDataResult> marketDataResultMap) {
    super(security.getExternalIdBundle());
    _security = security;
    _timeSeriesMap = timeSeriesMap;
    _marketDataResultMap = marketDataResultMap;
  }
  
  protected FixedSecurityLink(T security, HistoricalTimeSeries timeSeries, MarketDataResult marketDataResult) {
    super(security.getExternalIdBundle());
    _security = security;
    _timeSeries = timeSeries;
    _marketDataResult = marketDataResult;
  }

  @SuppressWarnings("unchecked")
  @Override
  public SecurityLink<T> with(ServiceContext resolver) {
    return new CustomResolverSecurityLink<T>(_security.getExternalIdBundle(), resolver);
  }

  @Override
  public T getSecurity() {
    return _security;
  }

  @Override
  public HistoricalTimeSeries getHistoricalData(HistoricalDataRequest request) {
    if (_timeSeriesMap != null) {
      return _timeSeriesMap.get(request);
    } else {
      return _timeSeries;
    }
  }

  @Override
  public MarketDataResult getCurrentData(String field) {
    if (_marketDataResultMap != null) {
      return _marketDataResultMap.get(field);
    } else {
      return _marketDataResult;
    }
  }
}
