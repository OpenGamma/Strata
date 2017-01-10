/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DepositIsdaCreditCurveNode;
import com.opengamma.strata.market.curve.IsdaCreditCurveDefinition;
import com.opengamma.strata.market.curve.IsdaCreditCurveNode;
import com.opengamma.strata.market.curve.SwapIsdaCreditCurveNode;
import com.opengamma.strata.market.curve.node.CdsIsdaCreditCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.PaymentOnDefault;
import com.opengamma.strata.product.credit.ProtectionStartOfDay;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.type.CdsConvention;
import com.opengamma.strata.product.credit.type.CdsTemplate;
import com.opengamma.strata.product.credit.type.DatesCdsTemplate;
import com.opengamma.strata.product.credit.type.ImmutableCdsConvention;

/**
 * Base class for testing credit curve calibrators.
 */
public class IsdaCompliantCreditCurveCalibratorBase {

  protected static final ReferenceData REF_DATA = ReferenceData.standard();
  protected static final HolidayCalendarId DEFAULT_CALENDAR = HolidayCalendarIds.SAT_SUN;
  protected static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final ResolvedCdsTrade[][] EXP_NODE_CDS;
  private static final CdsIsdaCreditCurveNode[][] NODE_CDS;
  private static final ImmutableMarketData[] CDS_MARKET_DATA;
  protected static final ImmutableCreditRatesProvider[] YIELD_CURVES;
  private static final double[][] SPREADS;
  protected static final BusinessDayAdjustment BUS_ADJ = BusinessDayAdjustment.of(FOLLOWING, DEFAULT_CALENDAR);
  private static final DaysAdjustment ADJ_3D = DaysAdjustment.ofBusinessDays(3, DEFAULT_CALENDAR);
  protected static final DaysAdjustment CDS_SETTLE_STD = DaysAdjustment.ofBusinessDays(3, DEFAULT_CALENDAR);

  // discount curve data
  private static final double[] RATES = new double[] {
      0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931,
      0.03017, 0.03092, 0.0316, 0.03231, 0.03367, 0.03419, 0.03411, 0.03412};
  private static final int[] MM_MONTHS = new int[] {1, 2, 3, 6, 9, 12};
  private static final int[] SWAP_YEARS = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30};
  private static final int NUM_MM = MM_MONTHS.length;
  private static final int NUM_SWAP = SWAP_YEARS.length;
  private static final int NUM_INSTRUMENTS = NUM_MM + NUM_SWAP;
  private static final String[] ID_VALUES = new String[] {
      "mm1M", "mm2M", "mm3M", "mm6M", "mm9M", "mm12M", "swap2Y", "swap3Y", "swap4Y", "swap5Y", "swap6Y", "swap7Y",
      "swap8Y", "swap9Y", "swap10Y", "swap11Y", "swap12Y", "swap15Y", "swap20Y", "swap25Y", "swap30Y"};
  private static final ImmutableList<IsdaCreditCurveNode> DSC_NODES;
  private static final int NUM_TESTS = 5;

  static {
    Builder<IsdaCreditCurveNode> dscNodeBuilder = ImmutableList.builder();
    for (int i = 0; i < NUM_MM; i++) {
      Tenor tenor = Tenor.ofMonths(MM_MONTHS[i]);
      dscNodeBuilder.add(DepositIsdaCreditCurveNode.of(
          QuoteId.of(StandardId.of("OG", ID_VALUES[i])), ADJ_3D, BUS_ADJ, tenor, ACT_360));
    }
    for (int i = NUM_MM; i < NUM_INSTRUMENTS; i++) {
      Tenor tenor = Tenor.ofYears(SWAP_YEARS[i - NUM_MM]);
      dscNodeBuilder.add(SwapIsdaCreditCurveNode.of(
          QuoteId.of(StandardId.of("OG", ID_VALUES[i])), ADJ_3D, BUS_ADJ, tenor, THIRTY_U_360, Frequency.P12M));
    }
    DSC_NODES = dscNodeBuilder.build();
    EXP_NODE_CDS = new ResolvedCdsTrade[NUM_TESTS][];
    NODE_CDS = new CdsIsdaCreditCurveNode[NUM_TESTS][];
    CDS_MARKET_DATA = new ImmutableMarketData[NUM_TESTS];
    SPREADS = new double[NUM_TESTS][];
    YIELD_CURVES = new ImmutableCreditRatesProvider[NUM_TESTS];
    // case0
    LocalDate tradeDate0 = LocalDate.of(2011, 6, 19);
    LocalDate startDate0 = LocalDate.of(2011, 3, 21);
    YIELD_CURVES[0] = createRatesProvider(tradeDate0, tradeDate0, 1d, 0.4);
    Period[] tenors = new Period[] {
        Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5), Period.ofYears(7), Period.ofYears(10)};
    int nTenors = tenors.length;
    EXP_NODE_CDS[0] = new ResolvedCdsTrade[nTenors];
    NODE_CDS[0] = new CdsIsdaCreditCurveNode[nTenors];
    ImmutableMarketDataBuilder builderCredit0 = ImmutableMarketData.builder(tradeDate0);
    SPREADS[0] = new double[] {
        0.00886315689995649, 0.00886315689995649, 0.0133044689825873, 0.0171490070952563, 0.0183903639181293, 0.0194721890639724};
    for (int i = 0; i < nTenors; ++i) {
      Cds product = Cds.of(
          BUY,
          LEGAL_ENTITY,
          EUR,
          1d,
          startDate0,
          LocalDate.of(2011, 6, 20).plus(tenors[i]),
          Frequency.P3M,
          DEFAULT_CALENDAR,
          SPREADS[0][i]);
      EXP_NODE_CDS[0][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate0, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
      CdsConvention conv = ImmutableCdsConvention.of("conv", EUR, ACT_360, Frequency.P3M, BUS_ADJ, CDS_SETTLE_STD);
      CdsTemplate temp = DatesCdsTemplate.of(startDate0, LocalDate.of(2011, 6, 20).plus(tenors[i]), conv);
      QuoteId id = QuoteId.of(StandardId.of("OG", tenors[i].toString()));
      NODE_CDS[0][i] = CdsIsdaCreditCurveNode.ofParSpread(temp, id, LEGAL_ENTITY);
      builderCredit0.addValue(id, SPREADS[0][i]);
    }
    CDS_MARKET_DATA[0] = builderCredit0.build();
    // case1
    LocalDate tradeDate1 = LocalDate.of(2011, 3, 21);
    LocalDate snapDate1 = LocalDate.of(2011, 3, 18);
    LocalDate effDate1 = LocalDate.of(2011, 3, 20); //note this is a Sunday - for a standard CDS this would roll to the Monday.
    YIELD_CURVES[1] = createRatesProvider(tradeDate1, snapDate1, 1d, 0.4);
    tenors = new Period[] {
        Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5), Period.ofYears(7), Period.ofYears(10)};
    nTenors = tenors.length;
    NODE_CDS[1] = new CdsIsdaCreditCurveNode[nTenors];
    ImmutableMarketDataBuilder builderCredit1 = ImmutableMarketData.builder(tradeDate1);
    EXP_NODE_CDS[1] = new ResolvedCdsTrade[nTenors];
    SPREADS[1] = new double[] {0.027, 0.018, 0.012, 0.009, 0.007, 0.006};
    for (int i = 0; i < nTenors; ++i) {
      Cds product = Cds.of(
          BUY, LEGAL_ENTITY, EUR, 1d, effDate1, LocalDate.of(2011, 6, 20).plus(tenors[i]), P3M, DEFAULT_CALENDAR, SPREADS[1][i]);
      EXP_NODE_CDS[1][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate1, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
      CdsConvention conv = ImmutableCdsConvention.builder()
          .name("conv")
          .currency(EUR)
          .dayCount(ACT_360)
          .paymentFrequency(P3M)
          .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
          .businessDayAdjustment(BUS_ADJ)
          .settlementDateOffset(CDS_SETTLE_STD)
          .build();
      CdsTemplate temp = DatesCdsTemplate.of(effDate1, LocalDate.of(2011, 6, 20).plus(tenors[i]), conv);
      QuoteId id = QuoteId.of(StandardId.of("OG", tenors[i].toString()));
      NODE_CDS[1][i] = CdsIsdaCreditCurveNode.ofParSpread(temp, id, LEGAL_ENTITY);
      builderCredit1.addValue(id, SPREADS[1][i]);
    }
    CDS_MARKET_DATA[1] = builderCredit1.build();
    // case2
    LocalDate tradeDate2 = LocalDate.of(2011, 5, 30);
    LocalDate snapDate2 = LocalDate.of(2011, 5, 29);
    YIELD_CURVES[2] = createRatesProvider(tradeDate2, snapDate2, 1d, 0.25);
    LocalDate[] matDates2 = new LocalDate[] {
        LocalDate.of(2011, 6, 20), LocalDate.of(2012, 5, 30), LocalDate.of(2014, 6, 20), LocalDate.of(2016, 6, 20),
        LocalDate.of(2018, 6, 20)};
    int nMatDates2 = matDates2.length;
    NODE_CDS[2] = new CdsIsdaCreditCurveNode[nMatDates2];
    ImmutableMarketDataBuilder builderCredit2 = ImmutableMarketData.builder(tradeDate2);
    EXP_NODE_CDS[2] = new ResolvedCdsTrade[nMatDates2];
    SPREADS[2] = new double[] {0.05, 0.05, 0.05, 0.05, 0.05};
    for (int i = 0; i < nMatDates2; ++i) {
      Cds product = Cds.of(BUY, LEGAL_ENTITY, EUR, 1d, tradeDate2.plusDays(1), matDates2[i], P3M, DEFAULT_CALENDAR, SPREADS[2][i])
          .toBuilder().dayCount(THIRTY_U_360).build();
      EXP_NODE_CDS[2][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate2, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
      CdsConvention conv = ImmutableCdsConvention.builder()
          .name("conv")
          .currency(EUR)
          .dayCount(THIRTY_U_360)
          .paymentFrequency(P3M)
          .rollConvention(RollConventions.NONE)
          .businessDayAdjustment(BUS_ADJ)
          .settlementDateOffset(CDS_SETTLE_STD)
          .build();
      CdsTemplate temp = DatesCdsTemplate.of(tradeDate2.plusDays(1), matDates2[i], conv);
      QuoteId id = QuoteId.of(StandardId.of("OG", matDates2[i].toString()));
      NODE_CDS[2][i] = CdsIsdaCreditCurveNode.ofParSpread(temp, id, LEGAL_ENTITY);
      builderCredit2.addValue(id, SPREADS[2][i]);
    }
    CDS_MARKET_DATA[2] = builderCredit2.build();
    // case3
    LocalDate tradeDate3 = LocalDate.of(2011, 5, 30);
    LocalDate snapDate3 = LocalDate.of(2011, 5, 29);
    LocalDate effDate3 = LocalDate.of(2011, 7, 31);
    YIELD_CURVES[3] = createRatesProvider(tradeDate3, snapDate3, 1d, 0.25);
    LocalDate[] matDates3 = new LocalDate[] {
        LocalDate.of(2011, 11, 30), LocalDate.of(2012, 5, 30), LocalDate.of(2014, 5, 30), LocalDate.of(2016, 5, 30),
        LocalDate.of(2018, 5, 30), LocalDate.of(2021, 5, 30)};
    int nMatDates3 = matDates3.length;
    NODE_CDS[3] = new CdsIsdaCreditCurveNode[nMatDates3];
    ImmutableMarketDataBuilder builderCredit3 = ImmutableMarketData.builder(tradeDate3);
    EXP_NODE_CDS[3] = new ResolvedCdsTrade[nMatDates3];
    SPREADS[3] = new double[] {0.07, 0.06, 0.05, 0.055, 0.06, 0.065};
    for (int i = 0; i < nMatDates3; ++i) {
      Cds product = Cds.builder()
          .buySell(BUY)
          .legalEntityId(LEGAL_ENTITY)
          .currency(EUR)
          .dayCount(ACT_365F)
          .fixedRate(SPREADS[3][i])
          .notional(1d)
          .paymentSchedule(
              PeriodicSchedule.builder()
                  .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, DEFAULT_CALENDAR))
                  .startDate(effDate3)
                  .endDate(matDates3[i])
                  .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                  .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                  .frequency(Frequency.P6M)
                  .rollConvention(RollConventions.NONE)
                  .stubConvention(StubConvention.LONG_INITIAL)
                  .build())
          .paymentOnDefault(PaymentOnDefault.ACCRUED_PREMIUM)
          .protectionStart(ProtectionStartOfDay.BEGINNING)
          .stepinDateOffset(DaysAdjustment.ofCalendarDays(1))
          .settlementDateOffset(CDS_SETTLE_STD)
          .build();
      EXP_NODE_CDS[3][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate3, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
      CdsConvention conv = ImmutableCdsConvention.builder()
          .name("conv")
          .currency(EUR)
          .dayCount(ACT_365F)
          .paymentFrequency(Frequency.P6M)
          .rollConvention(RollConventions.NONE)
          .stubConvention(StubConvention.LONG_INITIAL)
          .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
          .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, DEFAULT_CALENDAR))
          .settlementDateOffset(CDS_SETTLE_STD)
          .build();
      CdsTemplate temp = DatesCdsTemplate.of(effDate3, matDates3[i], conv);
      QuoteId id = QuoteId.of(StandardId.of("OG", matDates3[i].toString()));
      NODE_CDS[3][i] = CdsIsdaCreditCurveNode.ofParSpread(temp, id, LEGAL_ENTITY);
      builderCredit3.addValue(id, SPREADS[3][i]);
    }
    CDS_MARKET_DATA[3] = builderCredit3.build();
    // case4: designed to trip the low rates/low spreads branch
    LocalDate tradeDate4 = LocalDate.of(2014, 1, 14);
    LocalDate snapDate4 = LocalDate.of(2014, 1, 13);
    YIELD_CURVES[4] = createRatesProvider(tradeDate4, snapDate4, 1d / 1000d, 0.4);
    int nSpreads4 = 6;
    NODE_CDS[4] = new CdsIsdaCreditCurveNode[nSpreads4];
    ImmutableMarketDataBuilder builderCredit4 = ImmutableMarketData.builder(tradeDate4);
    SPREADS[4] = new double[nSpreads4];
    Arrays.fill(SPREADS[4], 1.0e-4);
    EXP_NODE_CDS[4] = new ResolvedCdsTrade[nSpreads4];
    for (int i = 0; i < nSpreads4; ++i) {
      Cds product = Cds.of(BUY, LEGAL_ENTITY, EUR, 1d, LocalDate.of(2013, 12, 20), LocalDate.of(2014, 3, 20).plus(tenors[i]),
          P3M, DEFAULT_CALENDAR, SPREADS[4][i]);
      EXP_NODE_CDS[4][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate4, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
      CdsConvention conv = ImmutableCdsConvention.of("conv", EUR, ACT_360, P3M, BUS_ADJ, CDS_SETTLE_STD);
      CdsTemplate temp = DatesCdsTemplate.of(LocalDate.of(2013, 12, 20), LocalDate.of(2014, 3, 20).plus(tenors[i]), conv);
      QuoteId id = QuoteId.of(StandardId.of("OG", tenors[i].toString()));
      NODE_CDS[4][i] = CdsIsdaCreditCurveNode.ofParSpread(temp, id, LEGAL_ENTITY);
      builderCredit4.addValue(id, SPREADS[4][i]);
    }
    CDS_MARKET_DATA[4] = builderCredit4.build();
  }

  //-------------------------------------------------------------------------
  private static ImmutableCreditRatesProvider createRatesProvider(
      LocalDate tradeDate,
      LocalDate snapDate,
      double rateScale,
      double recoveryRate) {

    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(snapDate);
    for (int j = 0; j < NUM_INSTRUMENTS; j++) {
      builder.addValue(QuoteId.of(StandardId.of("OG", ID_VALUES[j])), RATES[j] * rateScale);
    }
    ImmutableMarketData quotes = builder.build();
    IsdaCreditCurveDefinition curveDefinition = IsdaCreditCurveDefinition.of(
        CurveName.of("yield"), EUR, tradeDate, ACT_365F, DSC_NODES, false, false);
    IsdaCreditDiscountFactors yc =
        IsdaCompliantDiscountCurveCalibrator.standard().calibrate(curveDefinition, quotes, REF_DATA);
    return ImmutableCreditRatesProvider.builder()
        .valuationDate(tradeDate)
        .discountCurves(ImmutableMap.of(EUR, yc))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, tradeDate, recoveryRate)))
        .creditCurves(ImmutableMap.of())
        .build();
  }

  // observations
  private static final double[] OBS_TIMES =
      new double[] {30 / 365., 90 / 365., 180. / 365., 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
  private static final double[][] EXP_PROB_ISDA = new double[][] {
      {0.998772746815168, 0.996322757048216, 0.992659036212158, 0.985174753005029, 0.959541054444166, 0.9345154655283,
          0.897874320219939, 0.862605325653124, 0.830790530716993, 0.80016566917562, 0.76968842828467, 0.740364207356242,
          0.71215720464425, 0.685024855452902, 0.658926216751093},
      {0.996266535762958, 0.988841371514657, 0.977807258018988, 0.96475844963628, 0.953395823781617, 0.940590393592274,
          0.933146036536171, 0.927501935763199, 0.924978347338877, 0.923516383873675, 0.919646843289677, 0.914974439245307,
          0.91032577405212, 0.905700727101315, 0.901099178396858},
      {0.994488823839325, 0.983690222697363, 0.967711777571664, 0.935677618299157, 0.875533583554252, 0.819255475760025,
          0.7666904278069, 0.717503794565525, 0.671435362513808, 0.628322474825315, 0.587977867136078, 0.550223788092265,
          0.514893899760578, 0.481832544772127, 0.450894060523028},
      {0.99238650617037, 0.977332973057625, 0.955179740225657, 0.92187587198518, 0.868032006457467, 0.817353939709416,
          0.751100020583073, 0.690170357851426, 0.622562049244094, 0.561519352597547, 0.500515112466997, 0.44610942528539,
          0.397617603088025, 0.354396812361283, 0.315874095202052},
      {0.999986111241871, 0.999958334304303, 0.999916670344636, 0.999831033196934, 0.999662094963152, 0.999493185285761,
          0.999324304350342, 0.999155451994703, 0.998986628218491, 0.998817832978659, 0.998649066279251, 0.998480328100177,
          0.998311618432194, 0.998142937270482, 0.997974284610226}};
  private static final double[][] EXP_PROB_MARKIT_FIX = new double[][] {
      {0.998773616100865, 0.996325358510497, 0.992664220011069, 0.985181033285486, 0.959551128356433, 0.934529141029508,
          0.897893062747179, 0.862628725130658, 0.830817532293803, 0.800195970143901, 0.76972190245315, 0.740400570243092,
          0.712196187570045, 0.685066206017066, 0.658969697981512},
      {0.996272873932676, 0.988860244428938, 0.977844583012059, 0.964805380714707, 0.953429040991605, 0.940617833909825,
          0.933169293548597, 0.927521552929219, 0.924995002753253, 0.923530307620416, 0.91965942070523, 0.914986149602945,
          0.910336625838319, 0.905710728738695, 0.901108338244616},
      {0.994492541382389, 0.983716053360113, 0.967769880036333, 0.935798775210736, 0.875741081454824, 0.819537657320969,
          0.766996460740263, 0.717827034617184, 0.671770999435863, 0.628667500941574, 0.588329694303598, 0.550580121735183,
          0.515252711846802, 0.482192049049632, 0.451252689837008},
      {0.992434753056402, 0.977475525071675, 0.955458402114146, 0.923257693140384, 0.86924227242564, 0.818402338488625,
          0.752150342806546, 0.691215773405857, 0.623608833084194, 0.562557270491733, 0.50153493334764, 0.447102836461508,
          0.3985783104631, 0.355320200669978, 0.316756937570093},
      {0.999986111408132, 0.999958334803071, 0.999916671342131, 0.999831035053453, 0.999662097437689, 0.999493188187127,
          0.999324307673036, 0.9991554557374, 0.998986632383511, 0.998817837566403, 0.998649071291, 0.998480333536109,
          0.998311624292164, 0.998142943554348, 0.997974291317845},};
  private static final int N_OBS = OBS_TIMES.length;

  //-------------------------------------------------------------------------
  protected void testCalibrationAgainstISDA(
      IsdaCompliantCreditCurveCalibrator builder,
      DayCount dayCount,
      Currency currency,
      double tol) {

    IsdaCdsProductPricer pricer = new IsdaCdsProductPricer(builder.getAccrualOnDefaultFormula());
    for (int i = 0; i < NUM_TESTS; i++) {
      LegalEntitySurvivalProbabilities creditCurve = builder.calibrate(
          ImmutableList.copyOf(NODE_CDS[i]),
          CurveName.of("credit"),
          CDS_MARKET_DATA[i],
          YIELD_CURVES[i],
          dayCount,
          currency,
          false, false, REF_DATA);
      ResolvedCdsTrade[] expectedCds = EXP_NODE_CDS[i];
      ImmutableCreditRatesProvider provider = YIELD_CURVES[i].toBuilder()
          .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, EUR), creditCurve))
          .build();
      double[] expected =
          builder.getAccrualOnDefaultFormula() == AccrualOnDefaultFormula.MARKIT_FIX ? EXP_PROB_MARKIT_FIX[i] : EXP_PROB_ISDA[i];
      for (int k = 0; k < N_OBS; k++) {
        assertEquals(creditCurve.getSurvivalProbabilities().discountFactor(OBS_TIMES[k]), expected[k], tol);
      }
      int m = expectedCds.length;
      for (int j = 0; j < m; j++) {
        ResolvedCdsTrade cdsFromNode = NODE_CDS[i][j]
            .trade(1d, CDS_MARKET_DATA[i], REF_DATA)
            .getUnderlyingTrade()
            .resolve(REF_DATA);
        assertEquals(cdsFromNode.getProduct(), expectedCds[j].getProduct());
        double price1 = pricer.price(
            cdsFromNode.getProduct(),
            provider,
            SPREADS[i][j],
            cdsFromNode.getInfo().getSettlementDate().get(),
            PriceType.CLEAN,
            REF_DATA);
        assertEquals(price1, 0.0, 5e-16);
      }
    }
  }

  protected void testJacobian(
      IsdaCompliantCreditCurveCalibrator builder,
      LegalEntitySurvivalProbabilities curve,
      ImmutableCreditRatesProvider ratesProvider,
      List<CdsIsdaCreditCurveNode> nodes,
      double[] quotes,
      double quoteScale,
      double eps) {

    LocalDate valuationDate = curve.getValuationDate();
    int nNode = nodes.size();
    IsdaCreditDiscountFactors df = (IsdaCreditDiscountFactors) curve.getSurvivalProbabilities();
    CurveName name = df.getCurve().getName();
    int nCurveNode = df.getParameterCount();
    for (int i = 0; i < nCurveNode; ++i) {
      double[] quotesUp = Arrays.copyOf(quotes, nNode);
      double[] quotesDw = Arrays.copyOf(quotes, nNode);
      quotesUp[i] += eps / quoteScale;
      quotesDw[i] -= eps / quoteScale;
      ImmutableMarketDataBuilder builderCreditUp = ImmutableMarketData.builder(valuationDate);
      ImmutableMarketDataBuilder builderCreditDw = ImmutableMarketData.builder(valuationDate);
      for (int j = 0; j < nNode; ++j) {
        builderCreditUp.addValue(nodes.get(j).getObservableId(), quotesUp[j] * quoteScale);
        builderCreditDw.addValue(nodes.get(j).getObservableId(), quotesDw[j] * quoteScale);
      }
      ImmutableMarketData marketDataUp = builderCreditUp.build();
      ImmutableMarketData marketDataDw = builderCreditDw.build();
      IsdaCreditDiscountFactors ccUp = (IsdaCreditDiscountFactors) builder.calibrate(
          nodes, name, marketDataUp, ratesProvider, curve.getSurvivalProbabilities().getDayCount(), curve.getCurrency(),
          false, false, REF_DATA).getSurvivalProbabilities();
      IsdaCreditDiscountFactors ccDw = (IsdaCreditDiscountFactors) builder.calibrate(
          nodes, name, marketDataDw, ratesProvider, curve.getSurvivalProbabilities().getDayCount(), curve.getCurrency(),
          false, false, REF_DATA).getSurvivalProbabilities();
      for (int j = 0; j < nNode; ++j) {
        double computed = df.getCurve().getMetadata().findInfo(CurveInfoType.JACOBIAN).get().getJacobianMatrix().get(j, i);
        double expected = 0.5 * (ccUp.getCurve().getYValues().get(j) - ccDw.getCurve().getYValues().get(j)) / eps;
        assertEquals(computed, expected, eps * 10d);
      }
    }
  }

}
