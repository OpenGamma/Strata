/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache.source;

import com.opengamma.core.convention.Convention;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class CacheAwareConventionSource extends CacheAwareSourceWithExternalBundle<Convention>
    implements ConventionSource {

  private final ConventionSource _delegate;

  public CacheAwareConventionSource(ConventionSource delegate, CacheInvalidator cacheInvalidator) {
    super(delegate, cacheInvalidator);
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public <T extends Convention> T get(UniqueId uniqueId, Class<T> type) {
    return register(_delegate.get(uniqueId, type));
  }

  @Override
  public <T extends Convention> T get(ObjectId objectId, VersionCorrection versionCorrection, Class<T> type) {
    return register(_delegate.get(objectId, versionCorrection, type));
  }

  @Override
  public <T extends Convention> T getSingle(ExternalIdBundle bundle, VersionCorrection versionCorrection, Class<T> type) {
    return register(_delegate.getSingle(bundle, versionCorrection, type));
  }

  @Override
  public Convention getSingle(ExternalId externalId) {
    return register(_delegate.getSingle(externalId));
  }

  @Override
  public <T extends Convention> T getSingle(ExternalId externalId, Class<T> type) {
    return register(_delegate.getSingle(externalId, type));
  }

  @Override
  public <T extends Convention> T getSingle(ExternalIdBundle bundle, Class<T> type) {
    return register(_delegate.getSingle(bundle, type));
  }
}
