/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache.source;

import com.opengamma.core.region.Region;
import com.opengamma.core.region.RegionSource;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class CacheAwareRegionSource extends CacheAwareSourceWithExternalBundle<Region> implements RegionSource {

  private final RegionSource _delegate;

  public CacheAwareRegionSource(RegionSource delegate, CacheInvalidator cacheInvalidator) {
    super(delegate, cacheInvalidator);
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public Region getHighestLevelRegion(ExternalId externalId) {
    return register(_delegate.getHighestLevelRegion(externalId));
  }

  @Override
  public Region getHighestLevelRegion(ExternalIdBundle bundle) {
    return register(_delegate.getHighestLevelRegion(bundle));
  }
}
