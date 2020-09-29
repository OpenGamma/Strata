/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Guavate;

/**
 * A locator for a resource, specified as a file, URL, path or classpath resource.
 * <p>
 * An instance of this class provides access to a resource, such as a configuration file.
 * The resource data is accessed using {@link CharSource} or {@link ByteSource}.
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
   * The prefix for URL resource locators.
   */
  public static final String URL_PREFIX = "url:";

  /**
   * The resource locator.
   */
  private final String locator;
  /**
   * The source.
   */
  private final BeanByteSource source;

  //-------------------------------------------------------------------------
  /**
   * Creates a resource from a string locator.
   * <p>
   * This accepts locators starting with 'classpath:', 'url:' or 'file:'.
   * It also accepts unprefixed locators, treated as files.
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
        return ofClasspath(urlStr);

      } else if (locator.startsWith(FILE_URL_PREFIX)) {
        String fileStr = locator.substring(FILE_URL_PREFIX.length());
        return ofFile(new File(fileStr));

      } else if (locator.startsWith(URL_PREFIX)) {
        String pathStr = locator.substring(URL_PREFIX.length());
        return ofUrl(new URL(pathStr));

      } else {
        return ofFile(new File(locator));
      }
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid resource locator: " + locator, ex);
    }
  }

  /**
   * Creates a resource from a {@code File}.
   * <p>
   * Windows separators are converted to UNIX style.
   * This takes no account of possible '/' characters in the name.
   * 
   * @param file  the file to wrap
   * @return the resource locator
   */
  public static ResourceLocator ofFile(File file) {
    ArgChecker.notNull(file, "file");
    String filename = file.toString();
    // convert Windows separators to unix
    filename = (File.separatorChar == '\\' ? filename.replace('\\', '/') : filename);
    return new ResourceLocator(FILE_URL_PREFIX + filename, FileByteSource.of(file));
  }

  /**
   * Creates a resource from a {@code Path}.
   * <p>
   * This will return either a file locator or a URL locator.
   *
   * @param path  path to the file to wrap
   * @return the resource locator
   * @throws IllegalArgumentException if the path is neither a file nor a URL
   */
  public static ResourceLocator ofPath(Path path) {
    ArgChecker.notNull(path, "path");
    try {
      return ofFile(path.toFile());
    } catch (UnsupportedOperationException ex) {
      try {
        return ofUrl(path.toUri().toURL());
      } catch (MalformedURLException ex2) {
        throw new IllegalArgumentException("Path could not be converted to a File or URL: " + path);
      }
    }
  }

  /**
   * Creates a resource from a {@code URL}.
   *
   * @param url  path to the file to wrap
   * @return the resource locator
   */
  public static ResourceLocator ofUrl(URL url) {
    ArgChecker.notNull(url, "url");
    String filename = url.toString();
    return new ResourceLocator(URL_PREFIX + filename, UriByteSource.of(url));
  }

  /**
   * Creates a resource from a fully qualified resource name.
   * <p>
   * If the resource name does not start with a slash '/', one will be prepended.
   * Use {@link #ofClasspath(Class, String)} to get a relative resource.
   * <p>
   * In Java 9 and later, resources can be encapsulated due to the module system.
   * As such, this method is caller sensitive.
   * It finds the class of the method that called this one, and uses that to search for
   * resources using {@link Class#getResource(String)}.
   * 
   * @param resourceName  the resource name, which will have a slash '/' prepended if missing
   * @return the resource locator
   */
  public static ResourceLocator ofClasspath(String resourceName) {
    ArgChecker.notNull(resourceName, "classpathLocator");
    String searchName = resourceName.startsWith("/") ? resourceName : "/" + resourceName;
    Class<?> caller = Guavate.callerClass(3);
    return ofClasspath(caller, searchName);
  }

  /**
   * Creates a resource locator for a classpath resource which is associated with a class.
   * <p>
   * The classpath is searched using the same method as {@code Class.getResource}.
   * <ul>
   *   <li>If the resource name starts with '/' it is treated as an absolute path relative to the classpath root</li>
   *   <li>Otherwise the resource name is treated as a path relative to the package containing the class</li>
   * </ul>
   *
   * @param cls  the class used to find the resource
   * @param resourceName  the resource name
   * @return the resource locator
   */
  public static ResourceLocator ofClasspath(Class<?> cls, String resourceName) {
    ArgChecker.notNull(resourceName, "classpathLocator");
    URL url = cls.getResource(resourceName);
    if (url == null) {
      throw new IllegalArgumentException("Resource not found: " + resourceName);
    }
    return ofClasspathUrl(url);
  }

  /**
   * Creates a resource from a {@code URL}.
   * 
   * @param url  the URL to wrap
   * @return the resource locator
   */
  public static ResourceLocator ofClasspathUrl(URL url) {
    ArgChecker.notNull(url, "url");
    String locator = CLASSPATH_URL_PREFIX + url.toString();
    return new ResourceLocator(locator, UriByteSource.of(url));
  }

  /**
   * Selects a suitable class loader.
   * 
   * @return the class loader
   */
  static ClassLoader classLoader() {
    ClassLoader loader = ResourceConfig.class.getClassLoader();
    if (loader != null) {
      return loader;
    }
    return Thread.currentThread().getContextClassLoader();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance of the locator.
   * 
   * @param locator  the locator
   * @param source  the byte source
   */
  private ResourceLocator(String locator, BeanByteSource source) {
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
  public BeanByteSource getByteSource() {
    return source;
  }

  /**
   * Gets the char source to access the resource using UTF-8.
   * <p>
   * A char source is a supplier of data.
   * The source itself is neither opened nor closed.
   * <p>
   * This method handles Unicode Byte Order Marks.
   * 
   * @return the char source
   */
  public CharSource getCharSource() {
    return UnicodeBom.toCharSource(source);
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
