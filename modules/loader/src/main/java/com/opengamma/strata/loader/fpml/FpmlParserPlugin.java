/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.Trade;

/**
 * Pluggable FpML trade parser.
 * <p>
 * Implementations of this interface parse an FpML trade element, including any trade header.
 * The {@link FpmlDocument} instance provides many useful helper methods.
 * <p>
 * See {@link FpmlDocumentParser} for the main entry point for FpML parsing.
 */
public interface FpmlParserPlugin
    extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the parser
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FpmlParserPlugin of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the parser to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<FpmlParserPlugin> extendedEnum() {
    return FpmlDocumentParser.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a single FpML format trade.
   * <p>
   * This parses a trade from the given XML element.
   * Details of the whole document and parser helper methods are provided.
   * <p>
   * It is intended that this method is only called when the specified trade element
   * contains a child element of the correct type for this parser.
   * 
   * @param document  the document-wide information and parser helper
   * @param tradeEl  the trade element to parse
   * @return the trade object
   * @throws RuntimeException if unable to parse
   */
  public abstract Trade parseTrade(FpmlDocument document, XmlElement tradeEl);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this parser.
   * <p>
   * The name must be the name of the product element in FpML that is to be parsed.
   * For example, 'fra', 'swap' or 'fxSingleLeg'.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
