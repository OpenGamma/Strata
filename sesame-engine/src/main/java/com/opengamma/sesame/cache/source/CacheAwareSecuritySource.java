/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache.source;

import com.opengamma.core.security.Security;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.sesame.cache.CacheInvalidator;

/**
 *
 */
public class CacheAwareSecuritySource extends CacheAwareSourceWithExternalBundle<Security> implements SecuritySource {

  public CacheAwareSecuritySource(SecuritySource delegate, CacheInvalidator cacheInvalidator) {
    super(delegate, cacheInvalidator);
  }
}
