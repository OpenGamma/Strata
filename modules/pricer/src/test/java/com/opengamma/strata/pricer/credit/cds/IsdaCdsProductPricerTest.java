package com.opengamma.strata.pricer.credit.cds;

import static com.opengamma.strata.basics.currency.Currency.USD;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.cds.Cds;

@Test
public class IsdaCdsProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 1, 4);
  private static final LocalDate START_DATE = LocalDate.of(2014, 1, 5);
  private static final LocalDate END_DATE = LocalDate.of(2020, 12, 20);
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final DaysAdjustment DAY_ADJ = DaysAdjustment.ofBusinessDays(3, CALENDAR);
  private static final LocalDate REF_DATE = DAY_ADJ.adjust(VALUATION_DATE, REF_DATA);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");

  private static final Cds PRODUCT = Cds.of(
      START_DATE, END_DATE, Frequency.P3M, BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR),
      StubConvention.SHORT_INITIAL, USD, 1d, DayCounts.ACT_360, 0.05, true, true,
      DaysAdjustment.ofBusinessDays(3, CALENDAR), LEGAL_ENTITY, BuySell.BUY);

  private static final DoubleArray TIME_YC = DoubleArray.ofUnsafe(new double[] {0.09041095890410959, 0.16712328767123288,
      0.2547945205479452, 0.5041095890410959, 0.7534246575342466, 1.0054794520547945, 2.0054794520547947, 3.008219178082192,
      4.013698630136987, 5.010958904109589, 6.008219178082192, 7.010958904109589, 8.01095890410959, 9.01095890410959,
      10.016438356164384, 12.013698630136986, 15.021917808219179, 20.01917808219178, 30.024657534246575});
  private static final DoubleArray RATE_YC = DoubleArray.ofUnsafe(new double[] {-0.002078655697855299, -0.001686438401304855,
      -0.0013445486228483379, -4.237819925898475E-4, 2.5142499469348057E-5, 5.935063895780138E-4, -3.247081037469503E-4,
      6.147182786549223E-4, 0.0019060597240545122, 0.0033125742254568815, 0.0047766352312329455, 0.0062374324537341225,
      0.007639664176639106, 0.008971003650150983, 0.010167545380711455, 0.012196853322376243, 0.01441082634734099,
      0.016236611610989507, 0.01652439910865982});
  private static final IsdaCompliantZeroRateDiscountFactors YIELD_CRVE =
      IsdaCompliantZeroRateDiscountFactors.of(
          USD,
          VALUATION_DATE,
          InterpolatedNodalCurve.of(
              DefaultCurveMetadata.builder()
                  .xValueType(ValueType.YEAR_FRACTION)
                  .yValueType(ValueType.ZERO_RATE)
                  .curveName("yield")
                  .dayCount(DayCounts.ACT_365F)
                  .build(),
              TIME_YC,
              RATE_YC,
              CurveInterpolators.PRODUCT_LINEAR,
              CurveExtrapolators.FLAT,
              CurveExtrapolators.FLAT));

  private static final DoubleArray TIME_CC = DoubleArray.ofUnsafe(new double[] {1.2054794520547945, 1.7095890410958905,
      2.712328767123288, 3.712328767123288, 4.712328767123288, 5.712328767123288, 7.715068493150685, 10.717808219178082});
  private static final DoubleArray RATE_CC = DoubleArray.ofUnsafe(new double[] {0.009950757755865505, 0.011452393348242077,
      0.013437059827272778, 0.015946259262917734, 0.018726979366476607, 0.02193600460205013, 0.02583462020717916,
      0.02862842089201528});
  private static final LegalEntitySurvivalProbabilities CREDIT_CRVE = LegalEntitySurvivalProbabilities.builder()
      .valuationDate(VALUATION_DATE)
      .currency(USD)
      .legalEntityId(LEGAL_ENTITY)
      .survivalProbabilities(
          IsdaCompliantZeroRateDiscountFactors.of(
              USD,
              VALUATION_DATE,
              InterpolatedNodalCurve.of(
                  DefaultCurveMetadata.builder()
                      .xValueType(ValueType.YEAR_FRACTION)
                      .yValueType(ValueType.ZERO_RATE)
                      .curveName("credit")
                      .dayCount(DayCounts.ACT_365F)
                      .build(),
                  TIME_CC,
                  RATE_CC,
                  CurveInterpolators.PRODUCT_LINEAR,
                  CurveExtrapolators.FLAT,
                  CurveExtrapolators.FLAT)))
      .build();
  private static final ConstantRecoveryRates RECOVERY_RATES = ConstantRecoveryRates.of(
      LEGAL_ENTITY,
      VALUATION_DATE,
      ConstantCurve.of(
          DefaultCurveMetadata.builder()
              .xValueType(ValueType.YEAR_FRACTION)
              .yValueType(ValueType.RECOVERY_RATE)
              .curveName("recovery")
              .dayCount(DayCounts.ACT_365F)
              .build(),
          0.3)); // TODO
  private static final CreditRatesProvider RATES_PROVIDER = CreditRatesProvider.builder()
      .valuationDate(VALUATION_DATE)
      .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), CREDIT_CRVE))
      .discountCurves(ImmutableMap.of(USD, YIELD_CRVE))
      .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_RATES))
      .build();

  private static final IsdaCdsProductPricer PRICER = new IsdaCdsProductPricer();

  public void test(){
    double res = PRICER.protectionLeg(PRODUCT.resolve(REF_DATA), RATES_PROVIDER, REF_DATE);
    System.out.println(res); // 0.1176383576255324
  }

}
