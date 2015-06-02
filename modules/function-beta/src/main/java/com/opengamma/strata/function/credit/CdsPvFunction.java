package com.opengamma.strata.function.credit;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculations.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.credit.Cds;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.general.reference.ReferenceInformationType;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.market.curve.NodalCurve;

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

    ReferenceInformationType cdsType = cds.getGeneralTerms().getReferenceInformation().getType();
    switch (cdsType) {
      case SINGLE_NAME:
        // Recovery Rate
        break;
      case INDEX:
        // Other Thingy
        break;
      default:
        throw new IllegalStateException("unknown reference information type: " + cdsType);
    }

    return CalculationRequirements.builder()
        .singleValueRequirements(Sets.newHashSet())
        .timeSeriesRequirements()
        .outputCurrencies(ImmutableSet.of(cds.getFeeLeg().getPeriodicPayments().getCalculationAmount().getCurrency()))
        .build();
  }

  private MultiCurrencyAmount price(CdsTrade trade, MarketDataRatesProvider provider) {
    // get these from provider next

    return _wrapper.price(trade, discountCurve(), creditCurve(), recoveryRate());
  }


  private NodalCurve discountCurve() {
    return null; // will use raw analytic curves for now
  }

  private NodalCurve creditCurve() {
    return null;
  }

  private double recoveryRate() {
    return .40;
  }
}
