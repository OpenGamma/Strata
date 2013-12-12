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
 * A SecurityLink that uses the ThreadLocalServiceContext instance (per-thread) 
 * @param <T> the type of Security to which the link is referring
 */
class ThreadLocalSecurityLink<T extends Security> extends SecurityLink<T> {

  protected ThreadLocalSecurityLink(ExternalIdBundle bundle) {
    super(bundle);
  }

  @SuppressWarnings("unchecked")
  @Override
  public T getSecurity() {
    ServiceContext serviceContext = ThreadLocalServiceContext.getInstance();
    VersionCorrectionProvider vcProvider = serviceContext.getService(VersionCorrectionProvider.class);
    SecuritySource securitySource = serviceContext.getService(SecuritySource.class);
    return (T) securitySource.getSingle(getBundle(), vcProvider.getPortfolioVersionCorrection());
  }

  @Override
  public HistoricalTimeSeries getHistoricalData(HistoricalDataRequest request) {
    ServiceContext serviceContext = ThreadLocalServiceContext.getInstance();
    //VersionCorrectionProvider vcProvider = serviceContext.getService(VersionCorrectionProvider.class);
    HistoricalTimeSeriesSource htsSource = serviceContext.getService(HistoricalTimeSeriesSource.class);
    return htsSource.getHistoricalTimeSeries(request);
  }

  @Override
  public MarketDataResult getCurrentData(String field) {
    return null;
  }

}
