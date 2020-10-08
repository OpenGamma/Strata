/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.StandardSchemes.OG_SENSITIVITY_SCHEME;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.sensitivity.CurveSensitivitiesType.ZERO_RATE_DELTA;
import static com.opengamma.strata.market.sensitivity.CurveSensitivitiesType.ZERO_RATE_GAMMA;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.result.FailureItem;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.market.sensitivity.CurveSensitivities;
import com.opengamma.strata.market.sensitivity.CurveSensitivitiesType;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.PortfolioItemInfo;

/**
 * Test {@link SensitivityCsvLoader}.
 */
public final class SensitivityCsvLoaderTest {

  private static final CurveSensitivitiesType OTHER = CurveSensitivitiesType.of("Other");
  private static final AttributeType<String> CCP_ATTR = AttributeType.of("CCP");
  private static final SensitivityCsvInfoResolver RESOLVER_CCP = new SensitivityCsvInfoResolver() {
    @Override
    public boolean isInfoColumn(String headerLowerCase) {
      return "ccp".equals(headerLowerCase);
    }

    @Override
    public PortfolioItemInfo parseSensitivityInfo(CsvRow row, PortfolioItemInfo info) {
      return info.withAttribute(CCP_ATTR, row.getValue("CCP"));
    }

    @Override
    public ReferenceData getReferenceData() {
      return ReferenceData.standard();
    }
  };
  private static final SensitivityCsvInfoResolver RESOLVER_DATE = new SensitivityCsvInfoResolver() {
    @Override
    public boolean isTenorRequired() {
      return false;
    }

    @Override
    public Tenor checkSensitivityTenor(Tenor tenor) {
      Tenor adjustedTenor = SensitivityCsvInfoResolver.super.checkSensitivityTenor(tenor);
      return adjustedTenor.equals(Tenor.TENOR_12M) ? Tenor.TENOR_1Y : adjustedTenor;
    }

    @Override
    public ReferenceData getReferenceData() {
      return ReferenceData.standard();
    }
  };
  private static final SensitivityCsvLoader LOADER = SensitivityCsvLoader.standard();
  private static final SensitivityCsvLoader LOADER_CCP = SensitivityCsvLoader.of(RESOLVER_CCP);
  private static final SensitivityCsvLoader LOADER_DATE = SensitivityCsvLoader.of(RESOLVER_DATE);

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_standard() {
    CharSource source =
        ResourceLocator.ofClasspath("com/opengamma/strata/loader/csv/sensitivity-standard.csv").getCharSource();

    assertThat(LOADER.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(1);
    List<CurveSensitivities> list = test.getValue().get("");
    assertThat(list).hasSize(1);

    CurveSensitivities csens0 = list.get(0);
    assertThat(csens0.getTypedSensitivities()).hasSize(2);
    String tenors = "1D, 1W, 2W, 1M, 3M, 6M, 12M, 2Y, 5Y, 10Y";
    assertSens(csens0, ZERO_RATE_DELTA, "GBP", GBP, tenors, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    assertSens(csens0, ZERO_RATE_DELTA, "GBP-LIBOR", GBP, tenors, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    assertSens(csens0, ZERO_RATE_GAMMA, "GBP", GBP, tenors, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1);
    assertSens(csens0, ZERO_RATE_GAMMA, "GBP-LIBOR", GBP, tenors, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1);
  }

  @Test
  public void test_parse_standard_full() {
    CharSource source =
        ResourceLocator.ofClasspath("com/opengamma/strata/loader/csv/sensitivity-standard-full.csv").getCharSource();

    assertThat(LOADER_CCP.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER_CCP.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(2);

    List<CurveSensitivities> list0 = test.getValue().get("SCHEME~TR1");
    assertThat(list0).hasSize(1);
    CurveSensitivities csens0 = list0.get(0);
    assertThat(csens0.getInfo().getAttribute(CCP_ATTR)).isEqualTo("LCH");
    assertThat(csens0.getTypedSensitivities()).hasSize(1);
    assertSens(csens0, ZERO_RATE_DELTA, "GBCURVE", GBP, "1M, 3M, 6M", 1, 2, 3);

    List<CurveSensitivities> list1 = test.getValue().get("OG-Sensitivity~TR2");
    assertThat(list1).hasSize(1);
    CurveSensitivities csens1 = list1.get(0);
    assertThat(csens1.getInfo().getAttribute(CCP_ATTR)).isEqualTo("CME");
    assertThat(csens1.getTypedSensitivities()).hasSize(1);
    assertSens(csens1, ZERO_RATE_GAMMA, "GBCURVE", GBP, "1M, 3M, 6M", 4, 5, 6);
  }

  @Test
  public void test_parse_standard_dateInTenorColumn() {
    CharSource source = CharSource.wrap(
        "Reference,Sensitivity Type,Sensitivity Tenor,Value\n" +
            "GBP,ZeroRateGamma,2018-06-30,1\n");
    assertThat(LOADER_DATE.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER_DATE.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    assertThat(test.getValue().size()).isEqualTo(0);
    FailureItem failure0 = test.getFailures().get(0);
    assertThat(failure0.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure0.getMessage())
        .isEqualTo("CSV file could not be parsed at line 2: Invalid tenor '2018-06-30', must be expressed as nD, nW, nM or nY");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_list() {
    CharSource source =
        ResourceLocator.ofClasspath("com/opengamma/strata/loader/csv/sensitivity-list.csv").getCharSource();

    assertThat(LOADER.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(1);
    List<CurveSensitivities> list = test.getValue().get("");
    assertThat(list).hasSize(1);

    CurveSensitivities csens0 = list.get(0);
    assertThat(csens0.getTypedSensitivities()).hasSize(3);
    String tenors = "1D, 1W, 2W, 1M, 3M, 6M, 12M, 2Y, 5Y, 10Y";
    assertSens(csens0, ZERO_RATE_DELTA, "GBP", GBP, tenors, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    assertSens(csens0, ZERO_RATE_DELTA, "GBP-LIBOR", GBP, tenors, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    assertSens(csens0, ZERO_RATE_GAMMA, "GBP", GBP, tenors, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1);
    assertSens(csens0, ZERO_RATE_GAMMA, "GBP-LIBOR", GBP, tenors, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1);
    assertSens(csens0, OTHER, "GBP", GBP, tenors, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0);
    assertSens(csens0, OTHER, "GBP-LIBOR", GBP, tenors, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0);

    assertThat(LOADER.parseAndMerge(ImmutableList.of(source)).getValue()).isEqualTo(csens0);
  }

  @Test
  public void test_parse_list_full() {
    CharSource source =
        ResourceLocator.ofClasspath("com/opengamma/strata/loader/csv/sensitivity-list-full.csv").getCharSource();

    assertThat(LOADER_CCP.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER_CCP.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(2);

    List<CurveSensitivities> list0 = test.getValue().get("SCHEME~TR1");
    assertThat(list0).hasSize(1);
    CurveSensitivities csens0 = list0.get(0);
    assertThat(csens0.getInfo().getAttribute(CCP_ATTR)).isEqualTo("LCH");
    assertThat(csens0.getTypedSensitivities()).hasSize(1);
    assertSens(csens0, ZERO_RATE_DELTA, "GBCURVE", GBP, "1M, 3M, 6M", 1, 2, 3);

    List<CurveSensitivities> list1 = test.getValue().get("OG-Sensitivity~TR2");
    assertThat(list1).hasSize(1);
    CurveSensitivities csens1 = list1.get(0);
    assertThat(csens1.getInfo().getAttribute(CCP_ATTR)).isEqualTo("CME");
    assertThat(csens1.getTypedSensitivities()).hasSize(1);
    assertSens(csens1, ZERO_RATE_GAMMA, "GBCURVE", GBP, "1M, 3M, 6M", 4, 5, 6);
  }

  @Test
  public void test_parse_list_duplicateTenor() {
    CharSource source = CharSource.wrap(
        "Reference,Sensitivity Tenor,Zero Rate Delta\n" +
            "GBP,P1D,1\n" +
            "GBP,P1M,2\n" +
            "GBP,P1M,3\n");
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(1);
    List<CurveSensitivities> list = test.getValue().get("");
    assertThat(list).hasSize(1);

    CurveSensitivities csens0 = list.get(0);
    assertThat(csens0.getTypedSensitivities()).hasSize(1);
    assertSens(csens0, ZERO_RATE_DELTA, "GBP", GBP, "1D, 1M", 1, 5);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_list_allRowsBadNoEmptySensitvityCreated() {
    CharSource source = CharSource.wrap(
        "Reference,Sensitivity Tenor,ZeroRateDelta\n" +
            "GBP,XX,1\n");
    assertThat(LOADER_DATE.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER_DATE.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    assertThat(test.getValue().size()).isEqualTo(0);
    FailureItem failure0 = test.getFailures().get(0);
    assertThat(failure0.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure0.getMessage())
        .isEqualTo("CSV file could not be parsed at line 2: Invalid tenor 'XX', must be expressed as nD, nW, nM or nY");
  }

  @Test
  public void test_parse_list_unableToGetCurrency() {
    CharSource source = CharSource.wrap(
        "Reference,Sensitivity Tenor,Zero Rate Delta\n" +
            "X,1D,1.0");
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    FailureItem failure0 = test.getFailures().get(0);
    assertThat(failure0.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure0.getMessage())
        .isEqualTo("CSV file could not be parsed at line 2: Unable to parse currency from reference, " +
            "consider adding a 'Currency' column");
  }

  @Test
  public void test_parse_list_ioException() {
    CharSource source = new CharSource() {
      @Override
      public Reader openStream() throws IOException {
        throw new IOException("Oops");
      }
    };
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    assertThat(test.getFailures().get(0).getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(test.getFailures().get(0).getMessage().startsWith("CSV file could not be parsed: ")).isTrue();
    assertThat(test.getFailures().get(0).getMessage().contains("Oops")).isTrue();
  }

  @Test
  public void test_parse_list_badDayMonthTenor() {
    CharSource source = CharSource.wrap(
        "Reference,Sensitivity Tenor,Zero Rate Delta\n" +
            "GBP-LIBOR,P2M1D,1.0");
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    assertThat(test.getFailures().get(0).getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(test.getFailures().get(0).getMessage())
        .isEqualTo("CSV file could not be parsed at line 2: Invalid tenor, cannot mix years/months and days: 2M1D");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_grid() {
    CharSource source =
        ResourceLocator.ofClasspath("com/opengamma/strata/loader/csv/sensitivity-grid.csv").getCharSource();

    assertThat(LOADER.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(1);
    List<CurveSensitivities> list = test.getValue().get("");
    assertThat(list).hasSize(1);

    CurveSensitivities csens0 = list.get(0);
    assertThat(csens0.getTypedSensitivities()).hasSize(1);
    String tenors = "1D, 1W, 2W, 1M, 3M, 6M, 12M, 2Y, 5Y, 10Y";
    assertSens(csens0, ZERO_RATE_DELTA, "GBP", GBP, tenors, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    assertSens(csens0, ZERO_RATE_DELTA, "USD-LIBOR-3M", USD, tenors, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
  }

  @Test
  public void test_parse_grid_full() {
    CharSource source =
        ResourceLocator.ofClasspath("com/opengamma/strata/loader/csv/sensitivity-grid-full.csv").getCharSource();

    assertThat(LOADER_CCP.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER_CCP.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);

    List<CurveSensitivities> list0 = test.getValue().get("SCHEME~TR1");
    assertThat(list0).hasSize(1);
    CurveSensitivities csens0 = list0.get(0);
    assertThat(csens0.getId()).isEqualTo(Optional.of(StandardId.of("SCHEME", "TR1")));
    assertThat(csens0.getInfo().getAttribute(CCP_ATTR)).isEqualTo("LCH");
    assertThat(csens0.getTypedSensitivities()).hasSize(2);
    assertSens(csens0, ZERO_RATE_DELTA, "GBCURVE", GBP, "1M, 3M, 6M", 1, 2, 3);
    assertSens(csens0, ZERO_RATE_GAMMA, "GBCURVE", GBP, "1M, 3M, 6M", 4, 5, 6);

    List<CurveSensitivities> list1 = test.getValue().get("OG-Sensitivity~TR2");
    assertThat(list1).hasSize(1);
    CurveSensitivities csens1 = list1.get(0);
    assertThat(csens1.getId()).isEqualTo(Optional.of(StandardId.of(OG_SENSITIVITY_SCHEME, "TR2")));
    assertThat(csens1.getInfo().getAttribute(CCP_ATTR)).isEqualTo("CME");
    assertThat(csens1.getTypedSensitivities()).hasSize(1);
    assertSens(csens1, ZERO_RATE_DELTA, "GBCURVE", GBP, "1M, 3M, 6M", 7, 8, 9);
  }

  @Test
  public void test_parse_grid_duplicateTenor() {
    CharSource source = CharSource.wrap(
        "Sensitivity Type,Sensitivity Tenor,GBP\n" +
            "ZeroRateGamma,P6M,1\n" +
            "ZeroRateGamma,12M,2\n" +
            "ZeroRateGamma,12M,3\n");
    assertThat(LOADER_DATE.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER_DATE.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(1);
    List<CurveSensitivities> list = test.getValue().get("");
    assertThat(list).hasSize(1);

    CurveSensitivities csens0 = list.get(0);
    assertThat(csens0.getTypedSensitivities()).hasSize(1);
    assertSens(csens0, ZERO_RATE_GAMMA, "GBP", GBP, "6M, 1Y", 1, 5);  // 12M -> 1Y
  }

  @Test
  public void test_parse_grid_tenorAndDateColumns() {
    CharSource source = CharSource.wrap(
        "Sensitivity Type,Sensitivity Tenor,Sensitivity Date,GBP\n" +
            "ZeroRateGamma,1M,2018-06-30,1\n");
    assertThat(LOADER.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(1);
    List<CurveSensitivities> list = test.getValue().get("");
    assertThat(list).hasSize(1);

    CurveSensitivities csens0 = list.get(0);
    assertThat(csens0.getTypedSensitivities()).hasSize(1);
    CurrencyParameterSensitivities cpss = csens0.getTypedSensitivity(ZERO_RATE_GAMMA);
    assertThat(cpss.getSensitivities()).hasSize(1);
    CurrencyParameterSensitivity cps = cpss.getSensitivities().get(0);
    assertThat(cps.getParameterMetadata()).hasSize(1);
    assertThat(cps.getParameterMetadata().get(0)).isEqualTo(TenorDateParameterMetadata.of(date(2018, 6, 30), TENOR_1M));
  }

  @Test
  public void test_parse_grid_dateColumn() {
    CharSource source = CharSource.wrap(
        "Sensitivity Type,Sensitivity Date,GBP\n" +
            "ZeroRateGamma,2018-06-30,1\n");
    assertThat(LOADER_DATE.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER_DATE.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(1);
    List<CurveSensitivities> list = test.getValue().get("");
    assertThat(list).hasSize(1);

    CurveSensitivities csens0 = list.get(0);
    assertThat(csens0.getTypedSensitivities()).hasSize(1);
    CurrencyParameterSensitivities cpss = csens0.getTypedSensitivity(ZERO_RATE_GAMMA);
    assertThat(cpss.getSensitivities()).hasSize(1);
    CurrencyParameterSensitivity cps = cpss.getSensitivities().get(0);
    assertThat(cps.getParameterMetadata()).hasSize(1);
    assertThat(cps.getParameterMetadata().get(0)).isEqualTo(LabelDateParameterMetadata.of(date(2018, 6, 30), "2018-06-30"));
  }

  @Test
  public void test_parse_grid_dateInTenorColumn() {
    CharSource source = CharSource.wrap(
        "Sensitivity Type,Sensitivity Tenor,GBP\n" +
            "ZeroRateGamma,2018-06-30,1\n");
    assertThat(LOADER_DATE.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER_DATE.parse(ImmutableList.of(source));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().size()).isEqualTo(1);
    List<CurveSensitivities> list = test.getValue().get("");
    assertThat(list).hasSize(1);

    CurveSensitivities csens0 = list.get(0);
    assertThat(csens0.getTypedSensitivities()).hasSize(1);
    CurrencyParameterSensitivities cpss = csens0.getTypedSensitivity(ZERO_RATE_GAMMA);
    assertThat(cpss.getSensitivities()).hasSize(1);
    CurrencyParameterSensitivity cps = cpss.getSensitivities().get(0);
    assertThat(cps.getParameterMetadata()).hasSize(1);
    assertThat(cps.getParameterMetadata().get(0)).isEqualTo(LabelDateParameterMetadata.of(date(2018, 6, 30), "2018-06-30"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_grid_allRowsBadNoEmptySensitvityCreated() {
    CharSource source = CharSource.wrap(
        "Sensitivity Type,Sensitivity Tenor,GBP\n" +
            "ZeroRateGamma,XX,1\n");
    assertThat(LOADER.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    assertThat(test.getValue().size()).isEqualTo(0);
    FailureItem failure0 = test.getFailures().get(0);
    assertThat(failure0.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure0.getMessage())
        .isEqualTo("CSV file could not be parsed at line 2: Invalid tenor 'XX', must be expressed as nD, nW, nM or nY");
  }

  @Test
  public void test_parse_grid_badTenorWithValidDateColumn() {
    CharSource source = CharSource.wrap(
        "Sensitivity Type,Sensitivity Tenor,Sensitivity Date,GBP\n" +
            "ZeroRateGamma,XXX,2018-06-30,1\n");
    assertThat(LOADER.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    assertThat(test.getValue().size()).isEqualTo(0);
    FailureItem failure0 = test.getFailures().get(0);
    assertThat(failure0.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure0.getMessage())
        .isEqualTo("CSV file could not be parsed at line 2: Invalid tenor 'XXX', must be expressed as nD, nW, nM or nY");
  }

  @Test
  public void test_parse_grid_missingColumns() {
    CharSource source = CharSource.wrap(
        "GBP\n" +
            "1");
    assertThat(LOADER.isKnownFormat(source)).isFalse();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    assertThat(test.getValue().size()).isEqualTo(0);
    FailureItem failure0 = test.getFailures().get(0);
    assertThat(failure0.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure0.getMessage()).isEqualTo("CSV file could not be parsed as sensitivities, invalid format");
  }

  @Test
  public void test_parse_grid_neitherTenorNorDate() {
    CharSource source = CharSource.wrap(
        "Sensitivity Type,Sensitivity Tenor,Sensitivity Date,GBP\n" +
            "ZeroRateGamma,,,1\n");
    assertThat(LOADER_DATE.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER_DATE.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    assertThat(test.getValue().size()).isEqualTo(0);
    FailureItem failure0 = test.getFailures().get(0);
    assertThat(failure0.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure0.getMessage()).isEqualTo("CSV file could not be parsed at line 2: Unable to parse tenor or date, " +
        "check 'Sensitivity Tenor' and 'Sensitivity Date' columns");
  }

  @Test
  public void test_parse_grid_dateButTenorRequired() {
    CharSource source = CharSource.wrap(
        "Sensitivity Type,Sensitivity Tenor,Sensitivity Date,GBP\n" +
            "ZeroRateGamma,,2018-06-30,1\n");
    assertThat(LOADER.isKnownFormat(source)).isTrue();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source));
    assertThat(test.getFailures()).hasSize(1);
    assertThat(test.getValue().size()).isEqualTo(0);
    FailureItem failure0 = test.getFailures().get(0);
    assertThat(failure0.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure0.getMessage())
        .isEqualTo("CSV file could not be parsed at line 2: Missing value for 'Sensitivity Tenor' column");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_multipleSources() {
    CharSource source1 = CharSource.wrap(
        "Reference,Sensitivity Tenor,Zero Rate Delta\n" +
            "GBP-LIBOR,P1M,1.1\n" +
            "GBP-LIBOR,P2M,1.2\n");
    CharSource source2 = CharSource.wrap(
        "Reference,Sensitivity Tenor,Zero Rate Delta\n" +
            "GBP-LIBOR,P3M,1.3\n" +
            "GBP-LIBOR,P6M,1.4\n");
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> test = LOADER.parse(ImmutableList.of(source1, source2));
    assertThat(test.getFailures().size()).as(test.getFailures().toString()).isEqualTo(0);
    assertThat(test.getValue().keySet()).hasSize(1);
    List<CurveSensitivities> list = test.getValue().get("");
    assertThat(list).hasSize(2);

    CurveSensitivities csens0 = list.get(0);
    assertThat(csens0.getTypedSensitivities()).hasSize(1);
    assertSens(csens0, ZERO_RATE_DELTA, "GBP-LIBOR", GBP, "1M, 2M", 1.1, 1.2);

    CurveSensitivities csens1 = list.get(1);
    assertThat(csens1.getTypedSensitivities()).hasSize(1);
    assertSens(csens1, ZERO_RATE_DELTA, "GBP-LIBOR", GBP, "3M, 6M", 1.3, 1.4);
  }

  //-------------------------------------------------------------------------
  private void assertSens(
      CurveSensitivities sens,
      CurveSensitivitiesType type,
      String curveNameStr,
      Currency currency,
      String tenors,
      double... values) {

    CurveName curveName = CurveName.of(curveNameStr);
    CurrencyParameterSensitivity sensitivity = sens.getTypedSensitivity(type).getSensitivity(curveName, currency);
    assertThat(sensitivity.getMarketDataName()).isEqualTo(CurveName.of(curveNameStr));
    assertThat(sensitivity.getCurrency()).isEqualTo(currency);
    assertThat(metadataString(sensitivity.getParameterMetadata())).isEqualTo(tenors);
    assertThat(sensitivity.getSensitivity()).isEqualTo(DoubleArray.ofUnsafe(values));
  }

  private String metadataString(ImmutableList<ParameterMetadata> parameterMetadata) {
    return parameterMetadata.stream().map(md -> md.getLabel()).collect(joining(", "));
  }

}
