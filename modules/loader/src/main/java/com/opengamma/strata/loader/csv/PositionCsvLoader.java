/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.io.UnicodeBom;
import com.opengamma.strata.collect.result.FailureItem;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.product.GenericSecurityPosition;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.PositionInfoBuilder;
import com.opengamma.strata.product.ResolvableSecurityPosition;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.etd.EtdContractSpec;
import com.opengamma.strata.product.etd.EtdContractSpecId;
import com.opengamma.strata.product.etd.EtdFuturePosition;
import com.opengamma.strata.product.etd.EtdIdUtils;
import com.opengamma.strata.product.etd.EtdOptionPosition;
import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.etd.EtdPosition;
import com.opengamma.strata.product.etd.EtdSettlementType;

/**
 * Loads positions from CSV files.
 * <p>
 * The positions are expected to be in a CSV format known to Strata.
 * The parser is flexible, understanding a number of different ways to define each position.
 * Columns may occur in any order.
 * 
 * <h4>Common</h4>
 * <p>
 * The following standard columns are supported:<br />
 * <ul>
 * <li>The 'Strata Position Type' column is optional, but mandatory when checking headers
 * to see if the file is a known format. It defines the instrument type,
 *   'SEC' or'Security' for standard securities,
 *   'FUT' or 'Future' for ETD futures, and
 *   'OPT' or 'Option' for ETD options.
 *   If absent, the type is derived based on the presence or absence of the 'Expiry' column.
 * <li>The 'Id Scheme' column is optional, and is the name of the scheme that the position
 *   identifier is unique within, such as 'OG-Position'.
 * <li>The 'Id' column is optional, and is the identifier of the position,
 *   such as 'POS12345'.
 * </ul>
 * 
 * <h4>SEC/Security</h4>
 * <p>
 * The following columns are supported:
 * <ul>
 * <li>'Security Id Scheme' - optional, defaults to 'OG-Security'
 * <li>'Security Id' - mandatory
 * <li>'Quantity' - see below
 * <li>'Long Quantity' - see below
 * <li>'Short Quantity' - see below
 * </ul>
 * <p>
 * The quantity will normally be set from the 'Quantity' column.
 * If that column is not found, the 'Long Quantity' and 'Short Quantity' columns will be used instead.
 * 
 * <h4>FUT/Future</h4>
 * <p>
 * The following columns are supported:
 * <ul>
 * <li>'Exchange' - mandatory, the MIC code of the exchange where the ETD is traded
 * <li>'Contract Code' - mandatory, the contract code of the ETD at the exchange
 * <li>'Quantity' - see below
 * <li>'Long Quantity' - see below
 * <li>'Short Quantity' - see below
 * <li>'Expiry' - mandatory, the year-month of the expiry, in the format 'yyyy-MM'
 * <li>'Expiry Week' - optional, only used to obtain a weekly-expiring ETD
 * <li>'Expiry Day' - optional, only used to obtain a daily-expiring ETD, or Flex
 * <li>'Settlement Type' - optional, only used for Flex, see {@link EtdSettlementType}
 * </ul>
 * <p>
 * The exchange and contract code are combined to form an {@link EtdContractSpecId} which is
 * resolved in {@link ReferenceData} to find additional details about the ETD.
 * This process can be changed by providing an alternative {@link PositionCsvInfoResolver}.
 * <p>
 * The quantity will normally be set from the 'Quantity' column.
 * If that column is not found, the 'Long Quantity' and 'Short Quantity' columns will be used instead.
 * <p>
 * The expiry is normally controlled using just the 'Expiry' column.
 * Flex options will also set the 'Expiry Day' and 'Settlement Type'.
 * 
 * <h4>OPT/Option</h4>
 * <p>
 * The following columns are supported:
 * <ul>
 * <li>'Exchange' - mandatory, the MIC code of the exchange where the ETD is traded
 * <li>'Contract Code' - mandatory, the contract code of the ETD at the exchange
 * <li>'Quantity' - see below
 * <li>'Long Quantity' - see below
 * <li>'Short Quantity' - see below
 * <li>'Expiry' - mandatory, the year-month of the expiry, in the format 'yyyy-MM'
 * <li>'Expiry Week' - optional, only used to obtain a weekly-expiring ETD
 * <li>'Expiry Day' - optional, only used to obtain a daily-expiring ETD, or Flex
 * <li>'Settlement Type' - optional, only used for Flex, see {@link EtdSettlementType}
 * <li>'Exercise Style' - optional,  only used for Flex, see {@link EtdOptionType}
 * <li>'Put Call' - mandatory,  'Put', 'P', 'Call' or 'C'
 * <li>'Exercise Price' - mandatory,  the strike price, such as 1.23
 * <li>'Version' - optional, the version of the contract, not widely used, defaults to zero
 * <li>'Underlying Expiry' - optional, the expiry year-month of the underlying instrument if applicable, in the format 'yyyy-MM'
 * </ul>
 * <p>
 * The exchange and contract code are combined to form an {@link EtdContractSpecId} which is
 * resolved in {@link ReferenceData} to find additional details about the ETD.
 * This process can be changed by providing an alternative {@link PositionCsvInfoResolver}.
 * <p>
 * The quantity will normally be set from the 'Quantity' column.
 * If that column is not found, the 'Long Quantity' and 'Short Quantity' columns will be used instead.
 * <p>
 * The expiry is normally controlled using just the 'Expiry' column.
 * Flex options will also set the 'Expiry Day', 'Settlement Type' and 'Exercise Style'.
 */
public final class PositionCsvLoader {

  // default schemes
  static final String DEFAULT_POSITION_SCHEME = "OG-Position";
  static final String DEFAULT_SECURITY_SCHEME = "OG-Security";

  // CSV column headers
  private static final String TYPE_FIELD = "Strata Position Type";
  private static final String ID_SCHEME_FIELD = "Id Scheme";
  private static final String ID_FIELD = "Id";

  /**
   * The resolver, providing additional information.
   */
  private final PositionCsvInfoResolver resolver;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static PositionCsvLoader standard() {
    return new PositionCsvLoader(PositionCsvInfoResolver.standard());
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static PositionCsvLoader of(ReferenceData refData) {
    return new PositionCsvLoader(PositionCsvInfoResolver.of(refData));
  }

  /**
   * Obtains an instance that uses the specified resolver for additional information.
   * 
   * @param resolver  the resolver used to parse additional information
   * @return the loader
   */
  public static PositionCsvLoader of(PositionCsvInfoResolver resolver) {
    return new PositionCsvLoader(resolver);
  }

  // restricted constructor
  private PositionCsvLoader(PositionCsvInfoResolver resolver) {
    this.resolver = ArgChecker.notNull(resolver, "resolver");
  }

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format position files.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * This method uses {@link UnicodeBom} to interpret it.
   * 
   * @param resources  the CSV resources
   * @return the loaded positions, position-level errors are captured in the result
   */
  public ValueWithFailures<List<Position>> load(ResourceLocator... resources) {
    return load(Arrays.asList(resources));
  }

  /**
   * Loads one or more CSV format position files.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * This method uses {@link UnicodeBom} to interpret it.
   * 
   * @param resources  the CSV resources
   * @return the loaded positions, all errors are captured in the result
   */
  public ValueWithFailures<List<Position>> load(Collection<ResourceLocator> resources) {
    Collection<CharSource> charSources = resources.stream()
        .map(r -> UnicodeBom.toCharSource(r.getByteSource()))
        .collect(toList());
    return parse(charSources);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks whether the source is a CSV format position file.
   * <p>
   * This parses the headers as CSV and checks that mandatory headers are present.
   * This is determined entirely from the 'Strata Position Type' column.
   * 
   * @param charSource  the CSV character source to check
   * @return true if the source is a CSV file with known headers, false otherwise
   */
  public boolean isKnownFormat(CharSource charSource) {
    try (CsvIterator csv = CsvIterator.of(charSource, true)) {
      return csv.containsHeader(TYPE_FIELD);
    } catch (RuntimeException ex) {
      return false;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Parses one or more CSV format position files, returning ETD futures and
   * options using information from reference data.
   * <p>
   * When an ETD row is found, reference data is used to find the correct security.
   * This uses {@link EtdContractSpec} by default, although this can be overridden in the resolver.
   * Futures and options will be returned as {@link EtdFuturePosition} and {@link EtdOptionPosition}.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
   * 
   * @param charSources  the CSV character sources
   * @return the loaded positions, all errors are captured in the result
   */
  public ValueWithFailures<List<Position>> parse(Collection<CharSource> charSources) {
    return parse(charSources, Position.class);
  }

  /**
   * Parses one or more CSV format position files, returning ETD futures and
   * options by identifier without using reference data.
   * <p>
   * When an ETD row is found, {@link EtdIdUtils} is used to create an identifier.
   * The identifier is used to create a {@link SecurityPosition}, with no call to reference data.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
   * 
   * @param charSources  the CSV character sources
   * @return the loaded positions, all errors are captured in the result
   */
  public ValueWithFailures<List<SecurityPosition>> parseLightweight(Collection<CharSource> charSources) {
    return parse(charSources, SecurityPosition.class);
  }

  /**
   * Parses one or more CSV format position files.
   * <p>
   * A type is specified to filter the positions.
   * If the type is {@link SecurityPosition}, then ETD parsing will proceed as per {@link #parseLightweight(Collection)}.
   * Otherwise, ETD parsing will proceed as per {@link #parse(Collection)}.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
   * 
   * @param <T>  the position type
   * @param charSources  the CSV character sources
   * @param positionType  the position type to return
   * @return the loaded positions, all errors are captured in the result
   */
  public <T extends Position> ValueWithFailures<List<T>> parse(Collection<CharSource> charSources, Class<T> positionType) {
    try {
      ValueWithFailures<List<T>> result = ValueWithFailures.of(ImmutableList.of());
      for (CharSource charSource : charSources) {
        ValueWithFailures<List<T>> singleResult = parseFile(charSource, positionType);
        result = result.combinedWith(singleResult, Guavate::concatToList);
      }
      return result;

    } catch (RuntimeException ex) {
      return ValueWithFailures.of(ImmutableList.of(), FailureItem.of(FailureReason.ERROR, ex));
    }
  }

  // loads a single CSV file, filtering by position type
  private <T extends Position> ValueWithFailures<List<T>> parseFile(CharSource charSource, Class<T> positionType) {
    try (CsvIterator csv = CsvIterator.of(charSource, true)) {
      if (!csv.headers().contains(TYPE_FIELD)) {
        return ValueWithFailures.of(
            ImmutableList.of(),
            FailureItem.of(FailureReason.PARSING, "CSV file does not contain '{header}' header: {}", TYPE_FIELD, charSource));
      }
      return parseFile(csv, positionType);

    } catch (RuntimeException ex) {
      return ValueWithFailures.of(
          ImmutableList.of(),
          FailureItem.of(
              FailureReason.PARSING, ex, "CSV file could not be parsed: {exceptionMessage}: {}", ex.getMessage(), charSource));
    }
  }

  // loads a single CSV file
  private <T extends Position> ValueWithFailures<List<T>> parseFile(CsvIterator csv, Class<T> posType) {
    List<T> positions = new ArrayList<>();
    List<FailureItem> failures = new ArrayList<>();
    int line = 2;
    for (CsvRow row : (Iterable<CsvRow>) () -> csv) {
      try {
        PositionInfo info = parsePositionInfo(row);
        Optional<String> typeRawOpt = row.findValue(TYPE_FIELD);
        if (typeRawOpt.isPresent()) {
          // type specified
          String type = typeRawOpt.get().toUpperCase(Locale.ENGLISH);
          switch (type.toUpperCase(Locale.ENGLISH)) {
            case "SEC":
            case "SECURITY":
              if (posType == SecurityPosition.class || posType == ResolvableSecurityPosition.class) {
                positions.add(posType.cast(SecurityCsvLoader.parseSecurityPosition(row, info, resolver)));
              } else if (posType == GenericSecurityPosition.class || posType == Position.class) {
                Position parsed = SecurityCsvLoader.parseNonEtdPosition(row, info, resolver);
                if (posType.isInstance(parsed)) {
                  positions.add(posType.cast(parsed));
                }
              }
              break;
            case "FUT":
            case "FUTURE":
              if (posType == EtdPosition.class || posType == EtdFuturePosition.class ||
                  posType == ResolvableSecurityPosition.class || posType == Position.class) {
                positions.add(posType.cast((Position) resolver.parseEtdFuturePosition(row, info)));
              } else if (posType == SecurityPosition.class) {
                positions.add(posType.cast(resolver.parseEtdFutureSecurityPosition(row, info)));
              }
              break;
            case "OPT":
            case "OPTION":
              if (posType == EtdPosition.class || posType == EtdOptionPosition.class ||
                  posType == ResolvableSecurityPosition.class || posType == Position.class) {
                positions.add(posType.cast(resolver.parseEtdOptionPosition(row, info)));
              } else if (posType == SecurityPosition.class) {
                positions.add(posType.cast(resolver.parseEtdOptionSecurityPosition(row, info)));
              }
              break;
            default:
              failures.add(FailureItem.of(
                  FailureReason.PARSING,
                  "CSV file position type '{positionType}' is not known at line {lineNumber}",
                  typeRawOpt.get(),
                  line));
              break;
          }
        } else {
          // infer type
          if (posType == SecurityPosition.class) {
            positions.add(posType.cast(SecurityCsvLoader.parsePositionLightweight(row, info, resolver)));
          } else {
            Position position = SecurityCsvLoader.parsePosition(row, info, resolver);
            if (posType.isInstance(position)) {
              positions.add(posType.cast(position));
            }
          }
        }
      } catch (RuntimeException ex) {
        failures.add(FailureItem.of(
            FailureReason.PARSING,
            ex,
            "CSV file position could not be parsed at line {lineNumber}: {exceptionMessage}",
            line,
            ex.getMessage()));
      }
      line++;
    }
    return ValueWithFailures.of(positions, failures);
  }

  // parse the position info
  private PositionInfo parsePositionInfo(CsvRow row) {
    PositionInfoBuilder infoBuilder = PositionInfo.builder();
    String scheme = row.findField(ID_SCHEME_FIELD).orElse(DEFAULT_POSITION_SCHEME);
    row.findValue(ID_FIELD).ifPresent(id -> infoBuilder.id(StandardId.of(scheme, id)));
    resolver.parsePositionInfo(row, infoBuilder);
    return infoBuilder.build();
  }

}
