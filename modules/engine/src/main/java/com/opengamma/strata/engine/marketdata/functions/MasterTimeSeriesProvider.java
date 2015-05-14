///**
// * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
// * <p>
// * Please see distribution for license.
// */
//package com.opengamma.strata.engine.marketdata.functions;
//
//import com.opengamma.strata.collect.ArgChecker;
//import com.opengamma.strata.collect.result.Result;
//import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
//import com.opengamma.strata.marketdata.id.ObservableId;
//import com.opengamma.strata.masterdb.timeseries.TimeSeriesMaster;
//
///**
// * Time series provider which loads time series from a time series master.
// */
//class MasterTimeSeriesProvider implements TimeSeriesProvider {
//
//  /** The master containing the time series. */
//  private final TimeSeriesMaster master;
//
//  /**
//   * @param master the master containing the time series
//   */
//  MasterTimeSeriesProvider(TimeSeriesMaster master) {
//    this.master = ArgChecker.notNull(master, "master");
//  }
//
//  @Override
//  public Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id) {
//    return master.retrieve(id.getStandardId(), id.getFieldName().toString());
//  }
//}
