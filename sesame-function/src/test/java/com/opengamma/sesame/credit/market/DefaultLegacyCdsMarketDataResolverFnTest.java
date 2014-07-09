package com.opengamma.sesame.credit.market;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.analytics.financial.credit.DebtSeniority;
import com.opengamma.analytics.financial.credit.RestructuringClause;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.security.credit.LegacyCDSSecurity;
import com.opengamma.financial.security.swap.InterestRateNotional;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalScheme;
import com.opengamma.sesame.Environment;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * Tests credit curve data key inference for {@link LegacyCDSSecurity}.
 */
@Test(groups = TestGroup.UNIT)
public class DefaultLegacyCdsMarketDataResolverFnTest {

  private RestructuringClause _clause;
  private DebtSeniority _seniority;
  private ExternalId _refEntity;
  private Currency _ccy;
  
  private LegacyCDSSecurity _sec;
  private DefaultLegacyCdsMarketDataResolverFn _fn;
  
  private Environment _env;

  @BeforeMethod
  public void beforeMethod() {
    _clause = RestructuringClause.CR;
    _seniority = DebtSeniority.JRSUBUT2;
    _refEntity = ExternalId.of(ExternalScheme.of("refentity"), "test");
    _ccy = Currency.USD;
    _sec = mock(LegacyCDSSecurity.class);
    when(_sec.getNotional()).thenReturn(new InterestRateNotional(_ccy, 0));
    when(_sec.getReferenceEntity()).thenReturn(_refEntity);
    when(_sec.getDebtSeniority()).thenReturn(_seniority);
    when(_sec.getRestructuringClause()).thenReturn(_clause);
    
    _env = mock(Environment.class);
    
    _fn = new DefaultLegacyCdsMarketDataResolverFn();
  }
  
  @Test
  public void resolve() {
    Result<CreditCurveDataKey> result = _fn.resolve(_env, _sec);
    assertTrue("Expected success", result.isSuccess());
    
    CreditCurveDataKey key = result.getValue();
    assertEquals(_clause, key.getRestructuring());
    assertEquals(_seniority, key.getSeniority());
    assertEquals(_ccy, key.getCurrency());
    assertEquals(_refEntity.getValue(), key.getCurveName());
    
  }
}
