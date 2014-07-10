/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.Holiday;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.region.Region;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.financial.convention.DefaultConventionBundleSource;
import com.opengamma.financial.convention.InMemoryConventionBundleMaster;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ExternalIdBundleWithDates;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueIdentifiable;
import com.opengamma.id.VersionCorrection;
import com.opengamma.master.config.ConfigDocument;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.impl.InMemoryConfigMaster;
import com.opengamma.master.config.impl.MasterConfigSource;
import com.opengamma.master.convention.ConventionDocument;
import com.opengamma.master.convention.ConventionMaster;
import com.opengamma.master.convention.ManageableConvention;
import com.opengamma.master.convention.impl.InMemoryConventionMaster;
import com.opengamma.master.convention.impl.MasterConventionSource;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesInfoDocument;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.master.historicaltimeseries.ManageableHistoricalTimeSeriesInfo;
import com.opengamma.master.historicaltimeseries.impl.DefaultHistoricalTimeSeriesResolver;
import com.opengamma.master.historicaltimeseries.impl.DefaultHistoricalTimeSeriesSelector;
import com.opengamma.master.historicaltimeseries.impl.InMemoryHistoricalTimeSeriesMaster;
import com.opengamma.master.historicaltimeseries.impl.MasterHistoricalTimeSeriesSource;
import com.opengamma.master.holiday.HolidayDocument;
import com.opengamma.master.holiday.HolidayMaster;
import com.opengamma.master.holiday.impl.InMemoryHolidayMaster;
import com.opengamma.master.holiday.impl.MasterHolidaySource;
import com.opengamma.master.region.RegionDocument;
import com.opengamma.master.region.RegionMaster;
import com.opengamma.master.region.impl.InMemoryRegionMaster;
import com.opengamma.master.region.impl.MasterRegionSource;
import com.opengamma.master.security.ManageableSecurity;
import com.opengamma.master.security.SecurityDocument;
import com.opengamma.master.security.SecurityMaster;
import com.opengamma.master.security.impl.InMemorySecurityMaster;
import com.opengamma.master.security.impl.MasterSecuritySource;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.EngineTestUtils;
import com.opengamma.sesame.engine.CycleArguments;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.engine.ViewInputs;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.marketdata.CycleMarketDataFactory;
import com.opengamma.sesame.marketdata.DefaultStrategyAwareMarketDataSource;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.HtsRequestKey;
import com.opengamma.sesame.marketdata.MapMarketDataSource;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Enables the running of a view using the data captured from a
 * previous run of the view. This means a view can be rerun
 * indefinitely and does not need external access to market data,
 * trade data, config data etc. This is useful for performing
 * regression testing.
 */
public class CapturedResultsLoader {

  private final ViewInputs _viewInputs;
  private final AvailableOutputs _availableOutputs;
  private final AvailableImplementations _availableImplementations;
  private final Multimap<String, ConfigItem<?>> additionalConfigData = HashMultimap.create();

  /**
   * Creates a new loader.
   *
   * @param viewInputs the inputs recorded for the view
   * @param availableOutputs the available outputs
   * @param availableImplementations the available implementations
   */
  public CapturedResultsLoader(ViewInputs viewInputs,
                               AvailableOutputs availableOutputs,
                               AvailableImplementations availableImplementations) {
    _viewInputs = ArgumentChecker.notNull(viewInputs, "viewInputs");
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "availableOutputs");
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "availableImplementations");
  }

  /**
   * Run the view using the captured data, returning the output results.
   *
   * @return the results of running the view
   */
  public Results runViewFromInputs() {

    // Now build the config source, market data sources etc
    Multimap<Class<?>, UniqueIdentifiable> configData = _viewInputs.getConfigData();

    ImmutableMap<Class<?>, Object> components = createComponents(configData);

    ViewFactory viewFactory =
        EngineTestUtils.createViewFactory(components, _availableOutputs, _availableImplementations);

    Set<Class<?>> inputTypes = extractInputTypes(_viewInputs.getTradeInputs());
    View view = viewFactory.createView(_viewInputs.getViewConfig(), inputTypes);

    ZonedDateTime valTime = _viewInputs.getValuationTime();

    final Map<ZonedDateTime, Map<Pair<ExternalIdBundle, FieldName>, Result<?>>> marketData =
        _viewInputs.getMarketData();

    // Create a cycle market data factory that returns the market data from
    // the previously captured data
    CycleMarketDataFactory cycleMarketDataFactory = new CycleMarketDataFactory() {
      @Override
      public MarketDataSource getPrimaryMarketDataSource() {
        MarketDataSource marketDataSource = MapMarketDataSource.of(marketData.get(LocalDate.MAX.atStartOfDay(
            ZoneOffset.UTC)));
        // LiveSpec as good as any - won't actually be used for anything
        return new DefaultStrategyAwareMarketDataSource(LiveMarketDataSpecification.LIVE_SPEC, marketDataSource);
      }

      @Override
      public MarketDataSource getMarketDataSourceForDate(ZonedDateTime valuationDate) {
        return MapMarketDataSource.of(marketData.get(valuationDate));
      }

      @Override
      public CycleMarketDataFactory withMarketDataSpecification(MarketDataSpecification marketDataSpec) {
        throw new UnsupportedOperationException();
      }

      @Override
      public CycleMarketDataFactory withPrimedMarketDataSource() {
        throw new UnsupportedOperationException();
      }
    };

    VersionCorrection versionCorrection =
        ThreadLocalServiceContext.getInstance().get(VersionCorrectionProvider.class).getConfigVersionCorrection();

    CycleArguments cycleArguments = new CycleArguments(valTime, versionCorrection, cycleMarketDataFactory);
    return view.run(cycleArguments, _viewInputs.getTradeInputs());
  }

  private Set<Class<?>> extractInputTypes(List<Object> tradeInputs) {
    Set<Class<?>> inputTypes = new HashSet<>();
    for (Object tradeInput : tradeInputs) {
      inputTypes.add(tradeInput.getClass());
    }
    return inputTypes;
  }

  private ImmutableMap<Class<?>, Object> createComponents(Multimap<Class<?>, UniqueIdentifiable> configData) {
    ConfigMaster configMaster = new InMemoryConfigMaster();
    for (Map.Entry<String, ConfigItem<?>> entry : additionalConfigData.entries()) {
      ConfigDocument document = new ConfigDocument(entry.getValue());
      document.setName(entry.getKey());
      configMaster.add(document);
    }
    for (UniqueIdentifiable item : configData.get(ConfigSource.class)) {
      configMaster.add(new ConfigDocument((ConfigItem<?>) item));
    }
    ConfigSource configSource = new MasterConfigSource(configMaster);

    SecurityMaster securityMaster = new InMemorySecurityMaster();
    for (UniqueIdentifiable item : configData.get(SecuritySource.class)) {
      securityMaster.add(new SecurityDocument((ManageableSecurity) item));
    }
    SecuritySource securitySource = new MasterSecuritySource(securityMaster);

    ConventionMaster conventionMaster = new InMemoryConventionMaster();
    for (UniqueIdentifiable item : configData.get(ConventionSource.class)) {
      conventionMaster.add(new ConventionDocument((ManageableConvention) item));
    }

    ConventionSource conventionSource = new MasterConventionSource(conventionMaster);

    HolidayMaster holidayMaster = new InMemoryHolidayMaster();
    for (UniqueIdentifiable item : configData.get(HolidaySource.class)) {
      holidayMaster.add(new HolidayDocument((Holiday) item));
    }

    HolidaySource holidaySource = new MasterHolidaySource(holidayMaster);

    RegionMaster regionMaster = new InMemoryRegionMaster();
    for (UniqueIdentifiable item : configData.get(RegionSource.class)) {
      regionMaster.add(new RegionDocument((Region) item));
    }

    RegionSource regionSource = new MasterRegionSource(regionMaster);
    ConventionBundleSource conventionBundleSource =
        new DefaultConventionBundleSource(new InMemoryConventionBundleMaster());

    InMemoryHistoricalTimeSeriesMaster htsMaster = new InMemoryHistoricalTimeSeriesMaster();
    HistoricalTimeSeriesResolver resolver = new DefaultHistoricalTimeSeriesResolver(
        new DefaultHistoricalTimeSeriesSelector(configSource), htsMaster);
    HistoricalTimeSeriesSource historicalTimeSeriesSource =
        new MasterHistoricalTimeSeriesSource(htsMaster, resolver);

    Multimap<HtsRequestKey, LocalDateDoubleTimeSeries> htsData = _viewInputs.getHtsData();
    for (HtsRequestKey htsKey : htsData.keySet()) {
      ManageableHistoricalTimeSeriesInfo htsInfo = new ManageableHistoricalTimeSeriesInfo();
      htsInfo.setExternalIdBundle(ExternalIdBundleWithDates.of(htsKey.getIdentifierBundle()));
      htsInfo.setDataSource(htsKey.getDataSource() == null ? "Blah" : htsKey.getDataSource());
      htsInfo.setDataProvider(htsKey.getDataProvider() == null ? "Blah" : htsKey.getDataProvider());
      htsInfo.setObservationTime("blah");
      htsInfo.setDataField(htsKey.getDataField());

      ObjectId objectId  = htsMaster.add(new HistoricalTimeSeriesInfoDocument(htsInfo)).getObjectId();

      Collection<LocalDateDoubleTimeSeries> series = htsData.get(htsKey);
      if (series.size() == 1) {
        htsMaster.updateTimeSeriesDataPoints(objectId, series.iterator().next());
      } else {

        List<LocalDateDoubleTimeSeries> sorted = new ArrayList<>(series);
        Collections.sort(sorted, new Comparator<LocalDateDoubleTimeSeries>() {
          @Override
          public int compare(LocalDateDoubleTimeSeries o1, LocalDateDoubleTimeSeries o2) {
            return o1.getEarliestTimeFast() - o2.getEarliestTimeFast();
          }
        });

        int previousEnd = Integer.MIN_VALUE;
        for (LocalDateDoubleTimeSeries timeSeries : sorted) {

          if (timeSeries.getEarliestTimeFast() > previousEnd) {

            htsMaster.updateTimeSeriesDataPoints(objectId, timeSeries);
            previousEnd = timeSeries.getLatestTimeFast();

          } else if (timeSeries.getLatestTimeFast() > previousEnd) {

            // This timeseries overlaps with the previous, we
            // need to extract the dates from this one that weren't
            // in the previous
            LocalDateDoubleTimeSeries subSeries =
                timeSeries.subSeriesFast(previousEnd, timeSeries.getLatestTimeFast());
            htsMaster.updateTimeSeriesDataPoints(objectId, subSeries);
            previousEnd = timeSeries.getLatestTimeFast();
          }
        }
      }
    }

    return ImmutableMap.<Class<?>, Object>builder()
        .put(ConfigSource.class, configSource)
        .put(ConventionSource.class, conventionSource)
        .put(SecuritySource.class, securitySource)
        .put(HolidaySource.class, holidaySource)
        .put(RegionSource.class, regionSource)
        .put(HistoricalTimeSeriesSource.class, historicalTimeSeriesSource)
        .put(HistoricalTimeSeriesResolver.class, resolver)
        .put(ConventionBundleSource.class, conventionBundleSource)
        .build();
  }

  /**
   * Add extra config data items. Intended for items that cannot currently
   * be captured by the ViewInputs e.g. config links.
   *
   * @param data  config data to be added
   */
  public void addExtraConfigData(ConfigItem<?> data) {
    addExtraConfigData(data.getName(), data);
  }


  /**
   * Add extra config data items with a specific name. Intended for items
   * that cannot currently be captured by the ViewInputs e.g. config links.
   *
   * @param name  the name for the config item
   * @param data  config data to be added
   */
  public void addExtraConfigData(String name, ConfigItem<?> data) {
    additionalConfigData.put(ArgumentChecker.notEmpty(name, "name"), ArgumentChecker.notNull(data, "data"));
  }
}
