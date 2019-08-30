/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Tests {@link HistoricPriceIndexValues}.
 */
public class HistoricPriceIndexValuesTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2015, 5, 3);
  private static final YearMonth VAL_MONTH = YearMonth.of(2015, 5);

  // USD HICP, CPURNSA Index
  private static final double[] USCPI_VALUES = new double[] {
      211.143, 212.193, 212.709, 213.240, 213.856, 215.693, 215.351, 215.834, 215.969, 216.177, 216.330, 215.949, // 2009
      216.687, 216.741, 217.631, 218.009, 218.178, 217.965, 218.011, 218.312, 218.439, 218.711, 218.803, 219.179, // 2010
      220.223, 221.309, 223.467, 224.906, 225.964, 225.722, 225.922, 226.545, 226.889, 226.421, 226.230, 225.672, // 2011
      226.655, 227.663, 229.392, 230.085, 229.815, 229.478, 229.104, 230.379, 231.407, 231.317, 230.221, 229.601, // 2012
      230.280, 232.166, 232.773, 232.531, 232.945, 233.504, 233.596, 233.877, 234.149, 233.546, 233.069, 233.049, // 2013
      233.916, 234.781, 236.293, 237.072, 237.900, 238.343, 238.250, 237.852, 238.031, 237.433, 236.151, 234.812, // 2014
      233.707, 234.722, 236.119}; // 2015
  private static final LocalDate USCPI_START_DATE = LocalDate.of(2009, 1, 31);
  private static final LocalDateDoubleTimeSeries USCPI_TS;
  static {
    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < USCPI_VALUES.length; i++) {
      builder.put(USCPI_START_DATE.plusMonths(i), USCPI_VALUES[i]);
    }
    USCPI_TS = builder.build();
  }

  private static final YearMonth[] TEST_MONTHS = new YearMonth[] {
      YearMonth.of(2015, 1), YearMonth.of(2015, 2), YearMonth.of(2015, 5), YearMonth.of(2016, 5), YearMonth.of(2016, 6),
      YearMonth.of(2024, 12)};
  private static final PriceIndexObservation[] TEST_OBS = new PriceIndexObservation[TEST_MONTHS.length];
  static {
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      TEST_OBS[i] = PriceIndexObservation.of(US_CPI_U, TEST_MONTHS[i]);
    }
  }
  private static final double TOLERANCE_VALUE = 1.0E-10;

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    HistoricPriceIndexValues test = HistoricPriceIndexValues.of(US_CPI_U, VAL_DATE, USCPI_TS);
    assertThat(test.getIndex()).isEqualTo(US_CPI_U);
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.getFixings()).isEqualTo(USCPI_TS);
    assertThat(test.getParameterCount()).isEqualTo(0);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getParameter(0));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getParameterMetadata(0));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.withParameter(0, 1d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d)).isSameAs(test);
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_valuePointSensitivity_fixing() {
    HistoricPriceIndexValues test = HistoricPriceIndexValues.of(US_CPI_U, VAL_DATE, USCPI_TS);
    PriceIndexObservation obs = PriceIndexObservation.of(US_CPI_U, VAL_MONTH.minusMonths(3));
    assertThat(test.value(obs)).isCloseTo(234.722d, offset(TOLERANCE_VALUE));
    assertThat(test.valuePointSensitivity(obs)).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_valuePointSensitivity_forward() {
    YearMonth month = VAL_MONTH.plusMonths(3);
    HistoricPriceIndexValues test = HistoricPriceIndexValues.of(US_CPI_U, VAL_DATE, USCPI_TS);
    PriceIndexObservation obs = PriceIndexObservation.of(US_CPI_U, month);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.value(obs));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.valuePointSensitivity(obs));
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  @Test
  public void test_parameterSensitivity() {
    HistoricPriceIndexValues test = HistoricPriceIndexValues.of(US_CPI_U, VAL_DATE, USCPI_TS);
    InflationRateSensitivity point =
        InflationRateSensitivity.of(PriceIndexObservation.of(US_CPI_U, VAL_MONTH.plusMonths(3)), 1d);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.parameterSensitivity(point));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createParameterSensitivity() {
    HistoricPriceIndexValues test = HistoricPriceIndexValues.of(US_CPI_U, VAL_DATE, USCPI_TS);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15, 0.16, 0.17);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    HistoricPriceIndexValues instance1 = HistoricPriceIndexValues.of(US_CPI_U, VAL_DATE, USCPI_TS);
    coverImmutableBean(instance1);
    HistoricPriceIndexValues test2 =
        HistoricPriceIndexValues.of(
            GB_HICP,
            VAL_DATE.plusMonths(1),
            LocalDateDoubleTimeSeries.of(VAL_MONTH.minusMonths(2).atEndOfMonth(), 100d));
    coverBeanEquals(instance1, test2);
  }

}
