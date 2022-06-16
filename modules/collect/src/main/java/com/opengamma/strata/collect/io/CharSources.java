/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;

/**
 * Helper that allows {@code CharSource} objects to be created.
 */
public final class CharSources {

  private CharSources() {
  }

  /**
   * Obtains an instance of {@link CharSource} from a file name, specified as a String.
   *
   * @param fileName  the file name, as a String
   * @return a new instance of {@link CharSource} with UTF-8 for charset.
   */
  public static CharSource ofFileName(String fileName) {
    return Files.asCharSource(new File(fileName), Charsets.UTF_8);
  }

  /**
   * Obtains an instance of {@link CharSource} from a file name, specified as a String.
   * This also takes in a specific character set, as a {@link Charset}.
   *
   * @param fileName  the file name, as a String
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource}
   */
  public static CharSource ofFileName(String fileName, Charset charset) {
    return Files.asCharSource(new File(fileName), charset);
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of {@link CharSource} from a file object, specified as a {@link File}.
   *
   * @param file  the file object
   * @return a new instance of {@link CharSource} with UTF-8 for charset.
   */
  public static CharSource ofFile(File file) {
    return Files.asCharSource(file, Charsets.UTF_8);
  }

  /**
   * Obtains an instance of {@link CharSource} from a file object, specified as a {@link File}.
   * This also takes in a specific character set, as a {@link Charset}.
   *
   * @param file  the file object
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource}
   */
  public static CharSource ofFile(File file, Charset charset) {
    return Files.asCharSource(file, charset);
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of {@link CharSource} from a file path, specified as a {@link Path}.
   *
   * @param path  the path to create a {@link CharSource} from
   * @return a new instance of {@link CharSource} with UTF-8 for charset.
   */
  public static CharSource ofPath(Path path) {
    return MoreFiles.asCharSource(path, Charsets.UTF_8);
  }

  /**
   * Obtains an instance of {@link CharSource} from a file path, specified as a {@link Path}.
   * This also takes in a specific character set, as a {@link Charset}.
   *
   * @param path  the path to create a {@link CharSource} from
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource}
   */
  public static CharSource ofPath(Path path, Charset charset) {
    return MoreFiles.asCharSource(path, charset);
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of {@link CharSource} from a URL, specified as a {@link URL} object.
   *
   * @param url  the url to create a {@link CharSource} from
   * @return a new instance of {@link CharSource} with UTF-8 for charset.
   */
  public static CharSource ofUrl(URL url) {
    return Resources.asCharSource(url, Charsets.UTF_8);
  }

  /**
   * Obtains an instance of {@link CharSource} from an URL, specified as a {@link URL} object.
   * This also takes in a specific character set, as a {@link Charset}.
   *
   * @param url  the url to create a {@link CharSource} from
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource}.
   */
  public static CharSource ofUrl(URL url, Charset charset) {
    return Resources.asCharSource(url, charset);
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of {@link CharSource} from a text variable, specified as a {@link String} object.
   *
   * @param content  the text to create a {@link CharSource} for
   * @return a new instance of {@link CharSource}
   */
  public static StringCharSource ofContent(String content) {
    // bridged below for backwards compatibility
    return StringCharSource.of(content);
  }

  /**
   * @hidden
   * @param content  the content
   * @return the source
   */
  public CharSource ofContent$$bridge(String content) { // CSIGNORE
    return ofContent(content);
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of {@link CharSource} from a text variable, specified as a byte array.
   * 
   * @param content  the text to create a {@link CharSource} for
   * @return a new instance of {@link CharSource} with UTF-8 for charset
   */
  public static StringCharSource ofContent(byte[] content) {
    // bridged below for backwards compatibility
    return StringCharSource.fromBytesUtf8(content);
  }

  /**
   * @hidden
   * @param content  the content
   * @return the source
   */
  public CharSource ofContent$$bridge(byte[] content) { // CSIGNORE
    return ofContent(content);
  }

  /**
   * Obtains an instance of {@link CharSource} from a text variable, specified as a byte array.
   * This also takes in a specific character set, as a {@link Charset}.
   * 
   * @param content  the text to create a {@link CharSource} for
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource}
   */
  public static StringCharSource ofContent(byte[] content, Charset charset) {
    // bridged below for backwards compatibility
    return StringCharSource.fromBytes(content, charset);
  }

  /**
   * @hidden
   * @param content  the content
   * @param charset  the charset
   * @return the source
   */
  public CharSource ofContent$$bridge(byte[] content, Charset charset) { // CSIGNORE
    return ofContent(content, charset);
  }

  //-------------------------------------------------------------------------
  /**
   * Extracts the file name from a source.
   *
   * @param source  the source to extract the file name from
   * @return the file name
   */
  public static String extractFileName(CharSource source) {
    if (source instanceof BeanCharSource) {
      return ((BeanCharSource) source).getFileName().orElse("Unknown.txt");
    } else if (source == null) {
      // this is used in error messages, so avoid any chance of NPE hiding the real error
      return "Null.txt";
    } else {
      // handle all other char sources
      String str = source.toString();
      if (str.startsWith("Files.asByteSource(")) {
        // extract the file name from toString()
        int pos = str.indexOf(')', 19);
        return Paths.get(str.substring(19, pos)).getFileName().toString();
      } else if (str.startsWith("MoreFiles.asByteSource(")) {
        // extract the path name from toString()
        int pos = str.indexOf(',', 23);
        return Paths.get(str.substring(23, pos)).getFileName().toString();
      } else if (str.startsWith("Resources.asByteSource(")) {
        // extract the URI from toString()
        int pos = str.indexOf(')', 23);
        String path = str.substring(23, pos);
        int lastSlash = path.lastIndexOf('/');
        return path.substring(lastSlash + 1);
      }
      return "Unknown.txt";
    }
  }

}
