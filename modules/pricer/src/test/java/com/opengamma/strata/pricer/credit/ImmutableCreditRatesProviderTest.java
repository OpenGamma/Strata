/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.fx.FxForwardSensitivity;

/**
 * Test {@link ImmutableCreditRatesProvider}.
 */
public class ImmutableCreditRatesProviderTest {

  private static final LocalDate VALUATION = LocalDate.of(2015, 2, 11);

  private static final double RECOVERY_RATE_ABC = 0.25;
  private static final double RECOVERY_RATE_DEF = 0.35;
  private static final StandardId LEGAL_ENTITY_ABC = StandardId.of("OG", "ABC");
  private static final StandardId LEGAL_ENTITY_DEF = StandardId.of("OG", "DEF");
  private static final ConstantRecoveryRates RR_ABC = ConstantRecoveryRates.of(LEGAL_ENTITY_ABC, VALUATION, RECOVERY_RATE_ABC);
  private static final ConstantRecoveryRates RR_DEF = ConstantRecoveryRates.of(LEGAL_ENTITY_DEF, VALUATION, RECOVERY_RATE_DEF);

  // discount curves
  private static final DoubleArray TIME_DSC_USD = DoubleArray.ofUnsafe(new double[] {1.0, 2.0, 5.0, 10.0, 20.0, 30.0});
  private static final DoubleArray RATE_DSC_USD = DoubleArray.ofUnsafe(new double[] {0.015, 0.019, 0.016, 0.012, 0.01, 0.005});
  private static final CurveName NAME_DSC_USD = CurveName.of("yieldUsd");
  private static final IsdaCreditDiscountFactors DSC_USD =
      IsdaCreditDiscountFactors.of(USD, VALUATION, NAME_DSC_USD, TIME_DSC_USD, RATE_DSC_USD, ACT_365F);
  private static final DoubleArray TIME_DSC_JPY = DoubleArray.ofUnsafe(new double[] {1.0, 5.0, 10.0, 20.0});
  private static final DoubleArray RATE_DSC_JPY = DoubleArray.ofUnsafe(new double[] {0.01, 0.011, 0.007, 0.002});
  private static final CurveName NAME_DSC_JPY = CurveName.of("yieldJpy");
  private static final IsdaCreditDiscountFactors DSC_JPY =
      IsdaCreditDiscountFactors.of(JPY, VALUATION, NAME_DSC_JPY, TIME_DSC_JPY, RATE_DSC_JPY, ACT_365F);
  // credit curves
  private static final DoubleArray TIME_CRD_ABC_USD = DoubleArray.ofUnsafe(new double[] {1.0, 3.0, 5.0, 7.0, 10.0});
  private static final DoubleArray RATE_CRD_ABC_USD = DoubleArray.ofUnsafe(new double[] {0.005, 0.006, 0.004, 0.012, 0.01});
  private static final CurveName NAME_CRD_ABC_USD = CurveName.of("creditAbc_usd");
  private static final IsdaCreditDiscountFactors CRD_ABC_USD =
      IsdaCreditDiscountFactors.of(USD, VALUATION, NAME_CRD_ABC_USD, TIME_CRD_ABC_USD, RATE_CRD_ABC_USD, ACT_365F);
  private static final DoubleArray TIME_CRD_ABC_JPY = DoubleArray.ofUnsafe(new double[] {1.0, 3.0, 5.0, 7.0, 10.0});
  private static final DoubleArray RATE_CRD_ABC_JPY = DoubleArray.ofUnsafe(new double[] {0.005, 0.006, 0.004, 0.012, 0.01});
  private static final CurveName NAME_CRD_ABC_JPY = CurveName.of("creditAbc_jpy");
  private static final IsdaCreditDiscountFactors CRD_ABC_JPY =
      IsdaCreditDiscountFactors.of(JPY, VALUATION, NAME_CRD_ABC_JPY, TIME_CRD_ABC_JPY, RATE_CRD_ABC_JPY, ACT_365F);
  private static final DoubleArray TIME_CRD_DEF = DoubleArray.ofUnsafe(new double[] {3.0, 5.0, 10.0});
  private static final DoubleArray RATE_CRD_DEF = DoubleArray.ofUnsafe(new double[] {0.005, 0.006, 0.004});
  private static final CurveName NAME_CRD_DEF = CurveName.of("creditDef");
  private static final IsdaCreditDiscountFactors CRD_DEF =
      IsdaCreditDiscountFactors.of(JPY, VALUATION, NAME_CRD_DEF, TIME_CRD_DEF, RATE_CRD_DEF, ACT_365F);

  @Test
  public void test_getter() {
    ImmutableCreditRatesProvider test = ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION)
        .creditCurves(ImmutableMap.of(
            Pair.of(LEGAL_ENTITY_ABC, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_USD),
            Pair.of(LEGAL_ENTITY_ABC, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY),
            Pair.of(LEGAL_ENTITY_DEF, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_DEF, CRD_DEF)))
        .discountCurves(ImmutableMap.of(USD, DSC_USD, JPY, DSC_JPY))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY_ABC, RR_ABC, LEGAL_ENTITY_DEF, RR_DEF))
        .build();
    assertThat(test.discountFactors(USD)).isEqualTo(DSC_USD);
    assertThat(test.discountFactors(JPY)).isEqualTo(DSC_JPY);
    assertThat(test.survivalProbabilities(LEGAL_ENTITY_ABC, USD)).isEqualTo(LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_USD));
    assertThat(test.survivalProbabilities(LEGAL_ENTITY_ABC, JPY)).isEqualTo(LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY));
    assertThat(test.survivalProbabilities(LEGAL_ENTITY_DEF, JPY)).isEqualTo(LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_DEF, CRD_DEF));
    assertThat(test.recoveryRates(LEGAL_ENTITY_ABC)).isEqualTo(RR_ABC);
    assertThat(test.recoveryRates(LEGAL_ENTITY_DEF)).isEqualTo(RR_DEF);
    StandardId entity = StandardId.of("OG", "NONE");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.discountFactors(EUR));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.survivalProbabilities(LEGAL_ENTITY_DEF, USD));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.survivalProbabilities(entity, USD));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.recoveryRates(entity));
  }

  @Test
  public void test_valuationDateMismatch() {
    ConstantRecoveryRates rrWrong = ConstantRecoveryRates.of(LEGAL_ENTITY_ABC, VALUATION.plusWeeks(1), RECOVERY_RATE_ABC);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION)
        .creditCurves(ImmutableMap.of(
            Pair.of(LEGAL_ENTITY_ABC, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_USD),
            Pair.of(LEGAL_ENTITY_ABC, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY),
            Pair.of(LEGAL_ENTITY_DEF, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_DEF, CRD_DEF)))
        .discountCurves(ImmutableMap.of(USD, DSC_USD, JPY, DSC_JPY))
            .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY_ABC, rrWrong, LEGAL_ENTITY_DEF, RR_DEF))
        .build());
    IsdaCreditDiscountFactors crdWrong =
        IsdaCreditDiscountFactors.of(JPY, VALUATION.plusWeeks(1), NAME_CRD_DEF, TIME_CRD_DEF, RATE_CRD_DEF, ACT_365F);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION)
        .creditCurves(ImmutableMap.of(
            Pair.of(LEGAL_ENTITY_ABC, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_USD),
            Pair.of(LEGAL_ENTITY_ABC, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY),
                Pair.of(LEGAL_ENTITY_DEF, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_DEF, crdWrong)))
        .discountCurves(ImmutableMap.of(USD, DSC_USD, JPY, DSC_JPY))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY_ABC, RR_ABC, LEGAL_ENTITY_DEF, RR_DEF))
        .build());
    IsdaCreditDiscountFactors dscWrong =
        IsdaCreditDiscountFactors.of(USD, VALUATION.plusWeeks(1), NAME_DSC_USD, TIME_DSC_USD, RATE_DSC_USD, ACT_365F);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION)
        .creditCurves(ImmutableMap.of(
            Pair.of(LEGAL_ENTITY_ABC, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_USD),
            Pair.of(LEGAL_ENTITY_ABC, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY),
            Pair.of(LEGAL_ENTITY_DEF, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_DEF, CRD_DEF)))
            .discountCurves(ImmutableMap.of(USD, dscWrong, JPY, DSC_JPY))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY_ABC, RR_ABC, LEGAL_ENTITY_DEF, RR_DEF))
        .build());
  }

  @Test
  public void test_parameterSensitivity() {
    ZeroRateSensitivity zeroPt = ZeroRateSensitivity.of(USD, 10d, 5d);
    CreditCurveZeroRateSensitivity creditPt = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY_ABC, JPY, 2d, 3d);
    FxForwardSensitivity fxPt = FxForwardSensitivity.of(CurrencyPair.of(JPY, USD), USD, LocalDate.of(2017, 2, 14), 15d);
    CreditRatesProvider test = ImmutableCreditRatesProvider.builder()
        .creditCurves(ImmutableMap.of(
            Pair.of(LEGAL_ENTITY_ABC, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_USD),
            Pair.of(LEGAL_ENTITY_ABC, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY),
            Pair.of(LEGAL_ENTITY_DEF, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_DEF, CRD_DEF)))
        .discountCurves(ImmutableMap.of(USD, DSC_USD, JPY, DSC_JPY))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY_ABC, RR_ABC, LEGAL_ENTITY_DEF, RR_DEF))
        .valuationDate(VALUATION)
        .build();
    CurrencyParameterSensitivities computed =
        test.parameterSensitivity(zeroPt.combinedWith(creditPt).combinedWith(fxPt).build());
    CurrencyParameterSensitivities expected = DSC_USD.parameterSensitivity(zeroPt).combinedWith(
        LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY).parameterSensitivity(creditPt));
    assertThat(computed.equalWithTolerance(expected, 1.0e-14)).isTrue();
  }

  @Test
  public void test_singleCreditCurveParameterSensitivity() {
    ZeroRateSensitivity zeroPt = ZeroRateSensitivity.of(USD, 10d, 5d);
    CreditCurveZeroRateSensitivity creditPt = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY_ABC, JPY, 2d, 3d);
    FxForwardSensitivity fxPt = FxForwardSensitivity.of(CurrencyPair.of(JPY, USD), USD, LocalDate.of(2017, 2, 14), 15d);
    CreditRatesProvider test = ImmutableCreditRatesProvider.builder()
        .creditCurves(ImmutableMap.of(
            Pair.of(LEGAL_ENTITY_ABC, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_USD),
            Pair.of(LEGAL_ENTITY_ABC, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY),
            Pair.of(LEGAL_ENTITY_DEF, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_DEF, CRD_DEF)))
        .discountCurves(ImmutableMap.of(USD, DSC_USD, JPY, DSC_JPY))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY_ABC, RR_ABC, LEGAL_ENTITY_DEF, RR_DEF))
        .valuationDate(VALUATION)
        .build();
    CurrencyParameterSensitivities computed = CurrencyParameterSensitivities.of(test.singleCreditCurveParameterSensitivity(
        zeroPt.combinedWith(creditPt).combinedWith(fxPt).build(),
        LEGAL_ENTITY_ABC,
        JPY));
    CurrencyParameterSensitivities expected =
        LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY).parameterSensitivity(creditPt);
    assertThat(computed.equalWithTolerance(expected, 1.0e-14)).isTrue();
  }

  @Test
  public void test_singleDiscountCurveParameterSensitivity() {
    ZeroRateSensitivity zeroPt = ZeroRateSensitivity.of(USD, 10d, 5d);
    CreditCurveZeroRateSensitivity creditPt = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY_ABC, JPY, 2d, 3d);
    FxForwardSensitivity fxPt = FxForwardSensitivity.of(CurrencyPair.of(JPY, USD), USD, LocalDate.of(2017, 2, 14), 15d);
    CreditRatesProvider test = ImmutableCreditRatesProvider.builder()
        .creditCurves(ImmutableMap.of(
            Pair.of(LEGAL_ENTITY_ABC, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_USD),
            Pair.of(LEGAL_ENTITY_ABC, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_JPY),
            Pair.of(LEGAL_ENTITY_DEF, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_DEF, CRD_DEF)))
        .discountCurves(ImmutableMap.of(USD, DSC_USD, JPY, DSC_JPY))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY_ABC, RR_ABC, LEGAL_ENTITY_DEF, RR_DEF))
        .valuationDate(VALUATION)
        .build();
    CurrencyParameterSensitivities computed = CurrencyParameterSensitivities.of(
        test.singleDiscountCurveParameterSensitivity(zeroPt.combinedWith(creditPt).combinedWith(fxPt).build(), USD));
    CurrencyParameterSensitivities expected = DSC_USD.parameterSensitivity(zeroPt);
    assertThat(computed.equalWithTolerance(expected, 1.0e-14)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableCreditRatesProvider test1 = ImmutableCreditRatesProvider.builder()
        .creditCurves(
            ImmutableMap.of(Pair.of(LEGAL_ENTITY_ABC, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_ABC, CRD_ABC_USD)))
        .discountCurves(ImmutableMap.of(USD, DSC_USD))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY_ABC, RR_ABC))
        .valuationDate(VALUATION)
        .build();
    coverImmutableBean(test1);
    IsdaCreditDiscountFactors dsc =
        IsdaCreditDiscountFactors.of(JPY, VALUATION.plusDays(1), NAME_DSC_JPY, TIME_DSC_JPY, RATE_DSC_JPY, ACT_365F);
    IsdaCreditDiscountFactors hzd =
        IsdaCreditDiscountFactors.of(JPY, VALUATION.plusDays(1), NAME_CRD_DEF, TIME_CRD_DEF, RATE_CRD_DEF, ACT_365F);
    ConstantRecoveryRates rr = ConstantRecoveryRates.of(LEGAL_ENTITY_DEF, VALUATION.plusDays(1), RECOVERY_RATE_DEF);
    ImmutableCreditRatesProvider test2 =
        ImmutableCreditRatesProvider
            .builder()
            .creditCurves(
                ImmutableMap.of(Pair.of(LEGAL_ENTITY_DEF, JPY), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY_DEF, hzd)))
            .discountCurves(ImmutableMap.of(JPY, dsc))
            .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY_DEF, rr))
            .valuationDate(VALUATION.plusDays(1))
            .build();
    coverBeanEquals(test1, test2);
  }

}
