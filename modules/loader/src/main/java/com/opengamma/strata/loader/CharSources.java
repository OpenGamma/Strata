package com.opengamma.strata.loader;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.opengamma.strata.collect.io.ResourceLocator;

/**
 * Helper that allows {@link CharSource} objects to be created.
 */
public final class CharSources {

  /**
   * Obtains an instance of {@link CharSource} from a file name, specified as a String.
   *
   * @param fileName the file name, as a String
   * @return a new instance of {@link CharSource} with the default charset on the current JVM.
   */
  public static CharSource ofFileName(String fileName) {
    return Files.asCharSource(new File(fileName), Charset.defaultCharset());
  }

  /**
   * Obtains an instance of {@link CharSource} from a file name, specified as a String. This also takes in a specific character set, as a {@link Charset}.
   *
   * @param fileName the file name, as a String
   * @param charset the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource}
   */
  public static CharSource ofFileName(String fileName, Charset charset) {
    return Files.asCharSource(new File(fileName), charset);
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of {@link CharSource} from a file object, specified as a {@link File}.
   *
   * @param file the file object
   * @return a new instance of {@link CharSource} with the default charset on the current JVM.
   */
  public static CharSource ofFile(File file) {
    return Files.asCharSource(file, Charset.defaultCharset());
  }

  /**
   * Obtains an instance of {@link CharSource} from a file object, specified as a {@link File}. This also takes in a specific character set, as a {@link Charset}.
   *
   * @param file the file object
   * @param charset the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource}
   */
  public static CharSource ofFile(File file, Charset charset) {
    return Files.asCharSource(file, charset);
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of {@link CharSource} from a file path, specified as a {@link Path}.
   *
   * @param path the path to create a {@link CharSource} from
   * @return a new instance of {@link CharSource} with the default charset on the current JVM.
   */
  public static CharSource ofPath(Path path) {
    return Files.asCharSource(path.toFile(), Charset.defaultCharset());
  }

  /**
   * Obtains an instance of {@link CharSource} from a file path, specified as a {@link Path}. This also takes in a specific character set, as a {@link Charset}.
   *
   * @param path the path to create a {@link CharSource} from
   * @param charset the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource}
   */
  public static CharSource ofPath(Path path, Charset charset) {
    return Files.asCharSource(path.toFile(), charset);
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of {@link CharSource} from a resource such as a file specified as a {@link ResourceLocator}. This also takes in a specific character set, as a {@link Charset}.
   *
   * @param resourceLocator the resource to create a {@link CharSource} for, as an instance of {@link ResourceLocator}
   * @return a new instance of {@link CharSource} with the default charset on the current JVM.
   */
  public static CharSource ofResourceLocator(ResourceLocator resourceLocator) {
    return resourceLocator.getCharSource();
  }

  /**
   * Obtains an instance of {@link CharSource} from a resource such as a file, specified as a {@link ResourceLocator}. This also takes in a specific character set, as a {@link Charset}.
   *
   * @param resourceLocator the resource to create a {@link CharSource} for, as an instance of {@link ResourceLocator}
   * @param charset the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource} with the default charset on the current JVM.
   */
  public static CharSource ofResourceLocator(ResourceLocator resourceLocator, Charset charset) {
    return resourceLocator.getCharSource(charset);
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of {@link CharSource} from a byte source, specified as a {@link ByteSource}. This also takes in a specific character set, as a {@link Charset}.
   *
   * @param byteSource the {@link ByteSource} object to create the {@link CharSource} for
   * @return a new instance of {@link CharSource} with the default charset on the current JVM.
   */
  public static CharSource ofByteSource(ByteSource byteSource) {
    return byteSource.asCharSource(Charset.defaultCharset());
  }

  /**
   * Obtains an instance of {@link CharSource} from a byte source, specified as a {@link ByteSource}. This also takes in a specific character set, as a {@link Charset}.
   * @param byteSource the {@link ByteSource} object to create the {@link CharSource} for
   * @param charset the charset to build the new CharSource based on
   * @return a new instance of {@link CharSource} with the default charset on the current JVM.
   */
  public static CharSource ofByteSource(ByteSource byteSource, Charset charset) {
    return byteSource.asCharSource(charset);
  }
}
