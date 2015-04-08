/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.source;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.joda.beans.MetaBean;
import org.joda.beans.Property;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.id.StandardIdentifiable;

public class CachingSourceProvider implements SourceProvider, UpdateNotificationListener {

  private final SourceProvider sourceProvider;

  private final LoadingCache<StandardId, ? extends IdentifiableBean> cache;

//  private final ConcurrentMap<StandardId, ? extends IdentifiableBean> pendingUpdates;

  // possible strategies:
  // - just update cache, accept will get mix of old and updated
  // - block all reads while cache is being updated
  // - copy cache before update, apply updates to copy, then switch to be live
//  private final CacheInvalidator cacheInvalidator;


  public CachingSourceProvider(SourceProvider source) {
    sourceProvider = ArgChecker.notNull(source, "sourceProvider");
    cache = CacheBuilder.newBuilder()
        .initialCapacity(100_000)
        .build(createLoader(sourceProvider));
    sourceProvider.registerForUpdates(this);
  }


  private CacheLoader<StandardId, IdentifiableBean> createLoader(final SourceProvider source) {

    return new CacheLoader<StandardId, IdentifiableBean>() {

      @Override
      public IdentifiableBean load(StandardId key) throws Exception {
        return source.get(key).orElse(MissingValue.INSTANCE);
      }

      @Override
      public Map<StandardId, IdentifiableBean> loadAll(Iterable<? extends StandardId> keys) {

        @SuppressWarnings("unchecked")
        Map<StandardId, IdentifiableBean> results = source.bulkGet((Iterable<StandardId>) keys);
        return Guavate.stream(keys)
            .collect(Guavate.<StandardId, StandardId, IdentifiableBean>toImmutableMap(
                k -> k,
                k -> results.containsKey(k) ? results.get(k) : MissingValue.INSTANCE
            ));
      }
    };
  }

  @Override
  public Optional<IdentifiableBean> get(StandardId id) {
    IdentifiableBean bean = cache.getUnchecked(id);
    return bean == MissingValue.INSTANCE ? Optional.empty() : Optional.of(bean);
  }

  @Override
  public ImmutableMap<StandardId, IdentifiableBean> bulkGet(Iterable<StandardId> ids) {
    try {
      Collection<? extends IdentifiableBean> beans = cache.getAll(ids).values();
      return beans.stream()
          // Need to filter out the missing values
          .filter(MissingValue.INSTANCE::equals)
          .collect(Guavate.<IdentifiableBean, StandardId>toImmutableMap(StandardIdentifiable::getStandardId));

    } catch (ExecutionException e) {
      throw new RuntimeException("Error whilst loading data into cache", e);
    }
  }

  @Override
  public ImmutableSet<StandardId> changedSince(Iterable<StandardId> ids, Instant checkpoint) {
    return sourceProvider.changedSince(ids, checkpoint);
  }

  @Override
  public void dataUpdated(Collection<IdentifiableBean> updates) {
//    Only want to insert data that we already have entries for
//    ConcurrentMap<StandardId, ? extends IdentifiableBean> cacheMap = cache.asMap();
//    Collectors.
  }

  @Override
  public void idsUpdated(Collection<StandardId> updates) {
  }

  /**
   * Class providing a sentinel value, used to indicate that no entry
   * could be found in the data store for a particular id. As the cache
   * cannot store null, but we must provide an answer for all requested
   * values, we use the singleton instance of this class. This provides
   * the added advantage that we will not query the provider again for a
   * value it does not hold.
   * <p>
   * All method implementations throw {@link UnsupportedOperationException}s
   * as there should never be any need to call any of them. This
   * implementation should never leak outside of the enclosing class.
   */
  private static final class MissingValue implements IdentifiableBean {

    /**
     * Singleton sentinel value indicating that no value could
     * be found for a particular id.
     */
    private static final MissingValue INSTANCE = new MissingValue();

    /**
     * Private constructor to prevent instantiation of
     * anything other than the singleton instance.
     */
    private MissingValue() {
    }

    @Override
    public MetaBean metaBean() {
      throw new UnsupportedOperationException("Methods should not be called on this bean");
    }

    @Override
    public <R> Property<R> property(String propertyName) {
      throw new UnsupportedOperationException("Methods should not be called on this bean");
    }

    @Override
    public Set<String> propertyNames() {
      throw new UnsupportedOperationException("Methods should not be called on this bean");
    }

    @Override
    public StandardId getStandardId() {
      throw new UnsupportedOperationException("Methods should not be called on this bean");
    }
  }
}
