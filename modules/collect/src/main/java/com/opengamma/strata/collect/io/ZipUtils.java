/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.common.io.ByteStreams;

/**
 * Utility class to simplify accessing and creating zip files, and other packed formats.
 */
public final class ZipUtils {

  private ZipUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a zip file from the list of files in memory.
   * <p>
   * Each source must have a file name.
   * The output will not contain subfolders.
   * 
   * @param sources the byte sources to zip
   * @return the zip file
   */
  public static ArrayByteSource zipInMemory(List<? extends BeanByteSource> sources) {
    long instantMillis = System.currentTimeMillis();
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (ZipOutputStream out = new ZipOutputStream(baos)) {
        for (BeanByteSource source : sources) {
          ZipEntry entry = new ZipEntry(
              source.getFileName()
                  .orElseThrow(() -> new IllegalArgumentException("ByteSource must have a name in order to be zipped")));
          entry.setTime(instantMillis);
          out.putNextEntry(entry);
          source.copyTo(out);
          out.closeEntry();
        }
      }
      return ArrayByteSource.ofUnsafe(baos.toByteArray());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Unpacks the source into memory, invoking the consumer for each entry.
   * <p>
   * Unpacking handles ZIP, GZ and BASE64 formats based entirely on the suffix of the input file name.
   * If the input suffix is not recognized as a packed format, the consumer is invoked with the original file.
   * 
   * @param source the byte source to unpack
   * @param consumer the consumer, which is passed the relative path name and content for each entry
   */
  public static void unpackInMemory(BeanByteSource source, BiConsumer<String, ArrayByteSource> consumer) {
    String fileName = source.getFileName().orElse("");
    if (suffixMatches(fileName, ".zip")) {
      unzip(source, consumer);

    } else if (suffixMatches(fileName, ".gz")) {
      ungz(source, fileName, consumer);

    } else if (suffixMatches(fileName, ".base64")) {
      try (InputStream in = Base64.getDecoder().wrap(source.openBufferedStream())) {
        String shortFileName = fileName.substring(0, fileName.length() - 7);
        ArrayByteSource unbase64 = ArrayByteSource.from(in).withFileName(shortFileName);
        consumer.accept(shortFileName, unbase64);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }

    } else {
      consumer.accept(fileName, source.load());
    }
  }

  // unzips the contents
  private static void unzip(BeanByteSource source, BiConsumer<String, ArrayByteSource> consumer) {
    try (ZipInputStream in = new ZipInputStream(source.openStream())) {
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {
        if (!entry.isDirectory()) {
          ArrayByteSource entrySource = ArrayByteSource.copyOf(ByteStreams.toByteArray(in)).withFileName(entry.getName());
          consumer.accept(entry.getName(), entrySource);
        }
        in.closeEntry();
        entry = in.getNextEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // unzips the contents
  private static void ungz(BeanByteSource source, String fileName, BiConsumer<String, ArrayByteSource> consumer) {
    try (GZIPInputStream in = new GZIPInputStream(source.openStream())) {
      String shortFileName = fileName.substring(0, fileName.length() - 3);
      ArrayByteSource entrySource = ArrayByteSource.copyOf(ByteStreams.toByteArray(in)).withFileName(shortFileName);
      consumer.accept(shortFileName, entrySource);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // safely extract the suffix
  private static boolean suffixMatches(String name, String suffix) {
    return name.regionMatches(true, name.length() - suffix.length(), suffix, 0, suffix.length());
  }

}
