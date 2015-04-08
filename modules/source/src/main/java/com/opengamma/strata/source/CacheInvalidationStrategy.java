package com.opengamma.strata.source;

import com.google.common.cache.LoadingCache;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Created by julian on 21/10/14.
 */
public interface CacheInvalidationStrategy {

  public abstract CacheInvalidator createInvalidator(
      SourceProvider sourceProvider,
      LoadingCache<StandardId, ? extends IdentifiableBean> cache);
}
