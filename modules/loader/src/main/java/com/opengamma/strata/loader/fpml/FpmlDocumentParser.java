/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.collect.io.XmlFile;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.product.TradeInfo;

/**
 * Loader of trade data in FpML format.
 * <p>
 * This handles the subset of FpML necessary to populate the trade model.
 * The standard parsers accept FpML v5.8, which is often the same as earlier versions.
 * <p>
 * The trade parsers implement {@link FpmlTradeParser} and are pluggable using
 * the {@code FpmlTradeParser.ini} configuration file.
 */
public final class FpmlDocumentParser {
  // Notes: Streaming trades directly from the file is difficult due to the
  // need to parse the party element at the root, which is after the trades

  /**
   * The lookup of trade parsers.
   */
  static final ExtendedEnum<FpmlTradeParser> ENUM_LOOKUP = ExtendedEnum.of(FpmlTradeParser.class);

  /**
   * The FpML document.
   */
  private final FpmlDocument document;

  //-------------------------------------------------------------------------
  /**
   * Parses the first trade from the specified source using the first party as ours.
   * 
   * @param source  the source of the FpML XML document
   * @return the parsed trades
   * @throws RuntimeException if a parse error occurred
   */
  public static List<Trade> parseTrades(ByteSource source) {
    XmlFile xmlFile = XmlFile.of(source, "id");
    FpmlDocumentParser parser = new FpmlDocumentParser(xmlFile.getRoot(), xmlFile.getReferences(), "");
    return parser.parseTrades();
  }

  /**
   * Creates an instance, parsing the specified source.
   * 
   * @param source  the source of the FpML XML document
   * @param ourParty  our party identifier, as stored in {@code <partyId>}
   * @return the parsed trades
   * @throws RuntimeException if a parse error occurred
   */
  public static List<Trade> parseTrades(ByteSource source, String ourParty) {
    XmlFile xmlFile = XmlFile.of(source, "id");
    FpmlDocumentParser parser = new FpmlDocumentParser(xmlFile.getRoot(), xmlFile.getReferences(), ourParty);
    return parser.parseTrades();
  }

  /**
   * Creates an instance, based on the specified element.
   * <p>
   * The map of references is used to link one part of the XML to another.
   * For example, if one part of the XML has {@code <foo id="fooId">}, the references
   * map will contain an entry mapping "fooId" to the parsed element {@code <foo>}.
   * 
   * @param fpmlRootEl  the source of the FpML XML document
   * @param references  the map of id/href to referenced element
   * @param ourParty  our party identifier, as stored in {@code <partyId>}, empty if not applicable
   * @return the parsed trades
   * @throws RuntimeException if a parse error occurred
   */
  public static List<Trade> parseTrades(XmlElement fpmlRootEl, Map<String, XmlElement> references, String ourParty) {
    FpmlDocumentParser parser = new FpmlDocumentParser(fpmlRootEl, references, ourParty);
    return parser.parseTrades();
  }

  /**
   * Creates an instance, based on the specified element.
   * <p>
   * The map of references is used to link one part of the XML to another.
   * For example, if one part of the XML has {@code <foo id="fooId">}, the references
   * map will contain an entry mapping "fooId" to the parsed element {@code <foo>}.
   * 
   * @param fpmlRootEl  the source of the FpML XML document
   * @param references  the map of id/href to referenced element
   * @param ourParty  our party identifier, as stored in {@code <partyId>}
   */
  private FpmlDocumentParser(XmlElement fpmlRootEl, Map<String, XmlElement> references, String ourParty) {
    this.document = new FpmlDocument(fpmlRootEl, references, ourParty);
  }

  //-------------------------------------------------------------------------
  // parses all the trade elements
  private List<Trade> parseTrades() {
    List<XmlElement> tradeEls = document.getFpmlRoot().getChildren("trade");
    ImmutableList.Builder<Trade> builder = ImmutableList.builder();
    for (XmlElement tradeEl : tradeEls) {
      builder.add(parseTrade(tradeEl));
    }
    return builder.build();
  }

  // parses one trade element
  private Trade parseTrade(XmlElement tradeEl) {
    // element 'otherPartyPayment' is ignored
    // tradeHeader
    TradeInfo.Builder tradeInfoBuilder = TradeInfo.builder();
    XmlElement tradeHeaderEl = tradeEl.getChild("tradeHeader");
    tradeInfoBuilder.tradeDate(document.parseDate(tradeHeaderEl.getChild("tradeDate")));
    List<XmlElement> partyTradeIdentifierEls = tradeHeaderEl.getChildren("partyTradeIdentifier");
    for (XmlElement partyTradeIdentifierEl : partyTradeIdentifierEls) {
      String partyReferenceHref = partyTradeIdentifierEl.getChild("partyReference").getAttribute(FpmlDocument.HREF);
      if (partyReferenceHref.equals(document.getOurPartyHrefId())) {
        XmlElement firstTradeIdEl = partyTradeIdentifierEl.getChildren("tradeId").get(0);
        String tradeIdValue = firstTradeIdEl.getContent();
        tradeInfoBuilder.id(StandardId.of("FpML-tradeId", tradeIdValue));  // TODO: tradeIdScheme not used as URI clashes
      }
    }
    for (Entry<String, FpmlTradeParser> entry : ENUM_LOOKUP.lookupAll().entrySet()) {
      Optional<XmlElement> productOptEl = tradeEl.findChild(entry.getKey());
      if (productOptEl.isPresent()) {
        return entry.getValue().parseTrade(tradeEl, document);
      }
    }
    ImmutableSet<String> childNames = tradeEl.getChildren().stream().map(XmlElement::getName).collect(toImmutableSet());
    throw new FpmlParseException("Unknown product type: " + childNames);
  }

}
