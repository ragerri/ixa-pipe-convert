package eus.ixa.ixa.pipe.convert;

/*
 *Copyright 2014 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import eus.ixa.ixa.pipe.ml.StatisticalDocumentClassifier;
import eus.ixa.ixa.pipe.ml.tok.RuleBasedSegmenter;
import eus.ixa.ixa.pipe.ml.tok.RuleBasedTokenizer;
import eus.ixa.ixa.pipe.ml.tok.Token;
import opennlp.tools.util.Span;

/**
 * Pattern matching and other utility string functions.
 *
 * @author ragerri
 * @version 2013-03-19
 */
public final class StringUtils {

  /**
   * Private constructor.
   */
  private StringUtils() {
    throw new AssertionError("This class is not meant to be instantiated!");
  }

  /**
   * Finds a pattern (typically a named entity string) in a tokenized sentence.
   * It outputs the {@link Span} indexes of the named entity found, if any.
   *
   * @param pattern
   *          a string to find
   * @param tokens
   *          an array of tokens
   * @return token spans of the pattern (e.g. a named entity)
   */
  public static List<Integer> exactTokenFinderIgnoreCase(final String pattern,
      final String[] tokens) {
    String[] patternTokens = pattern.split(" ");
    int i, j;
    int patternLength = patternTokens.length;
    int sentenceLength = tokens.length;
    List<Integer> neTokens = new ArrayList<Integer>();
    for (j = 0; j <= sentenceLength - patternLength; ++j) {
      for (i = 0; i < patternLength
          && patternTokens[i].equalsIgnoreCase(tokens[i + j]); ++i)
        ;
      if (i >= patternLength) {
        neTokens.add(j);
        neTokens.add(i + j);
      }
    }
    return neTokens;
  }

  /**
   * Finds a pattern (typically a named entity string) in a tokenized sentence.
   * It outputs the {@link Span} indexes of the named entity found, if any
   *
   * @param pattern
   *          a string to find
   * @param tokens
   *          an array of tokens
   * @return token spans of the pattern (e.g. a named entity)
   */
  public static List<Integer> exactTokenFinder(final String pattern,
      final String[] tokens) {
    String[] patternTokens = pattern.split(" ");
    int i, j;
    int patternLength = patternTokens.length;
    int sentenceLength = tokens.length;
    List<Integer> neTokens = new ArrayList<Integer>();
    for (j = 0; j <= sentenceLength - patternLength; ++j) {
      for (i = 0; i < patternLength
          && patternTokens[i].equals(tokens[i + j]); ++i)
        ;
      if (i >= patternLength) {
        neTokens.add(j);
        neTokens.add(i + j);
      }
    }
    return neTokens;
  }

  /**
   * Finds a pattern (typically a named entity string) in a sentence string. It
   * outputs the offsets for the start and end characters named entity found, if
   * any.
   *
   * @param pattern
   *          the pattern to be searched
   * @param sentence
   *          the sentence
   * @return a list of integers corresponding to the characters of the string
   *         found
   */
  public static List<Integer> exactStringFinder(final String pattern,
      final String sentence) {
    char[] patternArray = pattern.toCharArray(),
        sentenceArray = sentence.toCharArray();
    int i, j;
    int patternLength = patternArray.length;
    int sentenceLength = sentenceArray.length;
    List<Integer> neChars = new ArrayList<Integer>();
    for (j = 0; j <= sentenceLength - patternLength; ++j) {
      for (i = 0; i < patternLength
          && patternArray[i] == sentenceArray[i + j]; ++i)
        ;
      if (i >= patternLength) {
        neChars.add(j);
        neChars.add(i + j);
      }
    }
    return neChars;
  }

  /**
   *
   * It takes a NE span indexes and the tokens in a sentence and produces the
   * string to which the NE span corresponds to. This function is used to get
   * the Named Entity or Name textual representation from a {@link Span}
   *
   * @param reducedSpan
   *          a {@link Span}
   * @param tokens
   *          an array of tokens
   * @return named entity string
   */
  public static String getStringFromSpan(final Span reducedSpan,
      final String[] tokens) {
    StringBuilder sb = new StringBuilder();
    for (int si = reducedSpan.getStart(); si < reducedSpan.getEnd(); si++) {
      sb.append(tokens[si]).append(" ");
    }
    return sb.toString().trim();
  }

  /**
   * Gets the String joined by a space of an array of tokens.
   *
   * @param tokens
   *          an array of tokens representing a tokenized sentence
   * @return sentence the sentence corresponding to the tokens
   */
  public static String getStringFromTokens(final String[] tokens) {
    StringBuilder sb = new StringBuilder();
    for (String tok : tokens) {
      sb.append(tok).append(" ");
    }
    return sb.toString().trim();
  }

  /**
   * Recursively get every file in a directory and add them to a list.
   * 
   * @param inputPath
   *          the input directory
   * @return the list containing all the files
   */
  public static List<File> getFilesInDir(File inputPath) {
    List<File> fileList = new ArrayList<File>();
    for (File aFile : com.google.common.io.Files.fileTreeTraverser().preOrderTraversal(inputPath)) {
      if (aFile.isFile()) {
        fileList.add(aFile);
      }
    }
    return fileList;
  }

  public static List<List<Token>> tokenizeSentence(String sentString,
      String language) {
    RuleBasedTokenizer tokenizer = new RuleBasedTokenizer(sentString,
        setTokenizeProperties(language));
    List<String> sentenceList = new ArrayList<>();
    sentenceList.add(sentString);
    String[] sentences = sentenceList.toArray(new String[sentenceList.size()]);
    List<List<Token>> tokens = tokenizer.tokenize(sentences);
    return tokens;
  }

  /**
   * Tokenize a document given in a one line string.
   * 
   * @param docString
   *          the oneline document string
   * @param language
   *          the language
   * @return the tokenized sentences
   */
  public static List<List<Token>> tokenizeDocument(String docString,
      String language) {
    RuleBasedSegmenter segmenter = new RuleBasedSegmenter(docString,
        setTokenizeProperties(language));
    RuleBasedTokenizer toker = new RuleBasedTokenizer(docString,
        setTokenizeProperties(language));
    String[] sentences = segmenter.segmentSentence();
    List<List<Token>> tokens = toker.tokenize(sentences);
    return tokens;
  }

  public static Properties setTokenizeProperties(String language) {
    Properties annotateProperties = new Properties();
    annotateProperties.setProperty("language", language);
    annotateProperties.setProperty("normalize", "default");
    annotateProperties.setProperty("hardParagraph", "no");
    annotateProperties.setProperty("untokenizable", "no");
    return annotateProperties;
  }
  
  public static void classifyDocuments(Path dir, String model, String language) throws IOException {
    // process one file
    if (Files.isRegularFile(dir)) {
      classifyDocument(dir, model, language);
    } // process one file
    else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            classifyDocuments(dir, model, language);
          } else {
            classifyDocument(file, model, language);
          }
        }
      }
    }
  }
  
  /**
   * Process a text file containing one tokenized sentence per line
   * and provides a document class per line.
   * @param inputFile the file to be processed
   * @param model the model
   * @param language the language
   * @throws IOException if io errors
   */
  public static void classifyDocument(Path inputFile, String model, String language)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    List<String> inputLines = com.google.common.io.Files
        .readLines(new File(inputFile.toString()), Charset.forName("UTF-8"));
    Properties properties = setDocProperties(model, language, "no");
    StatisticalDocumentClassifier docClassifier = new StatisticalDocumentClassifier(
        properties);
    for (String line : inputLines) {
      String[] document = line.split(" ");
      String docClass = docClassifier.classify(document);
      sb.append(docClass + "\t" + line).append("\n");
    }
    Path outfile = Files.createFile(Paths.get(inputFile + ".doc"));
    Files.write(outfile, sb.toString().getBytes(StandardCharsets.UTF_8));
    System.err.println(">> Wrote document classifier document to " + outfile);
  }
  
  private static Properties setDocProperties(String model, String language,
      String clearFeatures) {
    Properties oteProperties = new Properties();
    oteProperties.setProperty("model", model);
    oteProperties.setProperty("language", language);
    oteProperties.setProperty("clearFeatures", clearFeatures);
    return oteProperties;
  }
}
