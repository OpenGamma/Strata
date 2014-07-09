package com.opengamma.sesame.credit.market;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.security.bond.BondSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.config.BondCreditCurveDataKeyMap;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;

/**
 * Tests {@link DefaultBondCreditMarketDataResolverFn}.
 */
public class DefaultBondCreditMarketDataResolverFnTest {
  
  private ExternalIdBundle _goodId;
  private ExternalIdBundle _badId;
  private BondCreditMarketDataResolverFn _fn;
  private final Environment _env = mock(Environment.class);
  private BondSecurity _goodSecurity;
  private BondSecurity _badSecurity;

  @BeforeMethod
  public void beforeMethod() {
    _goodId = ExternalId.of("Bond", "BondId").toBundle();
    _badId = ExternalId.of("Bond", "MissingBondId").toBundle();
    CreditCurveDataKey key = CreditCurveDataKey.builder()
                                                .currency(Currency.USD)
                                                .curveName("CurveName")
                                                .build();
    Map<ExternalIdBundle, CreditCurveDataKey> map = ImmutableMap.of(_goodId, key);
    BondCreditCurveDataKeyMap keyMap = BondCreditCurveDataKeyMap.builder().keyMap(map).build();
    _fn = new DefaultBondCreditMarketDataResolverFn(keyMap);
    
    _goodSecurity = mock(BondSecurity.class);
    when(_goodSecurity.getExternalIdBundle()).thenReturn(_goodId);
    
    _badSecurity = mock(BondSecurity.class);
    when(_badSecurity.getExternalIdBundle()).thenReturn(_badId);
    
  }
  
  @Test
  public void resolve() {
    Result<CreditCurveDataKey> result = _fn.resolve(_env, _goodSecurity);
    assertTrue(result.isSuccess());
  }
  
  @Test
  public void resolveMissing() {
    Result<CreditCurveDataKey> result = _fn.resolve(_env, _badSecurity);
    assertFalse(result.isSuccess());
    
  }
}
