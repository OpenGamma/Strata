/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CurrencyParameterSensitivity}.
 */
public class CurrencyParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleArray VECTOR_USD1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR_USD2 = DoubleArray.of(150, 250, 350, 153, 550);
  private static final DoubleArray VECTOR_USD_COMBINED = VECTOR_USD1.concat(VECTOR_USD2);
  private static final DoubleArray VECTOR_USD_FACTOR =
      DoubleArray.of(100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1);
  private static final DoubleArray VECTOR_EUR1 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final DoubleArray VECTOR_EUR1_IN_USD =
      DoubleArray.of(1000 * 1.5, 250 * 1.5, 321 * 1.5, 123 * 1.5, 321 * 1.5);
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.5d);
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final MarketDataName<?> NAME_COMBINED = CurveName.of("NAME_COMBINED");
  private static final List<ParameterMetadata> METADATA_USD1 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA_USD2 = ParameterMetadata.listOfEmpty(5);
  private static final List<ParameterMetadata> METADATA_EUR1 = ParameterMetadata.listOfEmpty(5);
  private static final List<ParameterMetadata> METADATA_BAD = ParameterMetadata.listOfEmpty(1);
  private static final ImmutableList<ParameterMetadata> METADATA_COMBINED =
      ImmutableList.<ParameterMetadata>builder().addAll(METADATA_USD1).addAll(METADATA_USD2).build();
  private static final ParameterSize PARAM1 = ParameterSize.of(NAME1, 4);
  private static final ParameterSize PARAM2 = ParameterSize.of(NAME2, 5);
  private static final List<ParameterSize> PARAM_SPLIT = ImmutableList.of(PARAM1, PARAM2);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_metadata() {
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    assertThat(test.getMarketDataName()).isEqualTo(NAME1);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD1.size());
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA_USD1);
    assertThat(test.getParameterMetadata(0)).isEqualTo(METADATA_USD1.get(0));
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD1);
  }

  @Test
  public void test_of_metadata_badMetadata() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurrencyParameterSensitivity.of(NAME1, METADATA_BAD, USD, VECTOR_USD1));
  }

  @Test
  public void test_of_metadataParamSplit() {
    CurrencyParameterSensitivity test =
        CurrencyParameterSensitivity.of(NAME_COMBINED, METADATA_COMBINED, USD, VECTOR_USD_COMBINED, PARAM_SPLIT);
    assertThat(test.getMarketDataName()).isEqualTo(NAME_COMBINED);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD_COMBINED.size());
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA_COMBINED);
    assertThat(test.getParameterMetadata(0)).isEqualTo(METADATA_COMBINED.get(0));
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD_COMBINED);
    assertThat(test.getParameterSplit()).isEqualTo(Optional.of(PARAM_SPLIT));
  }

  @Test
  public void test_of_metadataParamSplit_badSplit() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurrencyParameterSensitivity.of(NAME_COMBINED, METADATA_USD1, USD, VECTOR_USD1, PARAM_SPLIT));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_map() {
    ImmutableMap<ParameterMetadata, Double> map = ImmutableMap.of(
        TenorParameterMetadata.of(Tenor.TENOR_1Y), 12d,
        TenorParameterMetadata.of(Tenor.TENOR_2Y), -32d,
        TenorParameterMetadata.of(Tenor.TENOR_5Y), 5d);
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.of(NAME1, USD, map);
    assertThat(test.getMarketDataName()).isEqualTo(NAME1);
    assertThat(test.getParameterCount()).isEqualTo(3);
    assertThat(test.getParameterMetadata()).isEqualTo(map.keySet().asList());
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getSensitivity()).isEqualTo(DoubleArray.copyOf(map.values()));
    assertThat(test.sensitivities().toMap()).isEqualTo(map);
    assertThat(test.toSensitivityMap(Tenor.class)).isEqualTo(MapStream.of(map).mapKeys(pm -> pm.getIdentifier()).toMap());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combine() {
    CurrencyParameterSensitivity base1 = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity base2 = CurrencyParameterSensitivity.of(NAME2, METADATA_USD2, USD, VECTOR_USD2);
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.combine(NAME_COMBINED, base1, base2);
    assertThat(test.getMarketDataName()).isEqualTo(NAME_COMBINED);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD_COMBINED.size());
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA_COMBINED);
    assertThat(test.getParameterMetadata(0)).isEqualTo(METADATA_COMBINED.get(0));
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD_COMBINED);
    assertThat(test.getParameterSplit()).isEqualTo(Optional.of(PARAM_SPLIT));
  }

  @Test
  public void test_combine_empty() {
    CurrencyParameterSensitivity base1 = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity base2 = CurrencyParameterSensitivity.of(NAME2, ImmutableList.of(), USD, DoubleArray.of());
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.combine(NAME_COMBINED, base1, base2);
    assertThat(test.getMarketDataName()).isEqualTo(NAME_COMBINED);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD1.size());
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA_USD1);
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD1);
    assertThat(test.getParameterSplit()).isEqualTo(Optional.of(ImmutableList.of(PARAM1)));
  }

  @Test
  public void test_combine_onlyEmpty() {
    CurrencyParameterSensitivity base1 = CurrencyParameterSensitivity.of(NAME1, ImmutableList.of(), USD, DoubleArray.of());
    CurrencyParameterSensitivity base2 = CurrencyParameterSensitivity.of(NAME2, ImmutableList.of(), USD, DoubleArray.of());
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.combine(NAME_COMBINED, base1, base2);
    assertThat(test.getMarketDataName()).isEqualTo(NAME_COMBINED);
    assertThat(test.getParameterCount()).isEqualTo(0);
    assertThat(test.getParameterMetadata()).isEmpty();
    assertThat(test.getSensitivity()).isEqualTo(DoubleArray.EMPTY);
    assertThat(test.getParameterSplit()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_combine_arraySize0() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurrencyParameterSensitivity.combine(NAME_COMBINED));
  }

  @Test
  public void test_combine_arraySize1() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.combine(NAME_COMBINED, base);
    assertThat(test.getMarketDataName()).isEqualTo(NAME_COMBINED);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD1.size());
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA_USD1);
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD1);
    assertThat(test.getParameterSplit()).isEqualTo(Optional.of(ImmutableList.of(PARAM1)));
  }

  @Test
  public void test_combine_arraySize1_matchingName() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.combine(NAME1, base);
    assertThat(test).isEqualTo(base);
  }

  @Test
  public void test_combine_duplicateNames() {
    CurrencyParameterSensitivity base1 = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity base2 = CurrencyParameterSensitivity.of(NAME1, METADATA_USD2, USD, VECTOR_USD2);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurrencyParameterSensitivity.combine(NAME_COMBINED, base1, base2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_EUR1, EUR, VECTOR_EUR1);
    CurrencyParameterSensitivity test = base.convertedTo(USD, FX_RATE);
    assertThat(test).isEqualTo(CurrencyParameterSensitivity.of(NAME1, METADATA_EUR1, USD, VECTOR_EUR1_IN_USD));
  }

  @Test
  public void test_convertedTo_sameCurrency() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_EUR1, EUR, VECTOR_EUR1);
    CurrencyParameterSensitivity test = base.convertedTo(EUR, FX_RATE);
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertThat(test).isEqualTo(CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD_FACTOR));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity test = base.withSensitivity(VECTOR_USD_FACTOR);
    assertThat(test).isEqualTo(CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD_FACTOR));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withSensitivity(DoubleArray.of(1d)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_array() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity test = base.plus(VECTOR_USD1);
    assertThat(test).isEqualTo(base.multipliedBy(2));
  }

  @Test
  public void test_plus_array_wrongSize() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.plus(VECTOR_USD2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_sensitivity() {
    CurrencyParameterSensitivity base1 = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity test = base1.plus(base1);
    assertThat(test).isEqualTo(base1.multipliedBy(2));
  }

  @Test
  public void test_plus_sensitivity_wrongName() {
    CurrencyParameterSensitivity base1 = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity base2 = CurrencyParameterSensitivity.of(NAME2, METADATA_USD1, USD, VECTOR_USD1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base1.plus(base2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_split1() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    ImmutableList<CurrencyParameterSensitivity> test = base.split();
    assertThat(test).hasSize(1);
    assertThat(test.get(0)).isEqualTo(base);
  }

  @Test
  public void test_split2() {
    CurrencyParameterSensitivity base1 = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyParameterSensitivity base2 = CurrencyParameterSensitivity.of(NAME2, METADATA_USD2, USD, VECTOR_USD2);
    CurrencyParameterSensitivity combined = CurrencyParameterSensitivity.combine(NAME_COMBINED, base1, base2);
    ImmutableList<CurrencyParameterSensitivity> test = combined.split();
    assertThat(test).hasSize(2);
    assertThat(test.get(0)).isEqualTo(base1);
    assertThat(test.get(1)).isEqualTo(base2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_total() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    CurrencyAmount test = base.total();
    assertThat(test.getCurrency()).isEqualTo(USD);
    double expected = VECTOR_USD1.get(0) + VECTOR_USD1.get(1) + VECTOR_USD1.get(2) + VECTOR_USD1.get(3);
    assertThat(test.getAmount()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toSensitivityMap_badType() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> base.toSensitivityMap(Tenor.class));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toUnitParameterSensitivity() {
    CurrencyParameterSensitivity base = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    UnitParameterSensitivity test = base.toUnitParameterSensitivity();
    assertThat(test).isEqualTo(UnitParameterSensitivity.of(NAME1, METADATA_USD1, VECTOR_USD1));
  }
  
  @Test
  public void test_toUnitParameterSensitivity_parameterSplit() {
    List<ParameterSize> parameterSplit = 
        ImmutableList.of(ParameterSize.of(CurveName.of("NAME-1-1"), 3), ParameterSize.of(CurveName.of("NAME-1-2"), 1));
    CurrencyParameterSensitivity base = 
        CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1, parameterSplit);
    UnitParameterSensitivity test = base.toUnitParameterSensitivity();
    assertThat(test).isEqualTo(UnitParameterSensitivity.of(NAME1, METADATA_USD1, VECTOR_USD1, parameterSplit));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CurrencyParameterSensitivity test = CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, VECTOR_USD1);
    coverImmutableBean(test);
    CurrencyParameterSensitivity test2 = CurrencyParameterSensitivity.of(NAME2, METADATA_EUR1, EUR, VECTOR_EUR1);
    coverBeanEquals(test, test2);
  }

}
