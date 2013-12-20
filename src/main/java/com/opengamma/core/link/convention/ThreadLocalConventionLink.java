/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link.convention;

import com.opengamma.core.convention.Convention;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;

/**
 * A ConventionLink that uses the ThreadLocalServiceContext instance (per-thread) 
 * @param <T> the type of Convention to which the link is referring
 */
class ThreadLocalConventionLink<T extends Convention> extends ConventionLink<T> {

  protected ThreadLocalConventionLink(ExternalIdBundle bundle) {
    super(bundle);
  }

  @SuppressWarnings("unchecked")
  @Override
  public T getConvention() {
    ServiceContext serviceContext = ThreadLocalServiceContext.getInstance();
    VersionCorrectionProvider vcProvider = serviceContext.getService(VersionCorrectionProvider.class);
    ConventionSource securitySource = serviceContext.getService(ConventionSource.class);
    return (T) securitySource.getSingle(getBundle(), vcProvider.getPortfolioVersionCorrection());
  }

}
