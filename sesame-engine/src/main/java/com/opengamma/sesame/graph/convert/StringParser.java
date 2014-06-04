/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph.convert;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */ /* package */ class StringParser {

  private StringParser() {
  }

  /* package */ static List<String> parse(String str) {
    List<String> strings = new ArrayList<>();
    StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(str));
    tokenizer.resetSyntax();
    tokenizer.wordChars('a', 'z');
    tokenizer.wordChars('A', 'Z');
    tokenizer.wordChars('-', '-');
    tokenizer.wordChars('0', '9');
    tokenizer.wordChars('.', '.');
    tokenizer.wordChars('~', '~');
    tokenizer.wordChars(128 + 32, 255);
    tokenizer.whitespaceChars(' ', ' ');
    tokenizer.whitespaceChars(',', ',');
    tokenizer.quoteChar('"');

    try {
      while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
        strings.add(tokenizer.sval);
      }
    } catch (IOException e) {
      // not going to happen
    }
    return strings;
  }
}
