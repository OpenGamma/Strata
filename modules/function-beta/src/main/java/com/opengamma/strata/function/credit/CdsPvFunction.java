package com.opengamma.strata.function.credit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDAInstrumentTypes;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculations.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.credit.Cds;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.general.reference.ReferenceInformationType;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConvention;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConventions;
import com.opengamma.strata.finance.credit.type.StandardCdsConvention;
import com.opengamma.strata.finance.credit.type.StandardCdsConventions;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.finance.rate.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.finance.rate.swap.type.IborRateSwapLegConvention;
import com.opengamma.strata.function.MarketDataRatesProvider;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.IntStream;

import static com.opengamma.strata.engine.calculations.function.FunctionUtils.toScenarioResult;

public class CdsPvFunction implements CalculationSingleFunction<CdsTrade, ScenarioResult<MultiCurrencyAmount>> {

  private final CdsAnalyticsWrapper _wrapper;

  public CdsPvFunction() {
    _wrapper = new CdsAnalyticsWrapper();
  }

  @Override
  public ScenarioResult<MultiCurrencyAmount> execute(CdsTrade trade, CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> price(trade, provider))
        .collect(toScenarioResult());
  }

  @Override
  public CalculationRequirements requirements(CdsTrade trade) {
    Cds cds = trade.getProduct();

    // Discount Curve

    // Credit Curve

    // Recovery Rate

    ReferenceInformationType cdsType = cds.getGeneralTerms().getReferenceInformation().getType();
    switch (cdsType) {
      case SINGLE_NAME:
        break;
      case INDEX:
        // Index Factor?
        break;
      default:
        throw new IllegalStateException("unknown reference information type: " + cdsType);
    }

    return CalculationRequirements.builder()
        .singleValueRequirements(Sets.newHashSet())
        .timeSeriesRequirements()
        .outputCurrencies(ImmutableSet.of(cds.getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getCalculationAmount().getCurrency()))
        .build();
  }

  private MultiCurrencyAmount price(CdsTrade trade, MarketDataRatesProvider provider) {
    // get market data from provider next

    LocalDate asOfDate = provider.getValuationDate();
    return _wrapper.price(asOfDate, trade.expand(), discountCurve(), creditCurve(), recoveryRate());
  }

  private CurveYieldPlaceholder discountCurve() {
    ImmutableList<String> raytheon20141020 = ImmutableList.of(
        "1M,M,0.001535",
        "2M,M,0.001954",
        "3M,M,0.002281",
        "6M,M,0.003217",
        "1Y,M,0.005444",
        "2Y,S,0.005905",
        "3Y,S,0.009555",
        "4Y,S,0.012775",
        "5Y,S,0.015395",
        "6Y,S,0.017445",
        "7Y,S,0.019205",
        "8Y,S,0.020660",
        "9Y,S,0.021885",
        "10Y,S,0.022940",
        "12Y,S,0.024615",
        "15Y,S,0.026300",
        "20Y,S,0.027950",
        "25Y,S,0.028715",
        "30Y,S,0.029160"
    );

    Period[] yieldCurvePoints = raytheon20141020
        .stream()
        .map(s -> Tenor.parse(s.split(",")[0]).getPeriod())
        .toArray(Period[]::new);
    ISDAInstrumentTypes[] yieldCurveInstruments = raytheon20141020
        .stream()
        .map(s -> (s.split(",")[1].equals("M") ? ISDAInstrumentTypes.MoneyMarket : ISDAInstrumentTypes.Swap))
        .toArray(ISDAInstrumentTypes[]::new);
    double[] rates = raytheon20141020
        .stream()
        .mapToDouble(s -> Double.valueOf(s.split(",")[2]))
        .toArray();

    IsdaYieldCurveConvention curveConvention = IsdaYieldCurveConventions.northAmericanUsd;
    return CurveYieldPlaceholder.of(yieldCurvePoints, yieldCurveInstruments, rates, curveConvention);
  }

  private CurveCreditPlaceholder creditCurve() {

    // ParSpreadQuote
    ImmutableList<String> raytheon20141020 = ImmutableList.of(
        "6M,0.0028",
        "1Y,0.0028",
        "2Y,0.0028",
        "3Y,0.0028",
        "4Y,0.0028",
        "5Y,0.0028"
    );

    Period[] creditCurvePoints = raytheon20141020
        .stream()
        .map(s -> Tenor.parse(s.split(",")[0]).getPeriod())
        .toArray(Period[]::new);
    double[] rates = raytheon20141020
        .stream()
        .mapToDouble(s -> Double.valueOf(s.split(",")[1]))
        .toArray();

    StandardCdsConvention cdsConvention = StandardCdsConventions.northAmericanUsd();

    return CurveCreditPlaceholder.of(creditCurvePoints, rates, cdsConvention);
  }

  private double recoveryRate() {
    return .40;
  }
}
