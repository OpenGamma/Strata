/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.measures;

import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.security.credit.LegacyCDSSecurity;
import com.opengamma.financial.security.credit.StandardCDSSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.CdsData;
import com.opengamma.sesame.credit.IsdaCompliantCreditCurveFn;
import com.opengamma.sesame.credit.IsdaCreditCurve;
import com.opengamma.sesame.credit.converter.LegacyCdsConverterFn;
import com.opengamma.sesame.credit.converter.StandardCdsConverterFn;
import com.opengamma.sesame.credit.market.CreditMarketDataResolverFn;
import com.opengamma.sesame.credit.market.LegacyCdsMarketDataResolverFn;
import com.opengamma.sesame.credit.market.StandardCdsMarketDataResolverFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Abstract implementation of a credit risk measure, e.g. pv, cs01, etc. This
 * class enforces a template for the generation of credit risk measures. The
 * standard steps are:
 * <li> resolve market data for security
 * <li> convert security to its analytics type
 * <li> extract any required meta-data into {@link CdsData}
 * <li> price
 * 
 * All steps are implemented except the last.
 * 
 * @param <T> the type of risk measure this function produces
 */
public abstract class AbstractCreditRiskMeasureFn<T> implements CreditRiskMesasureFn<T> {
  
  private final LegacyCdsConverterFn _legacyCdsConverterFn;
  private final StandardCdsConverterFn _standardCdsConverterFn;
  private final StandardCdsMarketDataResolverFn _standardCdsMarketDataResolverFn;
  private final LegacyCdsMarketDataResolverFn _legacyCdsMarketDataResolverFn;
  private final IsdaCompliantCreditCurveFn _creditCurveFn;
  
  /**
   * Creates an instance.
   * 
   * @param legacyCdsConverterFn a legacy cds converter
   * @param standardCdsConverterFn a standard cds converter
   * @param standardCdsMarketDataResolverFn a market data resolver for standard cds
   * @param legacyCdsMarketDataResolverFn a market data resolver for legacy cds
   * @param creditCurveFn the credit curve function
   */
  protected AbstractCreditRiskMeasureFn(LegacyCdsConverterFn legacyCdsConverterFn, 
                                     StandardCdsConverterFn standardCdsConverterFn, 
                                     StandardCdsMarketDataResolverFn standardCdsMarketDataResolverFn,
                                     LegacyCdsMarketDataResolverFn legacyCdsMarketDataResolverFn, 
                                     IsdaCompliantCreditCurveFn creditCurveFn) {
    _legacyCdsConverterFn = ArgumentChecker.notNull(legacyCdsConverterFn, "legacyCdsConverterFn");
    _standardCdsConverterFn = ArgumentChecker.notNull(standardCdsConverterFn, "standardCdsConverterFn");
    _standardCdsMarketDataResolverFn = ArgumentChecker.notNull(standardCdsMarketDataResolverFn, 
                                                               "standardCdsMarketDataResolverFn");
    _legacyCdsMarketDataResolverFn = ArgumentChecker.notNull(legacyCdsMarketDataResolverFn, 
                                                             "legacyCdsMarketDataResolverFn");
    _creditCurveFn = ArgumentChecker.notNull(creditCurveFn, "creditCurveFn");
  }

  @Override
  public Result<T> priceLegacyCds(Environment env, LegacyCDSSecurity cds) {
    
    Result<IsdaCreditCurve> marketDataResult = resolveMarketData(env, _legacyCdsMarketDataResolverFn, cds);
        
    //not much we can do if we can't resolve/build market data
    if (!marketDataResult.isSuccess()) {
      return Result.failure(marketDataResult);
    }
    
    IsdaCreditCurve creditCurve = marketDataResult.getValue();
    Result<CDSAnalytic> analyticResult = _legacyCdsConverterFn.toCdsAnalytic(env, cds, creditCurve);
    
    if (analyticResult.isSuccess()) {
      return price(extractForLegacyCds(cds), 
                   analyticResult.getValue(), 
                   creditCurve);
    } else {
      return Result.failure(analyticResult);
    }
    
  }
  
  /**
   * Resolve market data for the security using the passed function.
   * 
   * @param env pricing environment
   * @param resolverFn resolver function
   * @param security security
   * @return a resolved credit curve
   */
  private <S> Result<IsdaCreditCurve> resolveMarketData(Environment env, 
                                                        CreditMarketDataResolverFn<S> resolverFn, 
                                                        S security) {
    Result<CreditCurveDataKey> mdKeyResult = resolverFn.resolve(env, security);
    
    if (!mdKeyResult.isSuccess()) {
      return Result.failure(mdKeyResult);
    }
    
    return _creditCurveFn.buildIsdaCompliantCreditCurve(env, mdKeyResult.getValue()); //source from env
    
  }
  
  @Override
  public Result<T> priceStandardCds(Environment env, StandardCDSSecurity cds) {
    
    Result<IsdaCreditCurve> marketDataResult = resolveMarketData(env, _standardCdsMarketDataResolverFn, cds);
    
    //not much we can do if we can't resolve/build market data
    if (!marketDataResult.isSuccess()) {
      return Result.failure(marketDataResult);
    }
    IsdaCreditCurve creditCurve = marketDataResult.getValue();
    
    Result<CDSAnalytic> analyticResult = _standardCdsConverterFn.toCdsAnalytic(env, cds, creditCurve);
    
    if (analyticResult.isSuccess()) {
      return price(extractForStandardCds(cds),  
                   analyticResult.getValue(), 
                   creditCurve);
    } else {
      return Result.failure(analyticResult);
    }
  }

  //TODO index cds
  
  /**
   * Produce the risk measure for this instance using the analytics
   * types passed.
   * 
   * @param cdsData cds data
   * @param cdsAnalytic the cds analytic constructed
   * @param curve the resolved credit curve
   * @return a result of the appropriate type
   */
  protected abstract Result<T> price(CdsData cdsData, 
                                      CDSAnalytic cdsAnalytic, 
                                      IsdaCreditCurve curve);
  
  
  /**
   * Extracts relevant fields from standard cds security to CdsData.
   * 
   * @param cds the standard cds
   * @return a CdsData instance
   */
  private CdsData extractForStandardCds(StandardCDSSecurity cds) {
    
    return CdsData.builder()
                    .coupon(cds.getCoupon())
                    .interestRateNotional(cds.getNotional())
                    .build();
    
  }
  
  /**
   * Extracts relevant fields from legacy cds security to CdsData.
   * 
   * @param cds the legacy cds
   * @return a CdsData instance
   */
  private CdsData extractForLegacyCds(LegacyCDSSecurity cds) {
    return CdsData.builder()
                    .coupon(cds.getCoupon())
                    .interestRateNotional(cds.getNotional())
                    .build();
  }
  
}
