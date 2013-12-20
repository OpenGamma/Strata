/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link.config;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.VersionCorrectionProvider;

/**
 * A config link implementation where the user can provide a custom service context rather than use one
 * in a thread local context.
 * Instances of this object can be created using the appropriate of() factory method on @see ConfigLink<T>
 * @param <T> the type of the target object
 */
class CustomResolverConfigLink<T> extends ConfigLink<T> {
  private ServiceContext _serviceContext;

  protected CustomResolverConfigLink(Class<T> type, String name, ServiceContext serviceContext) {
    super(type, name);
    _serviceContext = serviceContext;
  }

  @Override
  public T getConfig() {
    VersionCorrectionProvider vcProvider = _serviceContext.getService(VersionCorrectionProvider.class);
    ConfigSource configSource = _serviceContext.getService(ConfigSource.class);
    return (T) configSource.getSingle(getType(), getName(), vcProvider.getConfigVersionCorrection());
  }
}
