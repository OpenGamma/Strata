/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link.config;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;

/**
 * A ConfigLink that uses the ThreadLocalServiceContext instance (per-thread) 
 * @param <T> the type of Config to which the link is referring
 */
class ThreadLocalConfigLink<T> extends ConfigLink<T> {

  protected ThreadLocalConfigLink(Class<T> type, String name) {
    super(type, name);
  }

  @Override
  public T getConfig() {
    ServiceContext serviceContext = ThreadLocalServiceContext.getInstance();
    VersionCorrectionProvider vcProvider = serviceContext.getService(VersionCorrectionProvider.class);
    ConfigSource configSource = serviceContext.getService(ConfigSource.class);
    return (T) configSource.getSingle(getType(), getName(), vcProvider.getConfigVersionCorrection());
  }

}
