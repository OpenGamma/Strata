/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FloatingRateName;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.product.TradeInfo;

/**
 * Provides data about the whole FpML document and parse helper methods.
 * <p>
 * See {@link FpmlDocumentParser} for the main entry point for FpML parsing.
 */
public final class FpmlDocument {
  // FRN definition is dates that are on same numerical day of month
  // Use last business day of month if no matching numerical date (eg. 31st June replaced by last business day of June)
  // Non-business days are replaced by following, or preceding to avoid changing the month
  // If last date was last business day of month, then all subsequent dates are last business day of month
  // While close to ModifiedFollowing, it is unclear is that is correct for BusinessDayConvention
  // FpML also has a 'NotApplicable' option, which probably should map to null in the caller

  /**
   * The 'id' attribute key.
   */
  public static final String ID = "id";
  /**
   * The 'href' attribute key.
   */
  public static final String HREF = "href";

  /**
   * Scheme used for parties that are read from FpML.
   */
  private static final String FPML_PARTY_SCHEME = "FpML-partyId";
  /**
   * The enum group for FpML conversions.
   */
  private static final String ENUM_FPML = "FpML";
  /**
   * The FpML date parser.
   */
  private static final DateTimeFormatter FPML_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd[XXX]");
  /**
   * The map of frequencies, designed to normalize and reduce object creation.
   */
  private static final Map<String, Frequency> FREQUENCY_MAP = ImmutableMap.<String, Frequency>builder()
      .put("1D", Frequency.P12M)
      .put("7D", Frequency.P1W)
      .put("14D", Frequency.P2W)
      .put("28D", Frequency.P4W)
      .put("91D", Frequency.P13W)
      .put("182D", Frequency.P26W)
      .put("364D", Frequency.P52W)
      .put("1W", Frequency.P1W)
      .put("2W", Frequency.P2W)
      .put("4W", Frequency.P4W)
      .put("13W", Frequency.P13W)
      .put("26W", Frequency.P26W)
      .put("52W", Frequency.P52W)
      .put("1M", Frequency.P1M)
      .put("2M", Frequency.P2M)
      .put("3M", Frequency.P3M)
      .put("4M", Frequency.P4M)
      .put("6M", Frequency.P6M)
      .put("12M", Frequency.P12M)
      .put("1Y", Frequency.P12M)
      .build();
  /**
   * The map of index tenors, designed to normalize and reduce object creation.
   */
  private static final Map<String, Tenor> TENOR_MAP = ImmutableMap.<String, Tenor>builder()
      .put("1W", Tenor.TENOR_1W)
      .put("2W", Tenor.TENOR_2W)
      .put("1M", Tenor.TENOR_1M)
      .put("2M", Tenor.TENOR_2M)
      .put("3M", Tenor.TENOR_3M)
      .put("6M", Tenor.TENOR_6M)
      .put("12M", Tenor.TENOR_12M)
      .put("1Y", Tenor.TENOR_12M)
      .build();

  /**
   * The parsed file.
   */
  private final XmlElement fpmlRoot;
  /**
   * The map of references.
   */
  private final ImmutableMap<String, XmlElement> references;
  /**
   * Map of reference id to partyId.
   */
  private final ImmutableListMultimap<String, String> parties;
  /**
   * The party reference id.
   */
  private final String ourPartyHrefId;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, based on the specified element.
   * <p>
   * The map of references is used to link one part of the XML to another.
   * For example, if one part of the XML has {@code <foo id="fooId">}, the references
   * map will contain an entry mapping "fooId" to the parsed element {@code <foo>}.
   * 
   * @param fpmlRootEl  the source of the FpML XML document
   * @param references  the map of id/href to referenced element
   * @param ourParty  our party identifier, as stored in {@code <partyId>}, may be null
   */
  public FpmlDocument(XmlElement fpmlRootEl, Map<String, XmlElement> references, String ourParty) {
    this.fpmlRoot = fpmlRootEl;
    this.references = ImmutableMap.copyOf(references);
    this.parties = parseParties(fpmlRootEl);
    this.ourPartyHrefId = findOurParty(ourParty);
  }

  // parse all the root-level party elements
  private static ImmutableListMultimap<String, String> parseParties(XmlElement root) {
    ListMultimap<String, String> parties = ArrayListMultimap.create();
    for (XmlElement child : root.getChildren("party")) {
      parties.putAll(child.getAttribute(ID), findPartyIds(child));
    }
    return ImmutableListMultimap.copyOf(parties);
  }

  // find the party identifiers
  private static List<String> findPartyIds(XmlElement party) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (XmlElement child : party.getChildren("partyId")) {
      if (child.hasContent()) {
        builder.add(child.getContent());
      }
    }
    return builder.build();
  }

  // locate our party href/id reference
  private String findOurParty(String ourParty) {
    if (ourParty.isEmpty()) {
      return "";
    }
    for (Entry<String, String> entry : parties.entries()) {
      if (ourParty.equals(entry.getValue())) {
        return entry.getKey();
      }
    }
    throw new FpmlParseException(Messages.format(
        "Document does not contain our party ID: {} not found in {}", ourParty, parties));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the FpML root element.
   * <p>
   * This is not necessarily the root of the whole document.
   * 
   * @return the FpML root element
   */
  public XmlElement getFpmlRoot() {
    return fpmlRoot;
  }

  /**
   * Gets the map of href/id references.
   * 
   * @return the reference map
   */
  public ImmutableMap<String, XmlElement> getReferences() {
    return references;
  }

  /**
   * Gets the map of party identifiers keyed by href/id reference.
   * 
   * @return the party map
   */
  public ImmutableListMultimap<String, String> getParties() {
    return parties;
  }

  /**
   * Gets our party href/id reference.
   * <p>
   * This is used to identify the direction of the trade.
   * 
   * @return our party, empty if not known
   */
  public String getOurPartyHrefId() {
    return ourPartyHrefId;
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the trade header element.
   * <p>
   * This parses the trade date and identifier.
   * 
   * @param tradeEl  the trade element
   * @return the trade info builder
   * @throws RuntimeException if unable to parse
   */
  public TradeInfo.Builder parseTradeInfo(XmlElement tradeEl) {
    TradeInfo.Builder tradeInfoBuilder = TradeInfo.builder();
    XmlElement tradeHeaderEl = tradeEl.getChild("tradeHeader");
    tradeInfoBuilder.tradeDate(parseDate(tradeHeaderEl.getChild("tradeDate")));
    List<XmlElement> partyTradeIdentifierEls = tradeHeaderEl.getChildren("partyTradeIdentifier");
    for (XmlElement partyTradeIdentifierEl : partyTradeIdentifierEls) {
      String partyReferenceHref = partyTradeIdentifierEl.getChild("partyReference").getAttribute(HREF);
      if (partyReferenceHref.equals(ourPartyHrefId)) {
        XmlElement firstTradeIdEl = partyTradeIdentifierEl.getChildren("tradeId").get(0);
        String tradeIdValue = firstTradeIdEl.getContent();
        // TODO: tradeIdScheme not used as URI not allowed in StandardId
        tradeInfoBuilder.id(StandardId.of("FpML-tradeId", tradeIdValue));
      }
    }
    return tradeInfoBuilder;
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'BuyerSeller.model' to a {@code BuySell}.
   * <p>
   * The {@link TradeInfo} builder is updated with the counterparty.
   * 
   * @param baseEl  the FpML payer receiver model element
   * @param tradeInfoBuilder  the builder of the trade info
   * @return the pay/receive flag
   * @throws RuntimeException if unable to parse
   */
  public BuySell parseBuyerSeller(XmlElement baseEl, TradeInfo.Builder tradeInfoBuilder) {
    String buyerPartyReference = baseEl.getChild("buyerPartyReference").getAttribute(FpmlDocument.HREF);
    String sellerPartyReference = baseEl.getChild("sellerPartyReference").getAttribute(FpmlDocument.HREF);
    if (ourPartyHrefId.isEmpty() || buyerPartyReference.equals(ourPartyHrefId)) {
      tradeInfoBuilder.counterparty(StandardId.of(FPML_PARTY_SCHEME, parties.get(sellerPartyReference).get(0)));
      return BuySell.BUY;
    } else if (sellerPartyReference.equals(ourPartyHrefId)) {
      tradeInfoBuilder.counterparty(StandardId.of(FPML_PARTY_SCHEME, parties.get(buyerPartyReference).get(0)));
      return BuySell.SELL;
    } else {
      throw new FpmlParseException(Messages.format(
          "Neither buyerPartyReference nor sellerPartyReference contain our party ID: {}", ourPartyHrefId));
    }
  }

  /**
   * Converts an FpML 'PayerReceiver.model' to a {@code PayReceive}.
   * <p>
   * The {@link TradeInfo} builder is updated with the counterparty.
   * 
   * @param baseEl  the FpML payer receiver model element
   * @param tradeInfoBuilder  the builder of the trade info
   * @return the pay/receive flag
   * @throws RuntimeException if unable to parse
   */
  public PayReceive parsePayerReceiver(XmlElement baseEl, TradeInfo.Builder tradeInfoBuilder) {
    String payerPartyReference = baseEl.getChild("payerPartyReference").getAttribute(HREF);
    String receiverPartyReference = baseEl.getChild("receiverPartyReference").getAttribute(HREF);
    Object currentCounterparty = tradeInfoBuilder.get(TradeInfo.meta().counterparty());
    // determine direction and setup counterparty
    if ((ourPartyHrefId.isEmpty() && currentCounterparty == null) || payerPartyReference.equals(ourPartyHrefId)) {
      StandardId proposedCounterparty = StandardId.of(FPML_PARTY_SCHEME, parties.get(receiverPartyReference).get(0));
      if (currentCounterparty == null) {
        tradeInfoBuilder.counterparty(proposedCounterparty);
      } else if (!currentCounterparty.equals(proposedCounterparty)) {
        throw new FpmlParseException(Messages.format(
            "Two different counterparties found: {} and {}", currentCounterparty, proposedCounterparty));
      }
      return PayReceive.PAY;

    } else if (ourPartyHrefId.isEmpty() || receiverPartyReference.equals(ourPartyHrefId)) {
      StandardId proposedCounterparty = StandardId.of(FPML_PARTY_SCHEME, parties.get(payerPartyReference).get(0));
      if (currentCounterparty == null) {
        tradeInfoBuilder.counterparty(proposedCounterparty);
      } else if (!currentCounterparty.equals(proposedCounterparty)) {
        throw new FpmlParseException(Messages.format(
            "Two different counterparties found: {} and {}", currentCounterparty, proposedCounterparty));
      }
      return PayReceive.RECEIVE;

    } else {
      throw new FpmlParseException(Messages.format(
          "Neither payerPartyReference nor receiverPartyReference contain our party ID: {}", ourPartyHrefId));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'AdjustedRelativeDateOffset' to a resolved {@code LocalDate}.
   * 
   * @param baseEl  the FpML adjustable date element
   * @return the resolved date
   * @throws RuntimeException if unable to parse
   */
  public AdjustableDate parseAdjustedRelativeDateOffset(XmlElement baseEl) {
    // FpML content: ('periodMultiplier', 'period', 'dayType?',
    //                'businessDayConvention', 'BusinessCentersOrReference.model?'
    //                'dateRelativeTo', 'adjustedDate', 'relativeDateAdjustments?')
    // The 'adjustedDate' element is ignored
    XmlElement relativeToEl = lookupReference(baseEl.getChild("dateRelativeTo"));
    LocalDate baseDate;
    if (relativeToEl.hasContent()) {
      baseDate = parseDate(relativeToEl);
    } else if (relativeToEl.getName().contains("relative")) {
      baseDate = parseAdjustedRelativeDateOffset(relativeToEl).adjusted();
    } else {
      throw new FpmlParseException(
          "Unable to resolve 'dateRelativeTo' to a date: " + baseEl.getChild("dateRelativeTo").getAttribute(HREF));
    }
    Period period = parsePeriod(baseEl);
    Optional<XmlElement> dayTypeEl = baseEl.findChild("dayType");
    boolean calendarDays = period.isZero() || (dayTypeEl.isPresent() && dayTypeEl.get().getContent().equals("Calendar"));
    BusinessDayAdjustment bda1 = parseBusinessDayAdjustments(baseEl);
    BusinessDayAdjustment bda2 = baseEl.findChild("relativeDateAdjustments")
        .map(el -> parseBusinessDayAdjustments(el))
        .orElse(bda1);
    // interpret and resolve, simple calendar arithmetic or business days
    LocalDate resolvedDate;
    if (period.getYears() > 0 || period.getMonths() > 0 || calendarDays) {
      resolvedDate = bda2.adjust(bda1.adjust(baseDate.plus(period)));
    } else {
      resolvedDate = bda2.adjust(bda1.adjust(bda1.getCalendar().shift(baseDate, period.getDays())));
    }
    return AdjustableDate.of(resolvedDate, bda2);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'RelativeDateOffset' to a {@code DaysAdjustment}.
   * 
   * @param baseEl  the FpML adjustable date element
   * @return the days adjustment
   * @throws RuntimeException if unable to parse
   */
  public DaysAdjustment parseRelativeDateOffsetDays(XmlElement baseEl) {
    // FpML content: ('periodMultiplier', 'period', 'dayType?',
    //                'businessDayConvention', 'BusinessCentersOrReference.model?'
    //                'dateRelativeTo', 'adjustedDate')
    // The 'dateRelativeTo' element is not used here
    // The 'adjustedDate' element is ignored
    Period period = parsePeriod(baseEl);
    if (period.toTotalMonths() != 0) {
      throw new FpmlParseException("Expected days-based period but found " + period);
    }
    Optional<XmlElement> dayTypeEl = baseEl.findChild("dayType");
    boolean calendarDays = period.isZero() || (dayTypeEl.isPresent() && dayTypeEl.get().getContent().equals("Calendar"));
    BusinessDayConvention fixingBdc = convertBusinessDayConvention(
        baseEl.getChild("businessDayConvention").getContent());
    HolidayCalendar calendar = parseBusinessCenters(baseEl);
    if (calendarDays) {
      return DaysAdjustment.ofCalendarDays(
          period.getDays(), BusinessDayAdjustment.of(fixingBdc, calendar));
    } else {
      return DaysAdjustment.ofBusinessDays(period.getDays(), calendar);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'AdjustableDate' to an {@code AdjustableDate}.
   * 
   * @param baseEl  the FpML adjustable date element
   * @return the adjustable date
   * @throws RuntimeException if unable to parse
   */
  public AdjustableDate parseAdjustableDate(XmlElement baseEl) {
    // FpML content: ('unadjustedDate', 'dateAdjustments', 'adjustedDate?')
    Optional<XmlElement> unadjOptEl = baseEl.findChild("unadjustedDate");
    if (unadjOptEl.isPresent()) {
      LocalDate unadjustedDate = parseDate(unadjOptEl.get());
      BusinessDayAdjustment adjustment = parseBusinessDayAdjustments(baseEl.getChild("dateAdjustments"));
      return AdjustableDate.of(unadjustedDate, adjustment);
    }
    LocalDate adjustedDate = parseDate(baseEl.getChild("adjustedDate"));
    return AdjustableDate.of(adjustedDate);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'BusinessDayAdjustments' to a {@code BusinessDayAdjustment}.
   * 
   * @param baseEl  the FpML business centers or reference element to parse 
   * @return the business day adjustment
   * @throws RuntimeException if unable to parse
   */
  public BusinessDayAdjustment parseBusinessDayAdjustments(XmlElement baseEl) {
    // FpML content: ('businessDayConvention', 'BusinessCentersOrReference.model?')
    BusinessDayConvention bdc = convertBusinessDayConvention(
        baseEl.getChild("businessDayConvention").getContent());
    Optional<XmlElement> centersEl = baseEl.findChild("businessCenters");
    Optional<XmlElement> centersRefEl = baseEl.findChild("businessCentersReference");
    HolidayCalendar calendar = (centersEl.isPresent() || centersRefEl.isPresent() ? parseBusinessCenters(baseEl) : HolidayCalendars.NO_HOLIDAYS);
    return BusinessDayAdjustment.of(bdc, calendar);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'BusinessCentersOrReference.model' to a {@code HolidayCalendar}.
   * 
   * @param baseEl  the FpML business centers or reference element to parse 
   * @return the holiday calendar
   * @throws RuntimeException if unable to parse
   */
  public HolidayCalendar parseBusinessCenters(XmlElement baseEl) {
    // FpML content: ('businessCentersReference' | 'businessCenters')
    // FpML 'businessCenters' content: ('businessCenter+')
    // Each 'businessCenter' is a location treated as a holiday calendar
    Optional<XmlElement> optionalBusinessCentersEl = baseEl.findChild("businessCenters");
    XmlElement businessCentersEl = optionalBusinessCentersEl.orElseGet(() -> lookupReference(baseEl.getChild("businessCentersReference")));
    HolidayCalendar calendar = HolidayCalendars.NO_HOLIDAYS;
    for (XmlElement businessCenterEl : businessCentersEl.getChildren("businessCenter")) {
      calendar = calendar.combineWith(parseBusinessCenter(businessCenterEl));
    }
    return calendar;
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'BusinessCenter' to a {@code HolidayCalendar}.
   * 
   * @param baseEl  the FpML calendar element to parse 
   * @return the calendar
   * @throws RuntimeException if unable to parse
   */
  public HolidayCalendar parseBusinessCenter(XmlElement baseEl) {
    validateScheme(baseEl, "businessCenterScheme", "http://www.fpml.org/coding-scheme/business-center");
    return convertHolidayCalendar(baseEl.getContent());
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'FloatingRateIndex.model' to an {@code Index}.
   * 
   * @param baseEl  the FpML floating rate index element to parse 
   * @return the index
   * @throws RuntimeException if unable to parse
   */
  public Index parseIndex(XmlElement baseEl) {
    List<Index> indexes = parseIndexes(baseEl);
    if (indexes.size() != 1) {
      throw new FpmlParseException("Expected one index but found " + indexes.size());
    }
    return indexes.get(0);
  }

  /**
   * Converts an FpML 'FloatingRateIndex' with multiple tenors to an {@code Index}.
   * 
   * @param baseEl  the FpML floating rate index element to parse 
   * @return the index
   * @throws RuntimeException if unable to parse
   */
  public List<Index> parseIndexes(XmlElement baseEl) {
    XmlElement indexEl = baseEl.getChild("floatingRateIndex");
    validateScheme(indexEl, "floatingRateIndexScheme", "http://www.fpml.org/coding-scheme/floating-rate-index");
    FloatingRateName floatingName = FloatingRateName.of(indexEl.getContent());
    List<XmlElement> tenorEls = baseEl.getChildren("indexTenor");
    if (tenorEls.isEmpty()) {
      return ImmutableList.of(floatingName.toOvernightIndex());
    } else {
      return tenorEls.stream()
          .map(el -> floatingName.toIborIndex(parseIndexTenor(el)))
          .collect(toImmutableList());
    }
  }

  /**
   * Converts an FpML 'FloatingRateIndex' tenor to a {@code Tenor}.
   * 
   * @param baseEl  the FpML floating rate index element to parse 
   * @return the period
   * @throws RuntimeException if unable to parse
   */
  public Tenor parseIndexTenor(XmlElement baseEl) {
    // FpML content: ('periodMultiplier', 'period')
    String multiplier = baseEl.getChild("periodMultiplier").getContent();
    String unit = baseEl.getChild("period").getContent();
    return convertIndexTenor(multiplier, unit);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'Period' to a {@code Period}.
   * 
   * @param baseEl  the FpML element to parse 
   * @return the period
   * @throws RuntimeException if unable to parse
   */
  public Period parsePeriod(XmlElement baseEl) {
    // FpML content: ('periodMultiplier', 'period')
    String multiplier = baseEl.getChild("periodMultiplier").getContent();
    String unit = baseEl.getChild("period").getContent();
    return Period.parse("P" + multiplier + unit);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML frequency to a {@code Frequency}.
   * 
   * @param baseEl  the FpML element to parse 
   * @return the frequency
   * @throws RuntimeException if unable to parse
   */
  public Frequency parseFrequency(XmlElement baseEl) {
    // FpML content: ('periodMultiplier', 'period')
    String multiplier = baseEl.getChild("periodMultiplier").getContent();
    String unit = baseEl.getChild("period").getContent();
    if (unit.equals("T")) {
      return Frequency.TERM;
    }
    return convertFrequency(multiplier, unit);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'Money' to a {@code CurrencyAmount}.
   * 
   * @param baseEl  the FpML money element to parse 
   * @return the currency amount
   * @throws RuntimeException if unable to parse
   */
  public CurrencyAmount parseCurrencyAmount(XmlElement baseEl) {
    // FpML content: ('currency', 'amount')
    Currency currency = parseCurrency(baseEl.getChild("currency"));
    double amount = parseDecimal(baseEl.getChild("amount"));
    return CurrencyAmount.of(currency, amount);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'Currency' to a {@code Currency}.
   * 
   * @param baseEl  the FpML currency element to parse 
   * @return the currency
   * @throws RuntimeException if unable to parse
   */
  public Currency parseCurrency(XmlElement baseEl) {
    validateScheme(baseEl, "currencyScheme", "http://www.fpml.org/coding-scheme/external/iso4217");
    return Currency.of(baseEl.getContent());
  }

  /**
   * Converts an FpML 'DayCountFraction' to a {@code DayCount}.
   * 
   * @param baseEl  the FpML day count element to parse 
   * @return the day count
   * @throws RuntimeException if unable to parse
   */
  public DayCount parseDayCountFraction(XmlElement baseEl) {
    validateScheme(baseEl, "dayCountFractionScheme", "http://www.fpml.org/coding-scheme/day-count-fraction");
    return convertDayCount(baseEl.getContent());
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML 'decimal' to a {@code double}.
   * 
   * @param baseEl  the FpML element to parse 
   * @return the double
   * @throws RuntimeException if unable to parse
   */
  public double parseDecimal(XmlElement baseEl) {
    return Double.parseDouble(baseEl.getContent());
  }

  /**
   * Converts an FpML 'date' to a {@code LocalDate}.
   * 
   * @param baseEl  the FpML element to parse 
   * @return the date
   * @throws RuntimeException if unable to parse
   */
  public LocalDate parseDate(XmlElement baseEl) {
    return convertDate(baseEl.getContent());
  }

  /**
   * Converts an FpML 'hourMinuteTime' to a {@code LocalTime}.
   * 
   * @param baseEl  the FpML element to parse 
   * @return the time
   * @throws RuntimeException if unable to parse
   */
  public LocalTime parseTime(XmlElement baseEl) {
    return LocalTime.parse(baseEl.getContent());
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an FpML day count string to a {@code DayCount}.
   * 
   * @param fpmlDayCountName  the day count name used by FpML
   * @return the day count
   * @throws IllegalArgumentException if the day count is not known
   */
  public DayCount convertDayCount(String fpmlDayCountName) {
    return DayCount.extendedEnum().externalNames(ENUM_FPML).lookup(fpmlDayCountName);
  }

  /**
   * Converts an FpML business day convention string to a {@code BusinessDayConvention}.
   * 
   * @param fmplBusinessDayConventionName  the business day convention name used by FpML
   * @return the business day convention
   * @throws IllegalArgumentException if the business day convention is not known
   */
  public BusinessDayConvention convertBusinessDayConvention(String fmplBusinessDayConventionName) {
    return BusinessDayConvention.extendedEnum().externalNames(ENUM_FPML).lookup(fmplBusinessDayConventionName);
  }

  /**
   * Converts an FpML roll convention string to a {@code RollConvention}.
   * 
   * @param fmplRollConventionName  the roll convention name used by FpML
   * @return the roll convention
   * @throws IllegalArgumentException if the roll convention is not known
   */
  public RollConvention convertRollConvention(String fmplRollConventionName) {
    return RollConvention.extendedEnum().externalNames(ENUM_FPML).lookup(fmplRollConventionName);
  }

  /**
   * Converts an FpML business center string to a {@code HolidayCalendar}.
   * 
   * @param fpmlBusinessCenter  the business center name used by FpML
   * @return the holiday calendar
   * @throws IllegalArgumentException if the holiday calendar is not known
   */
  public HolidayCalendar convertHolidayCalendar(String fpmlBusinessCenter) {
    return HolidayCalendar.of(fpmlBusinessCenter);
  }

  /**
   * Converts an FpML frequency string to a {@code Frequency}.
   * 
   * @param multiplier  the multiplier
   * @param unit  the unit
   * @return the frequency
   * @throws IllegalArgumentException if the frequency is not known
   */
  public Frequency convertFrequency(String multiplier, String unit) {
    String periodStr = multiplier + unit;
    Frequency frequency = FREQUENCY_MAP.get(periodStr);
    return frequency != null ? frequency : Frequency.parse(periodStr);
  }

  /**
   * Converts an FpML tenor string to a {@code Tenor}.
   * 
   * @param multiplier  the multiplier
   * @param unit  the unit
   * @return the tenor
   * @throws IllegalArgumentException if the tenor is not known
   */
  public Tenor convertIndexTenor(String multiplier, String unit) {
    String periodStr = multiplier + unit;
    Tenor tenor = TENOR_MAP.get(periodStr);
    return tenor != null ? tenor : Tenor.parse(periodStr);
  }

  /**
   * Converts an FpML date to a {@code LocalDate}.
   * 
   * @param dateStr  the business center name used by FpML
   * @return the holiday calendar
   * @throws DateTimeParseException if the date cannot be parsed
   */
  public LocalDate convertDate(String dateStr) {
    return LocalDate.parse(dateStr, FPML_DATE_FORMAT);
  }

  //-------------------------------------------------------------------------
  /**
   * Validates that a specific element is not present.
   * 
   * @param baseEl  the FpML element to parse 
   * @param elementName  the element name
   * @throws FpmlParseException if the element is found
   */
  public void validateNotPresent(XmlElement baseEl, String elementName) {
    if (baseEl.findChild(elementName).isPresent()) {
      throw new FpmlParseException("Unsupported element: '" + elementName + "'");
    }
  }

  /**
   * Validates that the scheme attribute is known.
   * 
   * @param baseEl  the FpML element to parse 
   * @param schemeAttr  the scheme attribute name
   * @param schemeValue  the scheme attribute value
   * @throws FpmlParseException if the scheme does not match
   */
  public void validateScheme(XmlElement baseEl, String schemeAttr, String schemeValue) {
    if (baseEl.getAttributes().containsKey(schemeAttr)) {
      String scheme = baseEl.getAttribute(schemeAttr);
      if (!scheme.startsWith(schemeValue)) {
        throw new FpmlParseException("Unknown '" + schemeAttr + "' attribute value: " + scheme);
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Looks up an element by href/id reference.
   * 
   * @param hrefEl  the element containing the href/id
   * @return the matched element
   * @throws FpmlParseException if the reference is not found
   */
  // lookup an element via href/id reference
  public XmlElement lookupReference(XmlElement hrefEl) {
    String hrefId = hrefEl.getAttribute(HREF);
    XmlElement el = references.get(hrefId);
    if (el == null) {
      throw new FpmlParseException(Messages.format("Document reference not found: href='{}'", hrefId));
    }
    return el;
  }

}
