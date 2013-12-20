/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link.config;

import com.opengamma.service.ServiceContext;

/**
 * Represents a link to a Config object using an ExternalId or ExternalIdBundle that is resolved on demand.  Use of links allows
 * provision of Securities by remote servers while maintaining the ability to capture updates to the linked resources on each
 * subsequent resolution.
 * @param <T> type of the config
 */
public abstract class ConfigLink<T> {
  private Class<T> _type;
  private String _name;
  
  protected ConfigLink(Class<T> type, String name) {
    _type = type;
    _name = name;
  }
  
  /**
   * Creates a link that will use a service context accessed via a thread local to access a pre-configured service context 
   * containing the ConfigSource and VersionCorrectionProvider necessary to resolve the provided bundle into the target 
   * object 
   * @param <E> the type of the object being linked to
   * @param type the class of the type of object being linked to
   * @param name the name of the config object
   * @return a config link
   */
  public static <E> ConfigLink<E> of(Class<E> type, String name) {
    return new ThreadLocalConfigLink<E>(type, name);
  }
  
  /**
   * Creates a link that embeds the provided object directly.  This should only be used for testing as it will not update
   * if the underlying object is updated via another data source or by a change in the VersionCorrection environment
   * @param <E> the type of the underlying Config the link refers to 
   * @param config the config to embed in the link, not null
   * @param name the name of the config object
   * @return the config link
   */
  public static <E> FixedConfigLink<E> of(E config, String name) {
    return new FixedConfigLink<E>(config, name);
  }
  
  /**
   * Creates a link that will use the provided service context to resolve the link rather than use one available via a 
   * thread local environment.  Use of this method should only be necessary when you need to use resolution outside of 
   * the current VersionCorrection threadlocal environment.
   * @param <E> the type of the underlying Config the link refers to 
   * @param type the class of the type of Config the link refers to
   * @param name the name of the config object
   * @param serviceContext a service context containing the ConfigSource and VersionCorrectionProvider necessary to resolve, not null
   * @return the config link
   */
  public static <E> ConfigLink<E> of(Class<E> type, String name, ServiceContext serviceContext) {
    return new CustomResolverConfigLink<E>(type, name, serviceContext);
  }
  
  /**
   * Create a new ConfigLink, with the same name and type as this one that uses a newly provided serviceContext.  This should
   * only be necessary when you need to use reoslution outside of the current VersionCorrection threadlocal environment.
   * @param <E> the type of the underlying Config the link refers to
   * @param serviceContext a service context containing the ConfigSource and VersionCorrectionProvider necessary to resolve, not null
   * @return a new config link
   */
  @SuppressWarnings("unchecked")
  public <E> ConfigLink<E> with(ServiceContext serviceContext) {
    return new CustomResolverConfigLink<E>((Class<E>) _type, _name, serviceContext);
  }
 
  /**
   * Resolve the link and get the underlying config
   * @return the config
   */
  public abstract T getConfig();
  
  
  /**
   * Get the name on which the link is based
   * @return the name
   */
  public String getName() {
    return _name;
  }
  
  /**
   * Get the name on which the link is based
   * @return the name
   */
  public Class<T> getType() {
    return _type;
  }
}
