/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.result.FailureReason.PARSING;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FloatingRateName;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.io.UnicodeBom;
import com.opengamma.strata.collect.result.FailureItem;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.market.param.TenoredParameterMetadata;
import com.opengamma.strata.market.sensitivity.CurveSensitivities;
import com.opengamma.strata.market.sensitivity.CurveSensitivitiesBuilder;
import com.opengamma.strata.market.sensitivity.CurveSensitivitiesType;
import com.opengamma.strata.product.PortfolioItemInfo;

/**
 * Loads sensitivities from CSV files.
 * <p>
 * The sensitivities are expected to be in a CSV format known to Strata.
 * The parser currently supports two different CSV formats.
 * Columns may occur in any order.
 * 
 * <h4>Standard format</h4>
 * <p>
 * The following columns are supported:
 * <ul>
 * <li>'Id Scheme' (optional) - the name of the scheme that the identifier is unique within, defaulted to 'OG-Sensitivity'.
 * <li>'Id' (optional) - the identifier of the sensitivity, such as 'SENS12345'.
 * <li>'Reference' - a currency, floating rate name, index name or curve name.
 *   The standard reference name for a discount curve is the currency, such as 'GBP'.
 *   The standard reference name for a forward curve is the index name, such as 'GBP-LIBOR-3M'.
 *   Any curve name may be used however, which will be specific to the market data setup.
 * <li>'Sensitivity Type' - defines the type of the sensitivity value, such as 'ZeroRateDelta' or 'ZeroRateGamma'.
 * <li>'Sensitivity Tenor' - the tenor of the bucketed sensitivity, such as '1Y'.
 * <li>'Sensitivity Date' (optional) - the date of the bucketed sensitivity, such as '2018-06-01'.
 * <li>'Currency' (optional) - the currency of each sensitivity value, such as 'GBP'.
 *   If omitted, the currency will be implied from the reference, which must start with the currency.
 * <li>'Value' - the sensitivity value
 * </ul>
 * <p>
 * The identifier columns are not normally present as the identifier is completely optional.
 * If present, the values must be repeated for each row that forms part of one sensitivity.
 * If the parser finds a different identifier, it will create a second sensitivity instance.
 * <p>
 * When parsing the value column, if the cell is empty, the combination of type/reference/tenor/value
 * will not be added to the result, so use an explicit zero to include a zero value.
 * 
 * <h4>List format</h4>
 * <p>
 * The following columns are supported:
 * <ul>
 * <li>'Id Scheme' (optional) - the name of the scheme that the identifier is unique within, defaulted to 'OG-Sensitivity'.
 * <li>'Id' (optional) - the identifier of the sensitivity, such as 'SENS12345'.
 * <li>'Reference' - a currency, floating rate name, index name or curve name.
 *   The standard reference name for a discount curve is the currency, such as 'GBP'.
 *   The standard reference name for a forward curve is the index name, such as 'GBP-LIBOR-3M'.
 *   Any curve name may be used however, which will be specific to the market data setup.
 * <li>'Sensitivity Tenor' - the tenor of the bucketed sensitivity, such as '1Y'.
 * <li>'Sensitivity Date' (optional) - the date of the bucketed sensitivity, such as '2018-06-01'.
 * <li>'Currency' (optional) - the currency of each sensitivity value, such as 'GBP'.
 *   If omitted, the currency will be implied from the reference, which must start with the currency.
 * <li>one or more sensitivity value columns, the type of the sensitivity is specified by the header name,
 *   such as 'ZeroRateDelta'.
 * </ul>
 * <p>
 * The identifier columns are not normally present as the identifier is completely optional.
 * If present, the values must be repeated for each row that forms part of one sensitivity.
 * If the parser finds a different identifier, it will create a second sensitivity instance.
 * <p>
 * When parsing the value columns, if the cell is empty, the combination of type/reference/tenor/value
 * will not be added to the result, so use an explicit zero to include a zero value.
 * 
 * <h4>Grid format</h4>
 * <p>
 * The following columns are supported:<br />
 * <ul>
 * <li>'Id Scheme' (optional) - the name of the scheme that the identifier is unique within, defaulted to 'OG-Sensitivity'.
 * <li>'Id' (optional) - the identifier of the sensitivity, such as 'SENS12345'.
 * <li>'Sensitivity Type' - defines the type of the sensitivity value, such as 'ZeroRateDelta' or 'ZeroRateGamma'.
 * <li>'Sensitivity Tenor' - the tenor of the bucketed sensitivity, such as '1Y'.
 * <li>'Sensitivity Date' (optional) - the date of the bucketed sensitivity, such as '2018-06-01'.
 * <li>'Currency' (optional) - the currency of each sensitivity value, such as 'GBP'.
 *   If omitted, the currency will be implied from the reference, which must start with the currency.
 * <li>one or more sensitivity value columns, the reference of the sensitivity is specified by the header name.
 *   The reference can be a currency, floating rate name, index name or curve name.
 *   The standard reference name for a discount curve is the currency, such as 'GBP'.
 *   The standard reference name for a forward curve is the index name, such as 'GBP-LIBOR-3M'.
 *   Any curve name may be used however, which will be specific to the market data setup.
 * </ul>
 * <p>
 * The identifier columns are not normally present as the identifier is completely optional.
 * If present, the values must be repeated for each row that forms part of one sensitivity.
 * If the parser finds a different identifier, it will create a second sensitivity instance.
 * <p>
 * When parsing the value columns, if the cell is empty, the combination of type/reference/tenor/value
 * will not be added to the result, so use an explicit zero to include a zero value.
 * 
 * <h4>Resolver</h4>
 * The standard resolver will ensure that the sensitivity always has a tenor and
 * implements {@link TenoredParameterMetadata}.
 * The resolver can be adjusted to allow date-only metadata (thereby making the 'Sensitivity Tenor' column optional).
 * The resolver can manipulate the tenor and/or curve name that is parsed if desired.
 */
public final class SensitivityCsvLoader {

  // default schemes
  static final String DEFAULT_SCHEME = "OG-Sensitivity";

  // CSV column headers
  private static final String ID_SCHEME_HEADER = "Id Scheme";
  private static final String ID_HEADER = "Id";
  static final String REFERENCE_HEADER = "Reference";
  static final String TYPE_HEADER = "Sensitivity Type";
  static final String TENOR_HEADER = "Sensitivity Tenor";
  static final String DATE_HEADER = "Sensitivity Date";
  static final String CURRENCY_HEADER = "Currency";
  static final String VALUE_HEADER = "Value";
  private static final ImmutableSet<String> TYPE_HEADERS =
      ImmutableSet.of(
          ID_SCHEME_HEADER.toLowerCase(Locale.ENGLISH),
          ID_HEADER.toLowerCase(Locale.ENGLISH),
          REFERENCE_HEADER.toLowerCase(Locale.ENGLISH),
          TENOR_HEADER.toLowerCase(Locale.ENGLISH),
          DATE_HEADER.toLowerCase(Locale.ENGLISH),
          CURRENCY_HEADER.toLowerCase(Locale.ENGLISH));
  private static final ImmutableSet<String> REF_HEADERS =
      ImmutableSet.of(
          ID_SCHEME_HEADER.toLowerCase(Locale.ENGLISH),
          ID_HEADER.toLowerCase(Locale.ENGLISH),
          TYPE_HEADER.toLowerCase(Locale.ENGLISH),
          TENOR_HEADER.toLowerCase(Locale.ENGLISH),
          DATE_HEADER.toLowerCase(Locale.ENGLISH),
          CURRENCY_HEADER.toLowerCase(Locale.ENGLISH));

  /**
   * The resolver, providing additional information.
   */
  private final SensitivityCsvInfoResolver resolver;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static SensitivityCsvLoader standard() {
    return new SensitivityCsvLoader(SensitivityCsvInfoResolver.standard());
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static SensitivityCsvLoader of(ReferenceData refData) {
    return new SensitivityCsvLoader(SensitivityCsvInfoResolver.of(refData));
  }

  /**
   * Obtains an instance that uses the specified resolver for additional information.
   * 
   * @param resolver  the resolver used to parse additional information
   * @return the loader
   */
  public static SensitivityCsvLoader of(SensitivityCsvInfoResolver resolver) {
    return new SensitivityCsvLoader(resolver);
  }

  // restricted constructor
  private SensitivityCsvLoader(SensitivityCsvInfoResolver resolver) {
    this.resolver = ArgChecker.notNull(resolver, "resolver");
  }

  //-------------------------------------------------------------------------
  /**
   * Checks whether the source is a CSV format sensitivities file.
   * <p>
   * This parses the headers as CSV and checks that mandatory headers are present.
   * 
   * @param charSource  the CSV character source to check
   * @return true if the source is a CSV file with known headers, false otherwise
   */
  public boolean isKnownFormat(CharSource charSource) {
    try (CsvIterator csv = CsvIterator.of(charSource, true)) {
      if (!csv.containsHeader(TENOR_HEADER) && !csv.containsHeader(DATE_HEADER)) {
        return false;
      }
      if (csv.containsHeader(REFERENCE_HEADER) && csv.containsHeader(TYPE_HEADER) && csv.containsHeader(VALUE_HEADER)) {
        return true;  // standard format
      } else if (csv.containsHeader(REFERENCE_HEADER) || csv.containsHeader(TYPE_HEADER)) {
        return true;  // list or grid format
      } else {
        return csv.headers().stream().anyMatch(SensitivityCsvLoader::knownReference);  // implied grid format
      }
    } catch (RuntimeException ex) {
      return false;
    }
  }

  // for historical compatibility, we determine known format by looking for these specific things
  // the new approach is to require either the 'Reference' or the 'Sensitivity Type' column
  private static boolean knownReference(String refStr) {
    try {
      Optional<IborIndex> ibor = IborIndex.extendedEnum().find(refStr);
      if (ibor.isPresent()) {
        return true;
      } else {
        Optional<FloatingRateName> frName = FloatingRateName.extendedEnum().find(refStr);
        if (frName.isPresent()) {
          return true;
        } else if (refStr.length() == 3) {
          Currency.of(refStr);  // this may throw an exception validating the string
          return true;
        } else {
          return false;
        }
      }
    } catch (RuntimeException ex) {
      return false;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format sensitivities files.
   * <p>
   * In most cases each file contains one sensitivity instance, however the file format is capable
   * of representing any number.
   * <p>
   * Within a single file and identifier, the same combination of type, reference and tenor must not be repeated.
   * No checks are performed between different input files.
   * It may be useful to merge the sensitivities in the resulting list in a separate step after parsing.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * This method uses {@link UnicodeBom} to interpret it.
   * 
   * @param resources  the CSV resources
   * @return the sensitivities keyed by identifier, parsing errors are captured in the result
   */
  public ValueWithFailures<ListMultimap<String, CurveSensitivities>> load(Collection<ResourceLocator> resources) {
    Collection<CharSource> charSources = resources.stream()
        .map(r -> r.getByteSource().asCharSourceUtf8UsingBom())
        .collect(toList());
    return parse(charSources);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses one or more CSV format position files, merging the result to a single sensitivities instance.
   * <p>
   * The standard way to write sensitivities files is for each file to contain one sensitivity instance.
   * The file format can handle multiple instances per file, where each instance has a separate identifier.
   * Most files will not have the identifier columns, thus the identifier will be the empty string.
   * This file handles the common case where the identifier is irrelevant by merging the sensitivities
   * using {@link CurveSensitivities#mergedWith(CurveSensitivities)}
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
   * 
   * @param charSources  the CSV character sources
   * @return the loaded sensitivities, parsing errors are captured in the result
   */
  public ValueWithFailures<CurveSensitivities> parseAndMerge(Collection<CharSource> charSources) {
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> parsed = parse(charSources);
    return parsed.withValue(parsed.getValue().values().stream()
        .reduce(CurveSensitivities.empty(), CurveSensitivities::mergedWith));
  }

  /**
   * Parses one or more CSV format position files, returning sensitivities.
   * <p>
   * The standard way to write sensitivities files is for each file to contain one sensitivity instance.
   * The file format can handle multiple instances per file, where each instance has a separate identifier.
   * Most files will not have the identifier columns, thus the identifier will be the empty string.
   * <p>
   * The returned multimap is keyed by identifier. The value will contain one entry for each instance.
   * If desired, the results can be reduced using {@link CurveSensitivities#mergedWith(CurveSensitivities)}
   * to merge those with the same identifier.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
   * 
   * @param charSources  the CSV character sources
   * @return the loaded sensitivities, parsing errors are captured in the result
   */
  public ValueWithFailures<ListMultimap<String, CurveSensitivities>> parse(Collection<CharSource> charSources) {
    ListMultimap<String, CurveSensitivities> parsed = ArrayListMultimap.create();
    List<FailureItem> failures = new ArrayList<>();
    for (CharSource charSource : charSources) {
      parse(charSource, parsed, failures);
    }
    return ValueWithFailures.of(ImmutableListMultimap.copyOf(parsed), failures);
  }

  // parse a single file
  private void parse(
      CharSource charSource,
      ListMultimap<String, CurveSensitivities> parsed,
      List<FailureItem> failures) {

    try (CsvIterator csv = CsvIterator.of(charSource, true)) {
      if (!csv.containsHeader(TENOR_HEADER) && !csv.containsHeader(DATE_HEADER)) {
        failures.add(FailureItem.of(
            FailureReason.PARSING, "CSV file could not be parsed as sensitivities, invalid format"));
      } else if (csv.containsHeader(REFERENCE_HEADER) &&
          csv.containsHeader(TYPE_HEADER) &&
          csv.containsHeader(VALUE_HEADER)) {
        parseStandardFormat(csv, parsed, failures);
      } else if (csv.containsHeader(REFERENCE_HEADER)) {
        parseListFormat(csv, parsed, failures);
      } else {
        parseGridFormat(csv, parsed, failures);
      }
    } catch (RuntimeException ex) {
      failures.add(FailureItem.of(FailureReason.PARSING, ex, "CSV file could not be parsed: {}", ex.getMessage()));
    }
  }

  //-------------------------------------------------------------------------
  // parses the file in standard format
  private void parseStandardFormat(
      CsvIterator csv,
      ListMultimap<String, CurveSensitivities> parsed,
      List<FailureItem> failures) {

    // loop around all rows, peeking to match batches with the same identifier
    // no exception catch at this level to avoid infinite loops
    while (csv.hasNext()) {
      CsvRow peekedRow = csv.peek();
      PortfolioItemInfo info = parseInfo(peekedRow);
      String id = info.getId().map(StandardId::toString).orElse("");

      // process in batches, where the ID is the same
      CurveSensitivitiesBuilder builder = CurveSensitivities.builder(info);
      List<CsvRow> batchRows = csv.nextBatch(r -> matchId(r, id));
      for (CsvRow batchRow : batchRows) {
        try {
          CurveName reference = CurveName.of(batchRow.getValue(REFERENCE_HEADER));
          CurveName resolvedCurveName = resolver.checkCurveName(reference);
          CurveSensitivitiesType type = CurveSensitivitiesType.of(batchRow.getValue(TYPE_HEADER));
          ParameterMetadata metadata = parseMetadata(batchRow, false);
          Currency currency = parseCurrency(batchRow, reference);
          String valueStr = batchRow.getField(VALUE_HEADER);
          if (!valueStr.isEmpty()) {
            double value = LoaderUtils.parseDouble(valueStr);
            builder.add(type, resolvedCurveName, currency, metadata, value);
          }

        } catch (IllegalArgumentException ex) {
          failures.add(FailureItem.of(
              PARSING, "CSV file could not be parsed at line {}: {}", batchRow.lineNumber(), ex.getMessage()));
        }
      }
      CurveSensitivities sens = builder.build();
      if (!sens.getTypedSensitivities().isEmpty()) {
        parsed.put(sens.getId().map(Object::toString).orElse(""), sens);
      }
    }
  }

  //-------------------------------------------------------------------------
  // parses the file in list format
  private void parseListFormat(
      CsvIterator csv,
      ListMultimap<String, CurveSensitivities> parsed,
      List<FailureItem> failures) {

    // find the applicable type columns
    Map<String, CurveSensitivitiesType> types = new LinkedHashMap<>();
    for (String header : csv.headers()) {
      String headerLowerCase = header.toLowerCase(Locale.ENGLISH);
      if (!TYPE_HEADERS.contains(headerLowerCase) && !resolver.isInfoColumn(headerLowerCase)) {
        types.put(header, CurveSensitivitiesType.of(header.replace(" ", "")));
      }
    }

    // loop around all rows, peeking to match batches with the same identifier
    // no exception catch at this level to avoid infinite loops
    while (csv.hasNext()) {
      CsvRow peekedRow = csv.peek();
      PortfolioItemInfo info = parseInfo(peekedRow);
      String id = info.getId().map(StandardId::toString).orElse("");

      // process in batches, where the ID is the same
      CurveSensitivitiesBuilder builder = CurveSensitivities.builder(info);
      List<CsvRow> batchRows = csv.nextBatch(r -> matchId(r, id));
      for (CsvRow batchRow : batchRows) {
        try {
          ParameterMetadata metadata = parseMetadata(batchRow, true);
          CurveName reference = CurveName.of(batchRow.getValue(REFERENCE_HEADER));
          CurveName resolvedCurveName = resolver.checkCurveName(reference);
          for (Entry<String, CurveSensitivitiesType> entry : types.entrySet()) {
            CurveSensitivitiesType type = entry.getValue();
            String valueStr = batchRow.getField(entry.getKey());
            Currency currency = parseCurrency(batchRow, reference);
            if (!valueStr.isEmpty()) {
              double value = LoaderUtils.parseDouble(valueStr);
              builder.add(type, resolvedCurveName, currency, metadata, value);
            }
          }

        } catch (IllegalArgumentException ex) {
          failures.add(FailureItem.of(
              PARSING, "CSV file could not be parsed at line {}: {}", batchRow.lineNumber(), ex.getMessage()));
        }
      }
      CurveSensitivities sens = builder.build();
      if (!sens.getTypedSensitivities().isEmpty()) {
        parsed.put(sens.getId().map(Object::toString).orElse(""), sens);
      }
    }
  }

  //-------------------------------------------------------------------------
  // parses the file in grid format
  private void parseGridFormat(
      CsvIterator csv,
      ListMultimap<String, CurveSensitivities> parsed,
      List<FailureItem> failures) {

    // find the applicable reference columns
    Map<String, CurveName> references = new LinkedHashMap<>();
    for (String header : csv.headers()) {
      String headerLowerCase = header.toLowerCase(Locale.ENGLISH);
      if (!REF_HEADERS.contains(headerLowerCase) && !resolver.isInfoColumn(headerLowerCase)) {
        references.put(header, CurveName.of(header));
      }
    }

    // loop around all rows, peeking to match batches with the same identifier
    // no exception catch at this level to avoid infinite loops
    while (csv.hasNext()) {
      CsvRow peekedRow = csv.peek();
      PortfolioItemInfo info = parseInfo(peekedRow);
      String id = info.getId().map(StandardId::toString).orElse("");

      // process in batches, where the ID is the same
      CurveSensitivitiesBuilder builder = CurveSensitivities.builder(info);
      List<CsvRow> batchRows = csv.nextBatch(r -> matchId(r, id));
      for (CsvRow batchRow : batchRows) {
        try {
          ParameterMetadata metadata = parseMetadata(batchRow, true);
          CurveSensitivitiesType type = batchRow.findValue(TYPE_HEADER)
              .map(str -> CurveSensitivitiesType.of(str))
              .orElse(CurveSensitivitiesType.ZERO_RATE_DELTA);
          for (Entry<String, CurveName> entry : references.entrySet()) {
            CurveName reference = entry.getValue();
            CurveName resolvedCurveName = resolver.checkCurveName(reference);
            String valueStr = batchRow.getField(entry.getKey());
            Currency currency = parseCurrency(batchRow, reference);
            if (!valueStr.isEmpty()) {
              double value = LoaderUtils.parseDouble(valueStr);
              builder.add(type, resolvedCurveName, currency, metadata, value);
            }
          }

        } catch (IllegalArgumentException ex) {
          failures.add(FailureItem.of(
              PARSING, "CSV file could not be parsed at line {}: {}", batchRow.lineNumber(), ex.getMessage()));
        }
      }
      CurveSensitivities sens = builder.build();
      if (!sens.getTypedSensitivities().isEmpty()) {
        parsed.put(sens.getId().map(Object::toString).orElse(""), sens);
      }
    }
  }

  //-------------------------------------------------------------------------
  // parses the currency as a column or from the reference
  private static Currency parseCurrency(CsvRow row, CurveName reference) {
    Optional<String> currencyStr = row.findValue(CURRENCY_HEADER);
    if (currencyStr.isPresent()) {
      return LoaderUtils.parseCurrency(currencyStr.get());
    }
    String referenceStr = reference.getName().toUpperCase(Locale.ENGLISH);
    try {
      Optional<IborIndex> ibor = IborIndex.extendedEnum().find(referenceStr);
      if (ibor.isPresent()) {
        return ibor.get().getCurrency();
      } else {
        Optional<FloatingRateName> frName = FloatingRateName.extendedEnum().find(referenceStr);
        if (frName.isPresent()) {
          return frName.get().getCurrency();
        } else if (referenceStr.length() == 3) {
          return Currency.of(referenceStr);
        } else if (referenceStr.length() > 3 && referenceStr.charAt(3) == '-' || referenceStr.charAt(3) == '_') {
          return LoaderUtils.parseCurrency(referenceStr.substring(0, 3));
        } else {
          // drop out to exception
        }
      }
    } catch (RuntimeException ex) {
      // drop out to exception
    }
    throw new IllegalArgumentException("Unable to parse currency from reference, consider adding a 'Currency' column");
  }

  // parses the currency as a column or from the reference
  private ParameterMetadata parseMetadata(CsvRow row, boolean lenientDateParsing) {
    // parse the tenor and date fields
    Optional<Tenor> tenorOpt = row.findValue(TENOR_HEADER).flatMap(LoaderUtils::tryParseTenor);
    Optional<LocalDate> dateOpt = row.findValue(DATE_HEADER).map(LoaderUtils::parseDate);
    Optional<String> tenorStrOpt = row.findValue(TENOR_HEADER);
    if (tenorStrOpt.isPresent() && !tenorOpt.isPresent()) {
      if (lenientDateParsing && !dateOpt.isPresent() && !resolver.isTenorRequired()) {
        try {
          dateOpt = tenorStrOpt.map(LoaderUtils::parseDate);
        } catch (RuntimeException ex2) {
          // hide this exception, as this is a historic format
          throw new IllegalArgumentException(Messages.format(
              "Invalid tenor '{}', must be expressed as nD, nW, nM or nY", tenorStrOpt.get()));
        }
      } else {
        throw new IllegalArgumentException(Messages.format(
            "Invalid tenor '{}', must be expressed as nD, nW, nM or nY", tenorStrOpt.get()));
      }
    }
    // build correct metadata based on the parsed fields
    if (tenorOpt.isPresent()) {
      Tenor tenor = resolver.checkSensitivityTenor(tenorOpt.get());
      if (dateOpt.isPresent()) {
        return TenorDateParameterMetadata.of(dateOpt.get(), tenor);
      } else {
        return TenorParameterMetadata.of(tenor);
      }
    } else if (resolver.isTenorRequired()) {
      throw new IllegalArgumentException(Messages.format("Missing value for '{}' column", TENOR_HEADER));
    } else if (dateOpt.isPresent()) {
      return LabelDateParameterMetadata.of(dateOpt.get(), dateOpt.get().toString());
    } else {
      throw new IllegalArgumentException(Messages.format(
          "Unable to parse tenor or date, check '{}' and '{}' columns", TENOR_HEADER, DATE_HEADER));
    }
  }

  //-------------------------------------------------------------------------
  // parse the sensitivity info
  private PortfolioItemInfo parseInfo(CsvRow row) {
    PortfolioItemInfo info = PortfolioItemInfo.empty();
    String scheme = row.findValue(ID_SCHEME_HEADER).orElse(DEFAULT_SCHEME);
    StandardId id = row.findValue(ID_HEADER).map(str -> StandardId.of(scheme, str)).orElse(null);
    if (id != null) {
      info = info.withId(id);
    }
    return resolver.parseSensitivityInfo(row, info);
  }

  // checks if the identifier in the row matches the previous one
  private static boolean matchId(CsvRow row, String id) {
    String scheme = row.findValue(ID_SCHEME_HEADER).orElse(DEFAULT_SCHEME);
    String rowId = row.findValue(ID_HEADER).map(str -> StandardId.of(scheme, str).toString()).orElse("");
    return id.equals(rowId);
  }

}
