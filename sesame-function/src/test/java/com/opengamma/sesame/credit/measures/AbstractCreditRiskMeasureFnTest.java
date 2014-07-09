package com.opengamma.sesame.credit.measures;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.security.credit.LegacyCDSSecurity;
import com.opengamma.financial.security.credit.StandardCDSSecurity;
import com.opengamma.financial.security.swap.InterestRateNotional;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.CdsData;
import com.opengamma.sesame.credit.IsdaCompliantCreditCurveFn;
import com.opengamma.sesame.credit.IsdaCreditCurve;
import com.opengamma.sesame.credit.converter.LegacyCdsConverterFn;
import com.opengamma.sesame.credit.converter.StandardCdsConverterFn;
import com.opengamma.sesame.credit.market.LegacyCdsMarketDataResolverFn;
import com.opengamma.sesame.credit.market.StandardCdsMarketDataResolverFn;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;

/**
 * Tests {@link AbstractCreditRiskMeasureFn} flow.
 */
public class AbstractCreditRiskMeasureFnTest {
  
  
  private AbstractCreditRiskMeasureFn<RiskResult> _fn;
  private Environment _env;
  private LegacyCDSSecurity _legCds;
  private StandardCDSSecurity _cds;
  
  //standard cds fields:
  private final InterestRateNotional _notional = new InterestRateNotional(Currency.USD, 1000000);
  private final double _coupon = 0.01;

  @SuppressWarnings("unchecked")
  @BeforeMethod
  public void beforeMethod() {
    _env = mock(Environment.class);
    _legCds = mock(LegacyCDSSecurity.class);
    _cds = mock(StandardCDSSecurity.class);
    
    when(_legCds.getNotional()).thenReturn(_notional);
    when(_legCds.getCoupon()).thenReturn(_coupon);
    when(_cds.getNotional()).thenReturn(_notional);
    when(_cds.getCoupon()).thenReturn(_coupon);
    
    LegacyCdsConverterFn legacyCdsConverter = mock(LegacyCdsConverterFn.class);
    StandardCdsConverterFn standardCdsConverter = mock(StandardCdsConverterFn.class);
    Result<CDSAnalytic> result = mock(Result.class);
    when(result.isSuccess()).thenReturn(true);
    when(legacyCdsConverter.toCdsAnalytic(any(Environment.class), any(LegacyCDSSecurity.class), any(IsdaCreditCurve.class))).thenReturn(result);
    when(standardCdsConverter.toCdsAnalytic(any(Environment.class), any(StandardCDSSecurity.class), any(IsdaCreditCurve.class))).thenReturn(result);
    
    final Result<CreditCurveDataKey> keyResult = mock(Result.class);
    when(keyResult.isSuccess()).thenReturn(true);
    //can't use mockito for these calls due to a limitation resolving calls via
    //interfaces with generic params (i.e. CreditMarketDataResolverFn)
    class StandardCdsMdResolverMock implements StandardCdsMarketDataResolverFn {
      
      @Override
      public Result<CreditCurveDataKey> resolve(Environment env, StandardCDSSecurity security) {
        return keyResult;
      }
    };
    
    class LegacyCdsMdResolverMock implements LegacyCdsMarketDataResolverFn {
      
      @Override
      public Result<CreditCurveDataKey> resolve(Environment env, LegacyCDSSecurity security) {
        return keyResult;
      }
    };
    
    IsdaCompliantCreditCurveFn curveFn = mock(IsdaCompliantCreditCurveFn.class);
    
    Result<IsdaCreditCurve> isdaCurveResult = mock(Result.class);
    when(isdaCurveResult.isSuccess()).thenReturn(true);
    when(curveFn.buildIsdaCompliantCreditCurve(any(Environment.class), any(CreditCurveDataKey.class))).thenReturn(isdaCurveResult);
    
    _fn = new AbstractCreditRiskMeasureFn<RiskResult>(legacyCdsConverter, 
                                                      standardCdsConverter, 
                                                      new StandardCdsMdResolverMock(), 
                                                      new LegacyCdsMdResolverMock(),
                                                      curveFn) {

      @Override
      protected Result<RiskResult> price(CdsData cdsData, CDSAnalytic cdsAnalytic, IsdaCreditCurve curve) {
        RiskResult result = new RiskResult();
        result._cdsData = cdsData;
        result._cdsAnalytic = cdsAnalytic;
        result._curve = curve;
        return Result.success(result);
      }
    };
    
  }
  
  class RiskResult {
    CdsData _cdsData;
    CDSAnalytic _cdsAnalytic;
    IsdaCreditCurve _curve;
  }
  
  
  @Test
  public void priceLegacyCds() {
    Result<RiskResult> result = _fn.priceLegacyCds(_env, _legCds);
    assertTrue(result.isSuccess());
  }

  @Test
  public void priceStandardCds() {
    Result<RiskResult> result = _fn.priceStandardCds(_env, _cds);
    assertTrue(result.isSuccess());
  }
}
