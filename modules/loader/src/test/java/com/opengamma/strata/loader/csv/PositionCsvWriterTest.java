/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.common.ExchangeIds;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.etd.EtdContractCode;
import com.opengamma.strata.product.etd.EtdContractSpec;
import com.opengamma.strata.product.etd.EtdContractSpecId;
import com.opengamma.strata.product.etd.EtdFuturePosition;
import com.opengamma.strata.product.etd.EtdFutureSecurity;
import com.opengamma.strata.product.etd.EtdOptionPosition;
import com.opengamma.strata.product.etd.EtdOptionSecurity;
import com.opengamma.strata.product.etd.EtdType;
import com.opengamma.strata.product.etd.EtdVariant;

/**
 * Test {@link PositionCsvWriter}.
 */
class PositionCsvWriterTest {

  private static EtdContractSpec FUTURE_CONTRACT;
  private static EtdContractSpec OPTION_CONTRACT;
  private static EtdContractSpecId FUTURE_SPEC_ID;
  private static EtdContractSpecId OPTION_SPEC_ID;
  private static final ResourceLocator FILE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/positions-2.csv");

  @BeforeAll
  static void beforeAll() {
    FUTURE_SPEC_ID = EtdContractSpecId.of("OG-ETD", "F-ECAG-FGBL");

    OPTION_SPEC_ID = EtdContractSpecId.of("OG-ETD", "O-ECAG-OGBL");

    FUTURE_CONTRACT = EtdContractSpec.builder()
        .id(FUTURE_SPEC_ID)
        .type(EtdType.FUTURE)
        .exchangeId(ExchangeIds.ECAG)
        .contractCode(EtdContractCode.of("FGBL"))
        .description("Dummy")
        .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
        .build();

    OPTION_CONTRACT = EtdContractSpec.builder()
        .id(OPTION_SPEC_ID)
        .type(EtdType.OPTION)
        .exchangeId(ExchangeIds.ECAG)
        .contractCode(EtdContractCode.of("OGBL"))
        .description("Dummy")
        .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
        .build();
  }

  @Test
  void test_write_etdFuturePosition() {
    EtdFuturePosition position = EtdFuturePosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123424"))
            .build())
        .security(EtdFutureSecurity.of(
            FUTURE_CONTRACT,
            YearMonth.of(2017, 6),
            EtdVariant.ofDaily(3)))
        .longQuantity(30d)
        .shortQuantity(0d)
        .build();

    StringBuffer buf = new StringBuffer();
    PositionCsvWriter.standard().write(ImmutableList.of(position), buf);
    String content = buf.toString();

    String expected = "Strata Position Type,Id Scheme,Id,Exchange,Contract Code,Long Quantity,Short Quantity,Expiry,Expiry Day\n" +
        "FUT,OG,123424,ECAG,FGBL,30,0,2017-06,3\n";
    assertThat(content).isEqualTo(expected);
  }

  @Test
  void test_write_etdOptionPosition() {
    EtdOptionPosition position = EtdOptionPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123431"))
            .build())
        .security(EtdOptionSecurity.of(
            OPTION_CONTRACT,
            YearMonth.of(2017, 6),
            EtdVariant.ofMonthly(), 0,
            PutCall.PUT, 3d,
            YearMonth.of(2017, 9)))
        .longQuantity(15d)
        .shortQuantity(2d)
        .build();

    StringBuffer buf = new StringBuffer();
    PositionCsvWriter.standard().write(ImmutableList.of(position), buf);
    String content = buf.toString();

    String expected = "Strata Position Type,Id Scheme,Id,Exchange,Contract Code,Long Quantity,Short Quantity,Expiry,Put Call,Exercise Price,Underlying Expiry\n"
        + "OPT,OG,123431,ECAG,OGBL,15,2,2017-06,Put,3,2017-09\n";
    assertThat(content).isEqualTo(expected);
  }

  @Test
  void test_write_roundTrip() {
    ReferenceData referenceData = ImmutableReferenceData.of(ImmutableMap.of(
        FUTURE_SPEC_ID, FUTURE_CONTRACT,
        OPTION_SPEC_ID, OPTION_CONTRACT));
    PositionCsvLoader loader = PositionCsvLoader.of(referenceData);

    ValueWithFailures<List<Position>> parsed1 = loader.parse(ImmutableList.of(FILE.getCharSource()));
    assertThat(parsed1.getFailures()).isEmpty();
    List<Position> positionsList1 = parsed1.getValue();
    assertThat(positionsList1).hasSize(2);

    StringBuffer buf = new StringBuffer();
    PositionCsvWriter.standard().write(positionsList1, buf);
    String content = buf.toString();

    ValueWithFailures<List<Position>> parsed2 = loader.parse(ImmutableList.of(CharSource.wrap(content)));
    assertThat(parsed2.getFailures()).isEmpty();
    List<Position> positionList2 = parsed2.getValue();
    assertThat(positionList2).hasSize(2);

    assertThat(positionList2).isEqualTo(positionsList1);
  }

}
