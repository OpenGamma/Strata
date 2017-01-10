/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.collect.io.XmlFile;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.product.Trade;

/**
 * Loader of trade data in FpML format.
 * <p>
 * This handles the subset of FpML necessary to populate the trade model.
 * The standard parsers accept FpML v5.8, which is often the same as earlier versions.
 * <p>
 * The trade parsers implement {@link FpmlParserPlugin} and are pluggable using
 * the {@code FpmlParserPlugin.ini} configuration file.
 */
public final class FpmlDocumentParser {
  // Notes: Streaming trades directly from the file is difficult due to the
  // need to parse the party element at the root, which is after the trades

  /**
   * The lookup of trade parsers.
   */
  static final ExtendedEnum<FpmlParserPlugin> ENUM_LOOKUP = ExtendedEnum.of(FpmlParserPlugin.class);

  /**
   * The selector used to find "our" party within the set of parties in the FpML document.
   */
  private final FpmlPartySelector ourPartySelector;
  /**
   * The trade info parser.
   */
  private final FpmlTradeInfoParserPlugin tradeInfoParser;
  /**
   * The trade parsers.
   */
  private final Map<String, FpmlParserPlugin> tradeParsers;
  /**
   * The reference data.
   */
  private final ReferenceData refData;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of the parser, based on the specified selector.
   * <p>
   * The FpML parser has a number of plugin points that can be controlled:
   * <ul>
   * <li>the {@linkplain FpmlPartySelector party selector}
   * <li>the {@linkplain FpmlTradeInfoParserPlugin trade info parser}
   * <li>the {@linkplain FpmlParserPlugin trade parsers}
   * <li>the {@linkplain ReferenceData reference data}
   * </ul>
   * This method uses the {@linkplain FpmlTradeInfoParserPlugin#standard() standard}
   * trade info parser, the trade parsers registered in {@link FpmlParserPlugin}
   * configuration and the {@linkplain ReferenceData#standard() standard} reference data.
   * 
   * @param ourPartySelector  the selector used to find "our" party within the set of parties in the FpML document
   * @return the document parser
   */
  public static FpmlDocumentParser of(FpmlPartySelector ourPartySelector) {
    return of(ourPartySelector, FpmlTradeInfoParserPlugin.standard());
  }

  /**
   * Obtains an instance of the parser, based on the specified selector and trade info plugin.
   * <p>
   * The FpML parser has a number of plugin points that can be controlled:
   * <ul>
   * <li>the {@linkplain FpmlPartySelector party selector}
   * <li>the {@linkplain FpmlTradeInfoParserPlugin trade info parser}
   * <li>the {@linkplain FpmlParserPlugin trade parsers}
   * <li>the {@linkplain ReferenceData reference data}
   * </ul>
   * This method uses the trade parsers registered in {@link FpmlParserPlugin} configuration
   * and the {@linkplain ReferenceData#standard() standard} reference data.
   * 
   * @param ourPartySelector  the selector used to find "our" party within the set of parties in the FpML document
   * @param tradeInfoParser  the trade info parser
   * @return the document parser
   */
  public static FpmlDocumentParser of(
      FpmlPartySelector ourPartySelector,
      FpmlTradeInfoParserPlugin tradeInfoParser) {

    return of(ourPartySelector, tradeInfoParser, FpmlParserPlugin.extendedEnum().lookupAllNormalized());
  }

  /**
   * Obtains an instance of the parser, based on the specified selector and plugins.
   * <p>
   * The FpML parser has a number of plugin points that can be controlled:
   * <ul>
   * <li>the {@linkplain FpmlPartySelector party selector}
   * <li>the {@linkplain FpmlTradeInfoParserPlugin trade info parser}
   * <li>the {@linkplain FpmlParserPlugin trade parsers}
   * <li>the {@linkplain ReferenceData reference data}
   * </ul>
   * This method uses the {@linkplain ReferenceData#standard() standard} reference data.
   * 
   * @param ourPartySelector  the selector used to find "our" party within the set of parties in the FpML document
   * @param tradeInfoParser  the trade info parser
   * @param tradeParsers  the map of trade parsers, keyed by the FpML element name
   * @return the document parser
   */
  public static FpmlDocumentParser of(
      FpmlPartySelector ourPartySelector,
      FpmlTradeInfoParserPlugin tradeInfoParser,
      Map<String, FpmlParserPlugin> tradeParsers) {

    return of(ourPartySelector, tradeInfoParser, tradeParsers, ReferenceData.standard());
  }

  /**
   * Obtains an instance of the parser, based on the specified selector and plugins.
   * <p>
   * The FpML parser has a number of plugin points that can be controlled:
   * <ul>
   * <li>the {@linkplain FpmlPartySelector party selector}
   * <li>the {@linkplain FpmlTradeInfoParserPlugin trade info parser}
   * <li>the {@linkplain FpmlParserPlugin trade parsers}
   * <li>the {@linkplain ReferenceData reference data}
   * </ul>
   * 
   * @param ourPartySelector  the selector used to find "our" party within the set of parties in the FpML document
   * @param tradeInfoParser  the trade info parser
   * @param tradeParsers  the map of trade parsers, keyed by the FpML element name
   * @param refData  the reference data to use
   * @return the document parser
   */
  public static FpmlDocumentParser of(
      FpmlPartySelector ourPartySelector,
      FpmlTradeInfoParserPlugin tradeInfoParser,
      Map<String, FpmlParserPlugin> tradeParsers,
      ReferenceData refData) {

    return new FpmlDocumentParser(ourPartySelector, tradeInfoParser, tradeParsers, refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, based on the specified element.
   * 
   * @param ourPartySelector  the selector used to find "our" party within the set of parties in the FpML document
   * @param tradeInfoParser  the trade info parser
   * @param tradeParsers  the map of trade parsers, keyed by the FpML element name
   */
  private FpmlDocumentParser(
      FpmlPartySelector ourPartySelector,
      FpmlTradeInfoParserPlugin tradeInfoParser,
      Map<String, FpmlParserPlugin> tradeParsers,
      ReferenceData refData) {

    this.ourPartySelector = ourPartySelector;
    this.tradeInfoParser = tradeInfoParser;
    this.tradeParsers = tradeParsers;
    this.refData = refData;
  }

  //-------------------------------------------------------------------------
  /**
   * Parses FpML from the specified source, extracting the trades.
   * <p>
   * This parses the specified byte source which must be an XML document.
   * <p>
   * Sometimes, the FpML document is embedded in a non-FpML wrapper.
   * This method will intelligently find the FpML document at the root or within one or two levels
   * of wrapper by searching for an element that contains both {@code <trade>} and {@code <party>}.
   * 
   * @param source  the source of the FpML XML document
   * @return the parsed trades
   * @throws RuntimeException if a parse error occurred
   */
  public List<Trade> parseTrades(ByteSource source) {
    XmlFile xmlFile = XmlFile.of(source, FpmlDocument.ID);
    XmlElement root = findFpmlRoot(xmlFile.getRoot());
    return parseTrades(root, xmlFile.getReferences());
  }

  // intelligently finds the FpML root element
  private static XmlElement findFpmlRoot(XmlElement root) {
    XmlElement fpmlRoot = getFpmlRoot(root);
    if (fpmlRoot != null) {
      return fpmlRoot;
    }
    // try children of root element
    for (XmlElement el : root.getChildren()) {
      fpmlRoot = getFpmlRoot(el);
      if (fpmlRoot != null) {
        return fpmlRoot;
      }
    }
    // try grandchildren of root element
    for (XmlElement el1 : root.getChildren()) {
      for (XmlElement el2 : el1.getChildren()) {
        fpmlRoot = getFpmlRoot(el2);
        if (fpmlRoot != null) {
          return fpmlRoot;
        }
      }
    }
    throw new FpmlParseException("Unable to find FpML root element");
  }

  // simple check to see if this is an FpML root
  private static XmlElement getFpmlRoot(XmlElement el) {
    if (el.getChildren("party").size() > 0) {
      // party and trade are siblings (the common case)
      if (el.getChildren("trade").size() > 0) {
        return el;
      }
      // trade is within a child alongside party (the unusual case, within clearingStatus/clearingStatusItem)
      for (XmlElement child : el.getChildren()) {
        if (child.getChildren("trade").size() > 0) {
          List<XmlElement> fakeChildren = new ArrayList<>();
          fakeChildren.addAll(el.getChildren("party"));
          fakeChildren.addAll(child.getChildren("trade"));
          XmlElement fakeRoot = XmlElement.ofChildren(el.getName(), el.getAttributes(), fakeChildren);
          return fakeRoot;
        }
      }
      // trade is within a grandchild alongside party (the unusual case, within clearingConfirmed/clearing/cleared)
      for (XmlElement child : el.getChildren()) {
        for (XmlElement grandchild : child.getChildren()) {
          if (grandchild.getChildren("trade").size() > 0) {
            List<XmlElement> fakeChildren = new ArrayList<>();
            fakeChildren.addAll(el.getChildren("party"));
            fakeChildren.addAll(grandchild.getChildren("trade"));
            XmlElement fakeRoot = XmlElement.ofChildren(el.getName(), el.getAttributes(), fakeChildren);
            return fakeRoot;
          }
        }
      }
    }
    return null;
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the FpML document extracting the trades.
   * <p>
   * This parses the specified FpML root element, using the map of references.
   * The FpML specification uses references to link one part of the XML to another.
   * For example, if one part of the XML has {@code <foo id="fooId">}, the references
   * map will contain an entry mapping "fooId" to the parsed element {@code <foo>}.
   * 
   * @param fpmlRootEl  the source of the FpML XML document
   * @param references  the map of id/href to referenced element
   * @return the parsed trades
   * @throws RuntimeException if a parse error occurred
   */
  public List<Trade> parseTrades(
      XmlElement fpmlRootEl,
      Map<String, XmlElement> references) {

    FpmlDocument document = new FpmlDocument(fpmlRootEl, references, ourPartySelector, tradeInfoParser, refData);
    List<XmlElement> tradeEls = document.getFpmlRoot().getChildren("trade");
    ImmutableList.Builder<Trade> builder = ImmutableList.builder();
    for (XmlElement tradeEl : tradeEls) {
      builder.add(parseTrade(document, tradeEl));
    }
    return builder.build();
  }

  // parses one trade element
  private Trade parseTrade(FpmlDocument document, XmlElement tradeEl) {
    // find which trade type it is by comparing children to known parsers
    for (Entry<String, FpmlParserPlugin> entry : tradeParsers.entrySet()) {
      Optional<XmlElement> productOptEl = tradeEl.findChild(entry.getKey());
      if (productOptEl.isPresent()) {
        return entry.getValue().parseTrade(document, tradeEl);
      }
    }
    // failed to find a known trade type
    ImmutableSet<String> childNames = tradeEl.getChildren().stream().map(XmlElement::getName).collect(toImmutableSet());
    throw new FpmlParseException("Unknown product type: " + childNames);
  }

}
