/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * Outputs an INI formatted file.
 * <p>
 * Provides a simple tool for writing an INI file.
 * <p>
 * Instances of this class contain mutable state.
 * A net instance must be created for each file to be output.
 */
public final class IniFileOutput {

  private static final String NEW_LINE = System.lineSeparator();

  /**
   * The destination to write to.
   */
  private final Appendable underlying;
  /**
   * The new line string.
   */
  private final String newLine;
  /**
   * Whether to include an additional whitespace character either side of the '='
   * character between keys and values.
   * <p>
   * Other parsers can be more strict and require that there is no whitespace
   * between the equals separator.
   */
  private final boolean padSeparatorWithWhitespace;

  /**
   * Creates an instance, using the system default line separator and including
   * additional whitespace around the '=' separator.
   *
   * @param underlying  the destination to write to
   * @return the INI outputter
   */
  public static IniFileOutput standard(Appendable underlying) {
    return new IniFileOutput(underlying, NEW_LINE, true);
  }

  /**
   * Creates an instance, allowing the new line separator to be controlled and including
   * additional whitespace around the '=' separator.
   *
   * @param underlying  the destination to write to
   * @param newLine  the destination to write to
   * @return the INI outputter
   */
  public static IniFileOutput standard(Appendable underlying, String newLine) {
    return new IniFileOutput(underlying, newLine, true);
  }

  /**
   * Creates an instance, using the system default line separator and allowing
   * the padding around the '=' separator to be controlled.
   *
   * @param underlying  the destination to write to
   * @param padSeparatorWithWhitespace  if true, the key-value separator will be padded with whitespace
   * @return the INI outputter
   */
  public static IniFileOutput standard(Appendable underlying, boolean padSeparatorWithWhitespace) {
    return new IniFileOutput(underlying, NEW_LINE, padSeparatorWithWhitespace);
  }

  /**
   * Creates an instance, allowing the new line separator and the padding around
   * the '=' separator to be controlled.
   *
   * @param underlying  the destination to write to
   * @param padSeparatorWithWhitespace  if true, the key-value separator will be padded with whitespace
   * @param newLine  the new line separator
   * @return the INI outputter
   */
  public static IniFileOutput standard(Appendable underlying, boolean padSeparatorWithWhitespace, String newLine) {
    return new IniFileOutput(underlying, newLine, padSeparatorWithWhitespace);
  }

  //-------------------------------------------------------------------------
  // creates an instance
  private IniFileOutput(Appendable underlying, String newLine, boolean padSeparatorWithWhitespace) {
    this.underlying = ArgChecker.notNull(underlying, "underlying");
    this.newLine = ArgChecker.notNull(newLine, "newLine");
    this.padSeparatorWithWhitespace = padSeparatorWithWhitespace;
  }

  //------------------------------------------------------------------------
  /**
   * Writes an individual section of an INI file to the underlying.
   *
   * @param sectionName  the name of the section
   * @param section  the section whose contents to write
   */
  public void writeSection(String sectionName, PropertySet section) {
    ArgChecker.notNull(sectionName, "sectionName");
    ArgChecker.notNull(section, "section");
    String assignment = padSeparatorWithWhitespace ? " = " : "=";

    try {
      underlying.append(formatSectionName(sectionName));
      underlying.append(newLine);
      for (Map.Entry<String, String> entry : section.asMultimap().entries()) {
        underlying.append(entry.getKey());
        underlying.append(assignment);
        underlying.append(entry.getValue());
        underlying.append(newLine);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Writes the provided file to the underlying.
   *
   * @param file  the file whose contents to write
   */
  public void writeIniFile(IniFile file) {
    for (Map.Entry<String, PropertySet> entry : file.asMap().entrySet()) {
      writeSection(entry.getKey(), entry.getValue());
      append(newLine);
    }
  }

  //-------------------------------------------------------------------------
  private void append(CharSequence charSequence) {
    try {
      underlying.append(charSequence);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Formats the section name.
   *
   * @param name  section name
   * @return the formatted name
   */
  private static String formatSectionName(String name) {
    return Messages.format("[{}]", name);
  }
}
