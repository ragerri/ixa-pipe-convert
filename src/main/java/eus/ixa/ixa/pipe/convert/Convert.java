/*
 * Copyright 2014 Rodrigo Agerri

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

package eus.ixa.ixa.pipe.convert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import eus.ixa.ixa.pipe.ml.tok.RuleBasedSegmenter;
import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.postag.POSDictionary;

/**
 * Convert functions.
 * 
 * @author ragerri
 * @version 2014-10-28
 * 
 */
public class Convert {

  private Convert() {
  }

  public static Pattern detokenizeTargets = Pattern.compile(
      "<\\s+START\\s+:\\s+target\\s+>", Pattern.UNICODE_CHARACTER_CLASS);
  public static Pattern detokenizeEnds = Pattern.compile("<\\s+END\\s+>");

  /**
   * Remove named entity related layers in NAF.
   * 
   * @param dir
   *          the directory containing the documents
   * @throws IOException
   *           if io problems
   */
  public static void removeEntities(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir)) {
      removeEntityLayer(dir);
    } // process one file
    else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            removeEntities(file);
          } else {
            removeEntityLayer(file);
          }
        }
      }
    }
  }

  /**
   * Remove the specified NAF layers.
   * 
   * @param inFile
   *          the NAF document
   * @throws IOException
   *           if io problems
   */
  private static void removeEntityLayer(Path inFile) {
    KAFDocument kaf = null;
    try {
      Path outfile = Files
          .createFile(Paths.get(inFile.toString() + ".tok.naf"));
      kaf = KAFDocument.createFromFile(inFile.toFile());
      // kaf.removeLayer(Layer.entities); kaf.removeLayer(Layer.constituency);
      // kaf.removeLayer(Layer.coreferences); kaf.removeLayer(Layer.chunks);
      // kaf.removeLayer(Layer.deps);
      Files.write(outfile, kaf.toString().getBytes(StandardCharsets.UTF_8));
      System.err
          .println(">> Wrote KAF document without entities to " + outfile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Extract entities that contain a link to an external resource in NAF.
   * 
   * @param dir
   *          the directory containing the NAF documents
   * @throws IOException
   *           if io problems
   */
  public static void getNERFromNAF(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir)) {
      printEntities(dir);
    } else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            getNERFromNAF(file);
          } else {
            printEntities(file);
          }
        }
      }
    }
  }

  /**
   * Print entities that contain an external resource link in NAF.
   * 
   * @param inFile
   *          the NAF document
   * @throws IOException
   *           if io problems
   */
  public static void printEntities(Path inFile) {
    KAFDocument kaf = null;
    try {
      kaf = KAFDocument.createFromFile(inFile.toFile());
    } catch (IOException e) {
      e.printStackTrace();
    }
    List<Entity> entityList = kaf.getEntities();
    for (Entity entity : entityList) {
      System.out.println(entity.getStr() + "\t" + entity.getType());
    }
  }

  /**
   * Extract entities that contain a link to an external resource in NAF.
   * 
   * @param dir
   *          the directory containing the NAF documents
   * @throws IOException
   *           if io problems
   */
  public static void getNEDFromNAF(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir)) {
      printEntities(dir);
    } else {
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            getNEDFromNAF(file);
          }
        }
      }
    }
  }

  /**
   * Print entities that contain an external resource link in NAF.
   * 
   * @param inFile
   *          the NAF document
   * @throws IOException
   *           if io problems
   */
  public static void printNEDEntities(Path inFile) {
    KAFDocument kaf = null;
    try {
      kaf = KAFDocument.createFromFile(inFile.toFile());
    } catch (IOException e) {
      e.printStackTrace();
    }
    List<Entity> entityList = kaf.getEntities();
    for (Entity entity : entityList) {
      if (entity.getExternalRefs().size() > 0)
        System.out.println(entity.getExternalRefs().get(0).getReference());
    }
  }

  /**
   * Convert a lemma dictionary (word lemma postag) into a
   * {@code POSTaggerDictionary}.
   * 
   * @param lemmaDict
   *          the input file
   * @throws IOException
   *           if io problems
   */
  public static void createMonosemicDictionary(Path lemmaDict)
      throws IOException {
    // process one file
    if (Files.isRegularFile(lemmaDict)) {
      List<String> inputLines = Files.readAllLines(lemmaDict,
          StandardCharsets.UTF_8);
      getMonosemicDict(inputLines);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }

  private static void getMonosemicDict(List<String> inputLines) {
    Map<String, String> monosemicMap = new HashMap<String, String>();
    ListMultimap<String, String> dictMultiMap = ArrayListMultimap.create();
    for (String line : inputLines) {
      String[] lineArray = line.split("\t");
      if (lineArray.length == 3) {
        if (!lineArray[0].contains("<")) {
          dictMultiMap.put(lineArray[0], lineArray[2]);
          monosemicMap.put(lineArray[0], lineArray[1] + "\t" + lineArray[2]);
        }
      }
    }
    for (String token : dictMultiMap.keySet()) {
      List<String> tags = dictMultiMap.get(token);
      // add only monosemic words
      if (tags.size() == 1) {
        System.out.println(token + "\t" + monosemicMap.get(token));
      }
    }
  }

  /**
   * Convert a lemma dictionary (word lemma postag) into a
   * {@code POSTaggerDictionary}. It saves the resulting file with the name of
   * the original dictionary changing the extension to .xml.
   * 
   * @param lemmaDict
   *          the input file
   * @throws IOException
   *           if io problems
   */
  public static void convertLemmaToPOSDict(Path lemmaDict) throws IOException {
    // process one file
    if (Files.isRegularFile(lemmaDict)) {
      List<String> inputLines = Files.readAllLines(lemmaDict,
          StandardCharsets.UTF_8);
      Path outFile = Files.createFile(Paths.get(lemmaDict.toString() + ".xml"));
      POSDictionary posTagDict = getPOSTaggerDict(inputLines);
      OutputStream outputStream = Files.newOutputStream(outFile);
      posTagDict.serialize(outputStream);
      outputStream.close();
      System.err.println(
          ">> Serialized Apache OpenNLP POSDictionary format to " + outFile);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }

  /**
   * Generates {@code POSDictionary} from a list of monosemic words and its
   * postag. form\tab\lemma\tabpostag
   * 
   * @param inputLines
   *          the list of words and postag per line
   * @return the POSDictionary
   */
  private static POSDictionary getPOSTaggerDict(List<String> inputLines) {
    POSDictionary posTaggerDict = new POSDictionary();
    ListMultimap<String, String> dictMultiMap = ArrayListMultimap.create();
    for (String line : inputLines) {
      String[] lineArray = line.split("\t");
      if (lineArray.length == 3) {
        if (!lineArray[0].contains("<")) {
          dictMultiMap.put(lineArray[0], lineArray[2]);
        }
      }
    }
    for (String token : dictMultiMap.keySet()) {
      List<String> tags = dictMultiMap.get(token);
      // add only monosemic words
      if (tags.size() == 1) {
        posTaggerDict.put(token, tags.toArray(new String[tags.size()]));
      }
    }
    return posTaggerDict;
  }

  /**
   * Aggregates a lemma dictionary (word lemma postag) into a
   * {@code POSTaggerDictionary}. It saves the resulting file with the name of
   * the original lemma dictionary changing the extension to .xml.
   * 
   * @param lemmaDict
   *          the input file
   * @throws IOException
   *           if io problems
   */
  public static void addLemmaToPOSDict(Path lemmaDict, Path posTaggerDict)
      throws IOException {
    // process one file
    if (Files.isRegularFile(lemmaDict) && Files.isRegularFile(posTaggerDict)) {
      InputStream posDictInputStream = Files.newInputStream(posTaggerDict);
      POSDictionary posDict = POSDictionary.create(posDictInputStream);
      List<String> inputLines = Files.readAllLines(lemmaDict);
      Path outFile = Paths.get(lemmaDict.toString() + ".xml");
      addPOSTaggerDict(inputLines, posDict);
      OutputStream outputStream = Files.newOutputStream(outFile);
      posDict.serialize(outputStream);
      outputStream.close();
      System.err.println(
          ">> Serialized Apache OpenNLP POSDictionary format to " + outFile);
    } else {
      System.out.println("Please choose a valid files as input.");
      System.exit(1);
    }
  }

  /**
   * Aggregates {@code POSDictionary} from a list of words and its postag.
   * 
   * @param inputLines
   *          the list of words and postag per line
   * @param tagDict
   *          the POSDictionary to which the lemma dictionary will be added
   */
  private static void addPOSTaggerDict(List<String> inputLines,
      POSDictionary tagDict) {
    ListMultimap<String, String> dictMultiMap = ArrayListMultimap.create();
    for (String line : inputLines) {
      String[] lineArray = line.split(" ");
      if (lineArray.length == 2) {
        dictMultiMap.put(lineArray[0], lineArray[1]);
      }
    }
    for (String token : dictMultiMap.keySet()) {
      List<String> tags = dictMultiMap.get(token);
      if (tags.size() == 1) {
        tagDict.put(token, tags.toArray(new String[tags.size()]));
      }
    }
  }

  public static void brownClusterClean(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir) && !dir.toString().endsWith(".clean")) {
      brownCleanUpperCase(dir);
    } else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            brownClusterClean(file);
          } else {
            if (!file.toString().endsWith(".clean")) {
              brownCleanUpperCase(file);
            }
          }
        }
      }
    }
  }

  /**
   * Do not print a sentence if is less than 90% lowercase.
   * 
   * @param sentences
   *          the list of sentences
   * @throws IOException
   */
  private static void brownCleanUpperCase(Path inFile) throws IOException {
    StringBuilder precleantext = new StringBuilder();
    InputStream inputStream = CmdLineUtil.openInFile(inFile.toFile());
    BufferedReader breader = new BufferedReader(
        new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      double lowercaseCounter = 0;
      StringBuilder sb = new StringBuilder();
      String[] lineArray = line.split(" ");
      for (String word : lineArray) {
        if (lineArray.length > 0) {
          sb.append(word);
        }
      }
      char[] lineCharArray = sb.toString().toCharArray();
      for (char lineArr : lineCharArray) {
        if (Character.isLowerCase(lineArr)) {
          lowercaseCounter++;
        }
      }
      double percent = lowercaseCounter / (double) lineCharArray.length;
      if (percent >= 0.90) {
        precleantext.append(line).append("\n");
      }
    }
    Path outfile = Files.createFile(Paths.get(inFile.toString() + ".clean"));
    Files.write(outfile,
        precleantext.toString().getBytes(StandardCharsets.UTF_8));
    System.err.println(">> Wrote clean document to " + outfile);
    breader.close();
  }

  /**
   * Takes a text file and put the contents in a NAF document. It creates the WF
   * elements.
   * 
   * @param inputFile
   * @throws IOException
   */
  public static void textToNAF(final Path inputFile) throws IOException {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    int noSents = 0;
    int noParas = 1;
    final List<String> sentences = Files.readAllLines(inputFile);
    for (final String sentence : sentences) {
      noSents = noSents + 1;
      final String[] tokens = sentence.split(" ");
      for (final String token : tokens) {
        if (token.equals(RuleBasedSegmenter.PARAGRAPH)) {
          ++noParas;
          // TODO sentences without end markers;
          // crap rule
          while (noParas > noSents) {
            ++noSents;
          }
        } else {
          // TODO add offset
          final WF wf = kaf.newWF(0, token, noSents);
          wf.setPara(noParas);
          // wf.setSent(noSents);
        }
      }
    }
  }

}
