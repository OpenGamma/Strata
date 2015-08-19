/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Stream;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;

/**
 * A locator for a resource, specified as a file or classpath resource.
 * <p>
 * An instance of this class provides access to a resource, such as a configuration file.
 * The resource data is accessed using Guava {@link CharSource} or {@link ByteSource}.
 */
public final class ResourceLocator {

  /**
   * The prefix for classpath resource locators.
   */
  public static final String CLASSPATH_URL_PREFIX = "classpath:";
  /**
   * The prefix for file resource locators.
   */
  public static final String FILE_URL_PREFIX = "file:";

  /**
   * The resource locator.
   */
  private final String locator;
  /**
   * The source.
   */
  private final ByteSource source;

  //-------------------------------------------------------------------------
  /**
   * Creates a resource from a string locator.
   * <p>
   * This accepts locators starting with 'classpath:' or 'file:'.
   * It also accepts unprefixed locators, treated as 'file:'.
   * 
   * @param locator  the string form of the resource locator
   * @return the resource locator
   */
  @FromString
  public static ResourceLocator of(String locator) {
    ArgChecker.notNull(locator, "locator");
    try {
      if (locator.startsWith(CLASSPATH_URL_PREFIX)) {
        String urlStr = locator.substring(CLASSPATH_URL_PREFIX.length());
        return ofClasspathUrl(Resources.getResource(urlStr));

      } else if (locator.startsWith(FILE_URL_PREFIX)) {
        String fileStr = locator.substring(FILE_URL_PREFIX.length());
        return ofFile(new File(fileStr));

      } else {
        return ofFile(new File(locator));
      }
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Invalid resource locator: " + locator, ex);
    }
  }

  /**
   * Creates a resource from a {@code File}.
   * 
   * @param file  the file to wrap
   * @return the resource
   */
  public static ResourceLocator ofFile(File file) {
    ArgChecker.notNull(file, "file");
    String filename = file.toString();
    // convert Windows separators to unix
    filename = (File.separatorChar == '\\' ? filename.replace('\\', '/') : filename);
    return new ResourceLocator(FILE_URL_PREFIX + filename, Files.asByteSource(file));
  }

  /**
   * Creates a resource from a {@code URL}.
   * 
   * @param url  the URL to wrap
   * @return the resource
   */
  public static ResourceLocator ofClasspathUrl(URL url) {
    ArgChecker.notNull(url, "url");
    String locator = CLASSPATH_URL_PREFIX + url.toString();
    return new ResourceLocator(locator, Resources.asByteSource(url));
  }

  /**
   * Creates a stream of resource locators.
   * <p>
   * This finds all occurrences of the resource name in the classpath and returns them.
   * 
   * @param classpathResourceName  the classpath resource name
   * @return the resource locators
   * @throws UncheckedIOException if an IO exception occurs
   */
  @FromString
  public static Stream<ResourceLocator> streamOfClasspathResources(String classpathResourceName) {
    ArgChecker.notNull(classpathResourceName, "classpathResourceName");
    return Unchecked.wrap(() -> stream(classpathResourceName));
  }

  // break method out to avoid Eclipse compiler issues
  private static Stream<ResourceLocator> stream(String classpathResourceName) throws IOException {
    ClassLoader classLoader = firstNonNull(
        Thread.currentThread().getContextClassLoader(),
        ResourceLocator.class.getClassLoader());
    return Collections.list(classLoader.getResources(classpathResourceName)).stream()
        .map(url -> ResourceLocator.ofClasspathUrl(url));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance of the locator.
   * 
   * @param locator  the locator
   * @param source  the byte source
   */
  private ResourceLocator(String locator, ByteSource source) {
    super();
    this.locator = locator;
    this.source = source;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the string form of the locator.
   * <p>
   * The string form of the locator describes the location of the resource.
   * 
   * @return the locator string
   */
  public String getLocator() {
    return locator;
  }

  /**
   * Gets the byte source to access the resource.
   * <p>
   * A byte source is a supplier of data.
   * The source itself is neither opened nor closed.
   * 
   * @return the byte source
   */
  public ByteSource getByteSource() {
    return source;
  }

  /**
   * Gets the char source to access the resource using UTF-8.
   * <p>
   * A char source is a supplier of data.
   * The source itself is neither opened nor closed.
   * 
   * @return the char source
   */
  public CharSource getCharSource() {
    return getCharSource(StandardCharsets.UTF_8);
  }

  /**
   * Gets the char source to access the resource specifying the character set.
   * <p>
   * A char source is a supplier of data.
   * The source itself is neither opened nor closed.
   * 
   * @param charset  the character set to use
   * @return the char source
   */
  public CharSource getCharSource(Charset charset) {
    return source.asCharSource(charset);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this locator equals another locator.
   * <p>
   * The comparison checks the locator string.
   * 
   * @param obj  the other locator, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof ResourceLocator) {
      return locator.equals(((ResourceLocator) obj).locator);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the locator.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return locator.hashCode();
  }

  /**
   * Returns a string describing the locator.
   * <p>
   * This can be parsed using {@link #of(String)}.
   * 
   * @return the descriptive string
   */
  @ToString
  @Override
  public String toString() {
    return locator;
  }

}
