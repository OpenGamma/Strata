package com.opengamma.sesame.credit.market;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.sesame.credit.config.CreditCurveDataKeyMap;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * Tests use cases of {@link DefaultCreditKeyMapperFn}.
 */
@Test(groups = TestGroup.UNIT)
public class DefaultCreditKeyMapperFnTest {
  
  private CreditCurveDataKey _source;
  private CreditCurveDataKey _target;
  private CreditCurveDataKey _missing;
  private DefaultCreditKeyMapperFn _fn;


  @BeforeMethod
  public void beforeMethod() {
    
    _source = CreditCurveDataKey.builder().curveName("source").currency(Currency.USD).build();
    _target = CreditCurveDataKey.builder().curveName("target").currency(Currency.USD).build();
    _missing = CreditCurveDataKey.builder().curveName("missing").currency(Currency.USD).build();
    
    Map<CreditCurveDataKey, CreditCurveDataKey> keyMap = ImmutableMap.of(_source, _target);
    
    CreditCurveDataKeyMap configKeyMap = CreditCurveDataKeyMap.builder()
                                                              .securityCurveMappings(keyMap)
                                                              .build();
    
    _fn = new DefaultCreditKeyMapperFn(configKeyMap);
  }
  
  @Test
  public void successfulMap() {
    Result<CreditCurveDataKey> result = _fn.getMapping(_source);
    
    assertTrue("Expected success", result.isSuccess());
    assertEquals("Expected target key", _target, result.getValue());
  }

  @Test
  public void noMapping() {
    Result<CreditCurveDataKey> result = _fn.getMapping(_missing);
    
    assertTrue("Expected success", result.isSuccess());
    assertEquals("Expected same key (i.e. missing)", _missing, result.getValue());
  }

}
