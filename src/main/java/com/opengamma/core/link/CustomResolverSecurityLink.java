/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries2.HistoricalDataRequest;
import com.opengamma.core.historicaltimeseries2.HistoricalTimeSeriesSource;
import com.opengamma.core.security.Security;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.VersionCorrectionProvider;

/**
 * 
 * @param <T>
 */
public class CustomResolverSecurityLink<T extends Security> extends SecurityLink<T> {
  private ServiceContext _serviceContext;

  protected CustomResolverSecurityLink(ExternalIdBundle bundle, ServiceContext serviceContext) {
    super(bundle);
    _serviceContext = serviceContext;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T getSecurity() {
    VersionCorrectionProvider vcProvider = _serviceContext.getService(VersionCorrectionProvider.class);
    SecuritySource securitySource = _serviceContext.getService(SecuritySource.class);
    return (T) securitySource.getSingle(getBundle(), vcProvider.getPortfolioVersionCorrection());
  }
  
  @Override
  public HistoricalTimeSeries getHistoricalData(HistoricalDataRequest request) {
    //VersionCorrectionProvider vcProvider = serviceContext.getService(VersionCorrectionProvider.class);
    HistoricalTimeSeriesSource htsSource = _serviceContext.getService(HistoricalTimeSeriesSource.class);
    return htsSource.getHistoricalTimeSeries(request);
  }

  @Override
  public MarketDataResult getCurrentData(String field) {
    return null;
  }
}
