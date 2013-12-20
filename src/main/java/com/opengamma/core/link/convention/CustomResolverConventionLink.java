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
import com.opengamma.service.VersionCorrectionProvider;

/**
 * A convention link implementation where the user can provide a custom service context rather than use one
 * in a thread local context.
 * Instances of this object can be created using the appropriate of() factory method on @see ConventionLink<T>
 * @param <T> the type of the target object
 */
class CustomResolverConventionLink<T extends Convention> extends ConventionLink<T> {
  private ServiceContext _serviceContext;

  protected CustomResolverConventionLink(ExternalIdBundle bundle, ServiceContext serviceContext) {
    super(bundle);
    _serviceContext = serviceContext;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T getConvention() {
    VersionCorrectionProvider vcProvider = _serviceContext.getService(VersionCorrectionProvider.class);
    ConventionSource conventionSource = _serviceContext.getService(ConventionSource.class);
    return (T) conventionSource.getSingle(getBundle(), vcProvider.getPortfolioVersionCorrection());
  }
}
