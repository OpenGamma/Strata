package com.opengamma.strata.pricer.rate;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.LegalEntityGroup;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;

@Test
public class LegalEntityDiscountingProviderTest {

  @Test(enabled = false)
  public void test() {
    LocalDate valDate = LocalDate.of(2014, 2, 4);
    CurveMetadata meta = DefaultCurveMetadata.builder().curveName(CurveName.of("const curve"))
        .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.ZERO_RATE).dayCount(DayCounts.ACT_360).build();
    Curve curve = ConstantNodalCurve.of(meta, 1.56);
    ZeroRateDiscountFactors dsc = ZeroRateDiscountFactors.of(Currency.AUD, valDate, curve);
    LegalEntityGroup group = LegalEntityGroup.of("US prime");
    ImmutableMap<StandardId, LegalEntityGroup> legalEntityMap =
        ImmutableMap.<StandardId, LegalEntityGroup>of(StandardId.of("UsPrime", "usp"), group);
    ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors> issuerCurve =
        ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>builder()
            .put(Pair.of(group, Currency.USD), dsc).build();

    LegalEntityDiscountingProvider
        .builder()
        .issuerCurves(issuerCurve)
        .legalEntityMap(legalEntityMap)
        .build();
  }
}
