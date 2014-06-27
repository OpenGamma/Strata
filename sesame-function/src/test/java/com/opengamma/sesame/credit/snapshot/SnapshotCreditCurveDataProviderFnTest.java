package com.opengamma.sesame.credit.snapshot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.DataNotFoundException;
import com.opengamma.core.link.SnapshotLink;
import com.opengamma.financial.analytics.isda.credit.CreditCurveData;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataSnapshot;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;

/**
 * Tests {@link SnapshotYieldCurveDataProviderFn}.
 */
public class SnapshotCreditCurveDataProviderFnTest {
  
  private SnapshotCreditCurveDataProviderFn _fnWithBadLink;
  private SnapshotCreditCurveDataProviderFn _fnWithUSDCurve;
  
  private final CreditCurveDataKey _goodKey = CreditCurveDataKey.builder().currency(Currency.USD).build();
  private final CreditCurveDataKey _badKey = CreditCurveDataKey.builder().currency(Currency.GBP).build();

  @SuppressWarnings("unchecked")
  @BeforeClass
  public void beforeClass() {
    
    SnapshotLink<CreditCurveDataSnapshot> badLink = mock(SnapshotLink.class);
    when(badLink.resolve()).thenThrow(new DataNotFoundException("test"));
    _fnWithBadLink = new SnapshotCreditCurveDataProviderFn(badLink);
    
    CreditCurveDataSnapshot snapshot = CreditCurveDataSnapshot.builder()
                      .name("")
                      .creditCurves(ImmutableMap.of(_goodKey, mock(CreditCurveData.class)))
                      .build();
    SnapshotLink<CreditCurveDataSnapshot> goodLink = SnapshotLink.resolved(snapshot);
    
    _fnWithUSDCurve = new SnapshotCreditCurveDataProviderFn(goodLink);
  }
  
  @Test(expectedExceptions = {DataNotFoundException.class})
  public void testFnWithBadLink() {
    
    Result<CreditCurveData> result = _fnWithBadLink.retrieveCreditCurveData(_goodKey);
    
    assertFalse("Link threw exception so function should fail.", result.isSuccess());
    
  }

  @Test
  public void testFnMissingData() {
    
    Result<CreditCurveData> result = _fnWithUSDCurve.retrieveCreditCurveData(_badKey);
    
    assertFalse(_badKey + " is missing so result should be failure.", result.isSuccess());
    
  }

  @Test
  public void testFn() {
    
    Result<CreditCurveData> result = _fnWithUSDCurve.retrieveCreditCurveData(_goodKey);
    
    assertTrue(_goodKey + " present in snapshot so should succeed.", result.isSuccess());
    
  }

}
