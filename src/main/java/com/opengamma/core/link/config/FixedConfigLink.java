/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link.config;

import com.opengamma.service.ServiceContext;

/**
 * 
 * @param <T> the type of the underlying config
 */
class FixedConfigLink<T> extends ConfigLink<T> {
  private T _config;
  private String _name;
    
  @SuppressWarnings("unchecked")
  protected FixedConfigLink(T config, String name) {
    super((Class<T>) config.getClass(), name);
    _name = name;
    _config = config;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ConfigLink<T> with(ServiceContext resolver) {
    return new CustomResolverConfigLink<T>((Class<T>) _config.getClass(), _name, resolver);
  }

  @Override
  public T getConfig() {
    return _config;
  }

}
