/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache.source;

import java.util.Map;
import java.util.Set;

import org.threeten.bp.LocalDate;

import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 *
 */
public class CacheAwareHistoricalTimeSeriesSource implements HistoricalTimeSeriesSource {

  private final HistoricalTimeSeriesSource _delegate;
  private final CacheInvalidator _cacheInvalidator;

  public CacheAwareHistoricalTimeSeriesSource(HistoricalTimeSeriesSource delegate, CacheInvalidator cacheInvalidator) {
    _cacheInvalidator = ArgumentChecker.notNull(cacheInvalidator, "cacheInvalidator");
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  private static Pair<LocalDate, Double> getLatestDataPoint(HistoricalTimeSeries timeSeries) {
    if (timeSeries == null) {
      return null;
    } else {
      LocalDateDoubleTimeSeries dateTimeSeries = timeSeries.getTimeSeries();
      return Pairs.of(dateTimeSeries.getLatestTime(), dateTimeSeries.getLatestValueFast());
    }
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(UniqueId uniqueId) {
    return register(_delegate.getHistoricalTimeSeries(uniqueId));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(UniqueId uniqueId,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd) {
    return register(_delegate.getHistoricalTimeSeries(uniqueId, start, includeStart, end, includeEnd));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(UniqueId uniqueId,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd,
                                                      int maxPoints) {
    return register(_delegate.getHistoricalTimeSeries(uniqueId, start, includeStart, end, includeEnd, maxPoints));
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(UniqueId uniqueId) {
    HistoricalTimeSeries timeSeries = _delegate.getHistoricalTimeSeries(uniqueId);
    return getLatestDataPoint(timeSeries);
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(UniqueId uniqueId,
                                                    LocalDate start,
                                                    boolean includeStart,
                                                    LocalDate end,
                                                    boolean includeEnd) {
    return getLatestDataPoint(_delegate.getHistoricalTimeSeries(uniqueId, start, includeStart, end, includeEnd));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(ExternalIdBundle identifierBundle,
                                                      String dataSource,
                                                      String dataProvider,
                                                      String dataField) {
    return register(_delegate.getHistoricalTimeSeries(identifierBundle, dataSource, dataProvider, dataField));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(ExternalIdBundle identifierBundle,
                                                      String dataSource,
                                                      String dataProvider,
                                                      String dataField,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd) {
    return register(_delegate.getHistoricalTimeSeries(identifierBundle,
                                                      dataSource,
                                                      dataProvider,
                                                      dataField,
                                                      start,
                                                      includeStart,
                                                      end,
                                                      includeEnd));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(ExternalIdBundle identifierBundle,
                                                      String dataSource,
                                                      String dataProvider,
                                                      String dataField,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd,
                                                      int maxPoints) {
    return register(_delegate.getHistoricalTimeSeries(identifierBundle,
                                                      dataSource,
                                                      dataProvider,
                                                      dataField,
                                                      start,
                                                      includeStart,
                                                      end,
                                                      includeEnd,
                                                      maxPoints));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(ExternalIdBundle identifierBundle,
                                                      LocalDate identifierValidityDate,
                                                      String dataSource,
                                                      String dataProvider,
                                                      String dataField) {
    return register(_delegate.getHistoricalTimeSeries(identifierBundle,
                                                      identifierValidityDate,
                                                      dataSource,
                                                      dataProvider,
                                                      dataField));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(ExternalIdBundle identifierBundle,
                                                      LocalDate identifierValidityDate,
                                                      String dataSource,
                                                      String dataProvider,
                                                      String dataField,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd) {
    return register(_delegate.getHistoricalTimeSeries(identifierBundle,
                                                      identifierValidityDate,
                                                      dataSource,
                                                      dataProvider,
                                                      dataField,
                                                      start,
                                                      includeStart,
                                                      end,
                                                      includeEnd));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(ExternalIdBundle identifierBundle,
                                                      LocalDate identifierValidityDate,
                                                      String dataSource,
                                                      String dataProvider,
                                                      String dataField,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd,
                                                      int maxPoints) {
    return register(_delegate.getHistoricalTimeSeries(identifierBundle,
                                                      identifierValidityDate,
                                                      dataSource,
                                                      dataProvider,
                                                      dataField,
                                                      start,
                                                      includeStart,
                                                      end,
                                                      includeEnd,
                                                      maxPoints));
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(ExternalIdBundle identifierBundle,
                                                    LocalDate identifierValidityDate,
                                                    String dataSource,
                                                    String dataProvider,
                                                    String dataField) {
    return getLatestDataPoint(_delegate.getHistoricalTimeSeries(identifierBundle,
                                                                identifierValidityDate,
                                                                dataSource,
                                                                dataProvider,
                                                                dataField));
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(ExternalIdBundle identifierBundle,
                                                    LocalDate identifierValidityDate,
                                                    String dataSource,
                                                    String dataProvider,
                                                    String dataField,
                                                    LocalDate start,
                                                    boolean includeStart,
                                                    LocalDate end,
                                                    boolean includeEnd) {
    return getLatestDataPoint(_delegate.getHistoricalTimeSeries(identifierBundle,
                                                                identifierValidityDate,
                                                                dataSource,
                                                                dataProvider,
                                                                dataField,
                                                                start,
                                                                includeStart,
                                                                end,
                                                                includeEnd));
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(ExternalIdBundle identifierBundle,
                                                    String dataSource,
                                                    String dataProvider,
                                                    String dataField) {
    return getLatestDataPoint(_delegate.getHistoricalTimeSeries(identifierBundle, dataSource, dataProvider, dataField));
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(ExternalIdBundle identifierBundle,
                                                    String dataSource,
                                                    String dataProvider,
                                                    String dataField,
                                                    LocalDate start,
                                                    boolean includeStart,
                                                    LocalDate end,
                                                    boolean includeEnd) {
    return getLatestDataPoint(_delegate.getHistoricalTimeSeries(identifierBundle,
                                                                dataSource,
                                                                dataProvider,
                                                                dataField,
                                                                start,
                                                                includeStart,
                                                                end,
                                                                includeEnd));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(String dataField,
                                                      ExternalIdBundle identifierBundle,
                                                      String resolutionKey) {
    return register(_delegate.getHistoricalTimeSeries(dataField, identifierBundle, resolutionKey));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(String dataField,
                                                      ExternalIdBundle identifierBundle,
                                                      String resolutionKey,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd) {
    return register(_delegate.getHistoricalTimeSeries(dataField,
                                                      identifierBundle,
                                                      resolutionKey,
                                                      start,
                                                      includeStart,
                                                      end,
                                                      includeEnd));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(String dataField,
                                                      ExternalIdBundle identifierBundle,
                                                      String resolutionKey,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd, int maxPoints) {
    return register(_delegate.getHistoricalTimeSeries(dataField,
                                                      identifierBundle,
                                                      resolutionKey,
                                                      start,
                                                      includeStart,
                                                      end,
                                                      includeEnd,
                                                      maxPoints));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(String dataField,
                                                      ExternalIdBundle identifierBundle,
                                                      LocalDate identifierValidityDate,
                                                      String resolutionKey) {
    return register(_delegate.getHistoricalTimeSeries(dataField, identifierBundle, identifierValidityDate, resolutionKey));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(String dataField,
                                                      ExternalIdBundle identifierBundle,
                                                      LocalDate identifierValidityDate,
                                                      String resolutionKey,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd) {
    return register(_delegate.getHistoricalTimeSeries(dataField,
                                                      identifierBundle,
                                                      identifierValidityDate,
                                                      resolutionKey,
                                                      start,
                                                      includeStart,
                                                      end,
                                                      includeEnd));
  }

  @Override
  public HistoricalTimeSeries getHistoricalTimeSeries(String dataField,
                                                      ExternalIdBundle identifierBundle,
                                                      LocalDate identifierValidityDate,
                                                      String resolutionKey,
                                                      LocalDate start,
                                                      boolean includeStart,
                                                      LocalDate end,
                                                      boolean includeEnd,
                                                      int maxPoints) {
    return register(_delegate.getHistoricalTimeSeries(dataField,
                                                      identifierBundle,
                                                      identifierValidityDate,
                                                      resolutionKey,
                                                      start,
                                                      includeStart,
                                                      end,
                                                      includeEnd,
                                                      maxPoints));
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(String dataField,
                                                    ExternalIdBundle identifierBundle,
                                                    String resolutionKey) {
    return getLatestDataPoint(_delegate.getHistoricalTimeSeries(dataField, identifierBundle, resolutionKey));
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(String dataField,
                                                    ExternalIdBundle identifierBundle,
                                                    String resolutionKey,
                                                    LocalDate start,
                                                    boolean includeStart,
                                                    LocalDate end,
                                                    boolean includeEnd) {
    return getLatestDataPoint(_delegate.getHistoricalTimeSeries(dataField,
                                                                identifierBundle,
                                                                resolutionKey,
                                                                start,
                                                                includeStart,
                                                                end,
                                                                includeEnd));
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(String dataField,
                                                    ExternalIdBundle identifierBundle,
                                                    LocalDate identifierValidityDate,
                                                    String resolutionKey) {
    return getLatestDataPoint(_delegate.getHistoricalTimeSeries(dataField, identifierBundle, resolutionKey));
  }

  @Override
  public Pair<LocalDate, Double> getLatestDataPoint(String dataField,
                                                    ExternalIdBundle identifierBundle,
                                                    LocalDate identifierValidityDate,
                                                    String resolutionKey,
                                                    LocalDate start,
                                                    boolean includeStart,
                                                    LocalDate end,
                                                    boolean includeEnd) {
    return getLatestDataPoint(_delegate.getHistoricalTimeSeries(dataField,
                                                                identifierBundle,
                                                                identifierValidityDate,
                                                                resolutionKey,
                                                                start,
                                                                includeStart,
                                                                end,
                                                                includeEnd));
  }

  @Override
  public Map<ExternalIdBundle, HistoricalTimeSeries> getHistoricalTimeSeries(Set<ExternalIdBundle> identifierSet,
                                                                             String dataSource,
                                                                             String dataProvider,
                                                                             String dataField,
                                                                             LocalDate start,
                                                                             boolean includeStart,
                                                                             LocalDate end,
                                                                             boolean includeEnd) {
    return register(_delegate.getHistoricalTimeSeries(identifierSet,
                                                      dataSource,
                                                      dataProvider,
                                                      dataField,
                                                      start,
                                                      includeStart,
                                                      end,
                                                      includeEnd));
  }

  @Override
  public ExternalIdBundle getExternalIdBundle(UniqueId uniqueId) {
    ExternalIdBundle bundle = _delegate.getExternalIdBundle(uniqueId);
    if (bundle != null) {
      _cacheInvalidator.register(uniqueId.getObjectId());
    }
    return bundle;
  }

  @Override
  public ChangeManager changeManager() {
    return _delegate.changeManager();
  }

  private <K> Map<K, HistoricalTimeSeries> register(Map<K, HistoricalTimeSeries> items) {
    for (HistoricalTimeSeries timeSeries : items.values()) {
      register(timeSeries);
    }
    return items;
  }

  private HistoricalTimeSeries register(HistoricalTimeSeries timeSeries) {
    if (timeSeries != null) {
      _cacheInvalidator.register(timeSeries.getUniqueId().getObjectId());
    }
    return timeSeries;
  }
}
