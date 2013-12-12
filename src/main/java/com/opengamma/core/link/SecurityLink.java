/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link;

import org.threeten.bp.LocalDate;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries2.HistoricalDataRequest;
import com.opengamma.core.security.Security;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.service.ServiceContext;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;

/**
 * Represents a link to a Security object using an ExternalId or ExternalIdBundle that is resolved on demand.  Use of links allows
 * provision of Securities by remote servers while maintaining the ability to capture updates to the linked resources on each
 * subsequent resolution.
 * @param <T> type of the security
 */
public abstract class SecurityLink<T extends Security> {
  private ExternalIdBundle _bundle;
  
  protected SecurityLink(ExternalIdBundle bundle) {
    _bundle = bundle;
  }
  
  /**
   * Creates a link that will use a service context accessed via a thread local to access a pre-configured service context 
   * containing the SecuritySource and VersionCorrectionProvider necessary to resolve the provided bundle into the target 
   * object 
   * @param <E> the type of the object being linked to
   * @param bundle the external id bundle to be resolved into the target object, not null
   * @return a security link
   */
  public static <E extends Security> SecurityLink<E> of(ExternalIdBundle bundle) {
    return new ThreadLocalSecurityLink<E>(bundle);
  }
  
  /**
   * Creates a link that will use a service context accessed via a thread local to access a pre-configured service context 
   * containing the SecuritySource and VersionCorrectionProvider necessary to resolve the provided externalId into the target 
   * object.  Try to use the bundle version of this call bundles where possible rather than a single externalId.
   * @param <E> the type of the object being linked to
   * @param externalId the external id to be resolved into the target object, not null
   * @return a security link  
   */
  public static <E extends Security> SecurityLink<E> of(ExternalId externalId) {
    return new ThreadLocalSecurityLink<E>(externalId.toBundle());
  }
  
  /**
   * Creates a link that embeds the provided object directly.  This should only be used for testing as it will not update
   * if the underlying object is updated via another data source or by a change in the VersionCorrection environment
   * @param <E> the type of the underlying Security the link refers to 
   * @param security the security to embed in the link, not null
   * @return the security link
   */
  public static <E extends Security> FixedSecurityLink<E> of(E security, HistoricalTimeSeries timeSeries, MarketDataResult marketDataResult) {
    return new FixedSecurityLink<E>(security, timeSeries, marketDataResult);
  }
  
  /**
   * Creates a link that will use the provided service context to resolve the link rather than use one available via a 
   * thread local environment.  Use of this method should only be necessary when you need to use resolution outside of 
   * the current VersionCorrection threadlocal environment.
   * @param <E> the type of the underlying Security the link refers to 
   * @param bundle the external id bundle to use as the link reference, not null
   * @param serviceContext a service context containing the SecuritySource and VersionCorrectionProvider necessary to resolve, not null
   * @return the security link
   */
  public static <E extends Security> SecurityLink<E> of(ExternalIdBundle bundle, ServiceContext serviceContext) {
    return new CustomResolverSecurityLink<E>(bundle, serviceContext);
  }
  
  /**
   * Creates a link that will use the provided service context to resolve the link rather than use one available via a 
   * thread local environment.  Use of this method should only be necessary when you need to use resolution outside of 
   * the current VersionCorrection threadlocal environment.  Links should be alternatively created from bundles where 
   * possible.
   * @param <E> the type of the underlying Security the link refers to
   * @param externalId a single ExternalId to use as the link reference, not null
   * @param serviceContext a service context containing the SecuritySource and VersionCorrectionProvider necessary to resolve, not null
   * @return the security link
   */
  public static <E extends Security> SecurityLink<E> of(ExternalId externalId, ServiceContext serviceContext) {
    return new CustomResolverSecurityLink<E>(externalId.toBundle(), serviceContext);
  }
  
  /**
   * Create a new SecurityLink, with the same ID bundle as this one that uses a newly provided serviceContext.  This should
   * only be necessary when you need to use reoslution outside of the current VersionCorrection threadlocal environment.
   * @param <E> the type of the underlying Security the link refers to
   * @param serviceContext a service context containing the SecuritySource and VersionCorrectionProvider necessary to resolve, not null
   * @return a new security link
   */
  public <E extends Security> SecurityLink<E> with(ServiceContext serviceContext) {
    return new CustomResolverSecurityLink<E>(_bundle, serviceContext);
  }
 
  /**
   * Resolve the link and get the underlying security
   * @return the security
   */
  public abstract T getSecurity();
  
  /**
   * Resolve the link and get the linked timeseries
   * @param request the request object
   * @return the time series
   */
  public abstract HistoricalTimeSeries getHistoricalData(HistoricalDataRequest request);
  
  public abstract MarketDataResult getCurrentData(String field);
  
  /**
   * Get the bundle on which the link is based
   * @return the bundle
   */
  public ExternalIdBundle getBundle() {
    return _bundle;
  }
}
