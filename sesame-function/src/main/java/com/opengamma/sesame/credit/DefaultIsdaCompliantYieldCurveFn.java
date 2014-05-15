/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurveBuild;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDAInstrumentTypes;
import com.opengamma.analytics.financial.schedule.ScheduleCalculator;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.financial.analytics.conversion.CalendarUtils;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.curve.IsdaYieldCurveDefinition;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.DataFieldType;
import com.opengamma.financial.analytics.ircurve.strips.IsdaYieldCurveNode;
import com.opengamma.financial.convention.IsdaYieldCurveConvention;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;

/**
 * Default implementation. Builds an ISDA yield curve from the given {@link IsdaYieldCurveDefinition}.
 * The returned {@link ISDACompliantYieldCurve} is the yield curve used in ISDA compliant analytic
 * credit computations.
 */
public class DefaultIsdaCompliantYieldCurveFn implements IsdaCompliantYieldCurveFn {


  private final CurveSpecificationMarketDataFn _curveSpecMarketDataProvider;
  
  //TODO SSM-293 - these should not be necessary in future 
  //once we switch to using a CalendarLink
  private final RegionSource _regionSource;
  private final HolidaySource _holidaySource;
  
  /**
   * Constructs the function.
   * @param curveSpecMarketDataProvider used to resolve market data for the derived curve spec
   * @param regionSource used for resolving calendar data (see SSM-293)
   * @param holidaySource used for resolving calendar data (see SSM-293)
   */
  public DefaultIsdaCompliantYieldCurveFn(CurveSpecificationMarketDataFn curveSpecMarketDataProvider, 
      RegionSource regionSource, HolidaySource holidaySource) {
    _curveSpecMarketDataProvider = ArgumentChecker.notNull(curveSpecMarketDataProvider, "curveSpecMarketDataProvider");
    _regionSource = ArgumentChecker.notNull(regionSource, "regionSource");
    _holidaySource = ArgumentChecker.notNull(holidaySource, "holidaySource");
  }


  @Override
  public Result<ISDACompliantYieldCurve> buildISDACompliantCurve(Environment env, IsdaYieldCurveDefinition definition) {
  
    CurveSpecification spec = buildSpec(definition, env.getValuationDate());
    
    Result<Map<ExternalIdBundle, Double>> marketDataResult =
        _curveSpecMarketDataProvider.requestData(env, spec);
    
    if (marketDataResult.isSuccess()) {
      return Result.success(buildCurve(env.getValuationDate(), definition, marketDataResult.getValue()));
    } else {
      return Result.failure(marketDataResult);
    }
  }


  /**
   * Builds a {@link CurveSpecification} used to resolve the market data. This would previously have been
   * done in conjunction with the CurveNodeIdMapper. However since tickers and other relevant data are
   * stored on the definition itself, the curve spec can be built without reference to any such object.
   * 
   * Converting to a {@link CurveSpecification} object is not strictly necessary since all necessary ids
   * are stored on the node. However, calling the {@link CurveSpecificationMarketDataFn} to load market
   * data allows us to reuse other implementations, including scenario type classes.
   */
  private CurveSpecification buildSpec(IsdaYieldCurveDefinition definition, LocalDate valuationDate) {
    //no need to use the normal CurveSpecificationFn for this since no CurveNodeIdMapper
    //exists for ISDAYieldCurveDefinitions. Tickers live on the definition itself.
    SortedSet<CurveNodeWithIdentifier> nodes = Sets.newTreeSet();
    
    for (IsdaYieldCurveNode node : definition.getNodes()) {
      CurveNodeWithIdentifier nodeWithId = new CurveNodeWithIdentifier(node, 
                                                node.getTicker(), 
                                                node.getDataField(), 
                                                DataFieldType.OUTRIGHT);
      nodes.add(nodeWithId);
    }
    
    return new CurveSpecification(valuationDate, definition.getName(), nodes);
  }


  /**
   * Builds the curve using the curve definition and the resolved market data.
   */
  private ISDACompliantYieldCurve buildCurve(LocalDate valuationDate, 
                                             IsdaYieldCurveDefinition definition, 
                                             Map<ExternalIdBundle, Double> marketData) {
    
    IsdaYieldCurveConvention ycConvention = definition.getCurveConventionLink().resolve();
    
    //TODO SSM-293 - switch to use a CalendarLink
    Calendar ycCalendar = CalendarUtils.getCalendar(_regionSource, _holidaySource, ycConvention.getRegionCalendar());
    
    LocalDate spotDate = ScheduleCalculator.getAdjustedDate(valuationDate, 
                                                            ycConvention.getSettlementDays(), 
                                                            ycCalendar);
    
    int curveNodesLength = definition.getNodes().size();
    
    Period[] periods = new Period[curveNodesLength];
    double[] rates = new double[curveNodesLength];
    ISDAInstrumentTypes[] instrumentTypes = new ISDAInstrumentTypes[curveNodesLength];
    
    List<IsdaYieldCurveNode> nodeList = Lists.newArrayList(definition.getNodes());

    for (int i = 0; i < curveNodesLength; i++) {
      IsdaYieldCurveNode node = nodeList.get(i);
      
      ExternalId ticker = node.getTicker();
      Tenor tenor = node.getResolvedMaturity();
      periods[i] = tenor.getPeriod();
      instrumentTypes[i] = node.getISDAInstrumentType();
      
      rates[i] = marketData.get(ticker.toBundle());
    }
    
    ISDACompliantYieldCurveBuild builder = new ISDACompliantYieldCurveBuild(
            valuationDate, 
            spotDate, 
            instrumentTypes, 
            periods,
            ycConvention.getCashDayCount(),
            ycConvention.getSwapFixedLegDayCount(),
            ycConvention.getSwapFixedLegInterval(),
            ycConvention.getCurveDayCount(),
            ycConvention.getBusinessDayConvention(),
            ycCalendar
    );
    
    return builder.build(rates);
  }


}
