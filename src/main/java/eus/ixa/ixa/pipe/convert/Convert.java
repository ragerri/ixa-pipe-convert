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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import eus.ixa.ixa.pipe.ml.tok.RuleBasedSegmenter;
import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import ixa.kaflib.WF;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.parser.Parse;
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
   * Process the ancora constituent XML annotation into Penn Treebank bracketing
   * style.
   * 
   * @param inXML
   *          the ancora xml constituent document
   * @return the ancora trees in penn treebank one line format
   * @throws IOException
   *           if io exception
   */
  public static void ancora2treebank(Path inXML) throws IOException {
    String filteredTrees = null;
    if (Files.isRegularFile(inXML)) {
      Path outfile = Paths.get(inXML.toString() + ".th");
      System.err.println(">> Wrote XML ancora file to Penn Treebank in "
          + outfile);
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      SAXParser saxParser;
      try {
        saxParser = saxParserFactory.newSAXParser();
        AncoraTreebank ancoraParser = new AncoraTreebank();
        saxParser.parse(inXML.toFile(), ancoraParser);
        String trees = ancoraParser.getTrees();
        // remove empty trees created by "missing" and "elliptic" attributes
        filteredTrees = trees.replaceAll("\\(\\SN\\)", "");
        // format correctly closing brackets
        filteredTrees = filteredTrees.replace(") )", "))");
        // remove double spaces
        filteredTrees = filteredTrees.replaceAll("  ", " ");
        // remove empty sentences created by <sentence title="yes"> elements
        filteredTrees = filteredTrees.replaceAll("\\(SENTENCE \\)\n", "");
        Files.write(outfile, filteredTrees.getBytes(StandardCharsets.UTF_8));
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      } catch (SAXException e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("Please choose a valid file as input");
    }
  }

  /**
   * Calls the ancora2treebank function to generate Penn Treebank trees from
   * Ancora XML constituent parsing.
   * 
   * @param dir
   *          the directory containing the documents
   * @throws IOException
   *           if io problems
   */
  public static void processAncoraConstituentXMLCorpus(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir)) {
      ancora2treebank(dir);
    } else {
      // recursively process directories
        try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
          for (Path file : filesDir) {
            if (Files.isDirectory(file)) {
              processAncoraConstituentXMLCorpus(file);
            } else {
              ancora2treebank(file);
            }
          }
        }
    }
  }

  /**
   * Takes a file containing Penn Treebank oneline annotation and creates
   * tokenized sentences saving it to a file with the *.tok extension.
   * 
   * @param treebankFile
   *          the input file
   * @throws IOException
   */
  public static void treebank2tokens(Path treebankFile) throws IOException {
    // process one file
    if (Files.isRegularFile(treebankFile)) {
      List<String> inputTrees = Files.readAllLines(treebankFile, StandardCharsets.UTF_8);
      Path outfile = Files.createFile(Paths.get(treebankFile.toString() + ".tok"));
      String outFile = getTokensFromTree(inputTrees);
      Files.write(outfile, outFile.getBytes(StandardCharsets.UTF_8));
      System.err.println(">> Wrote tokens to " + outfile);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }

  /**
   * Reads a list of Parse trees and calls {@code getTokens} to create tokenized
   * oneline text.
   * 
   * @param inputTrees
   *          the list of trees in penn treebank format
   * @return the tokenized document the document tokens
   */
  private static String getTokensFromTree(List<String> inputTrees) {

    StringBuilder parsedDoc = new StringBuilder();
    for (String parseSent : inputTrees) {
      Parse parse = Parse.parseParse(parseSent);
      StringBuilder sentBuilder = new StringBuilder();
      getTokens(parse, sentBuilder);
      parsedDoc.append(sentBuilder.toString()).append("\n");
    }
    return parsedDoc.toString();
  }

  /**
   * It converts a penn treebank constituent tree into tokens oneline form.
   * 
   * @param parse
   *          the parse tree
   * @param sb
   *          the stringbuilder to add the trees
   */
  private static void getTokens(Parse parse, StringBuilder sb) {
    if (parse.isPosTag()) {
      if (!parse.getType().equals("-NONE-")) {
        sb.append(parse.getCoveredText()).append(" ");
      }
    } else {
      Parse children[] = parse.getChildren();
      for (int i = 0; i < children.length; i++) {
        getTokens(children[i], sb);
      }
    }
  }

  /**
   * Takes a file containing Penn Treebank oneline annotation and creates
   * Word_POS sentences for POS tagger training, saving it to a file with the
   * *.pos extension.
   * 
   * @param treebankFile
   *          the input file
   * @throws IOException
   */
  public static void treebank2WordPos(Path treebankFile) throws IOException {
    // process one file
    if (Files.isRegularFile(treebankFile)) {
      List<String> inputTrees = Files.readAllLines(treebankFile);
      Path outfile = Paths.get(treebankFile.toString() + ".pos");
      String outFile = getPreTerminals(inputTrees);
      Files.write(outfile, outFile.getBytes());
      System.err.println(">> Wrote Apache OpenNLP POS training format to "
          + outfile);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }

  /**
   * Reads a list of Parse trees and calls {@code getWordType} to create POS
   * training data in Word_POS form
   * 
   * @param inputTrees
   * @return the document with Word_POS sentences
   */
  private static String getPreTerminals(List<String> inputTrees) {

    StringBuilder parsedDoc = new StringBuilder();
    for (String parseSent : inputTrees) {
      Parse parse = Parse.parseParse(parseSent);
      StringBuilder sentBuilder = new StringBuilder();
      getWordType(parse, sentBuilder);
      parsedDoc.append(sentBuilder.toString()).append("\n");
    }
    return parsedDoc.toString();
  }

  /**
   * It converts a penn treebank constituent tree into Word_POS form
   * 
   * @param parse
   * @param sb
   */
  private static void getWordType(Parse parse, StringBuilder sb) {
    if (parse.isPosTag()) {
      if (!parse.getType().equals("-NONE-")) {
        sb.append(parse.getCoveredText()).append("_").append(parse.getType())
            .append(" ");
      }
    } else {
      Parse children[] = parse.getChildren();
      for (int i = 0; i < children.length; i++) {
        getWordType(children[i], sb);
      }
    }
  }

  /**
   * It normalizes a oneline Penn treebank style tree removing trace nodes
   * (-NONE-) and pruning the empty trees created by removing the trace nodes.
   * 
   * @param treebankFile
   * @throws IOException
   */
  public static void getCleanPennTrees(Path treebankFile) throws IOException {
    if (Files.isRegularFile(treebankFile)) {
      List<String> inputTrees = Files.readAllLines(treebankFile, StandardCharsets.UTF_8);
      Path outfile = Files.createFile(Paths.get(treebankFile.toString() + ".treeN"));
      String outFile = normalizeParse(inputTrees);
      Files.write(outfile, outFile.getBytes(StandardCharsets.UTF_8));
      System.err.println(">> Wrote normalized parse to " + outfile);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }

  /**
   * It takes as input a semi-pruned penn treebank tree (e.g., with -NONE-
   * traces removed) via sed 's/-NONE-\s[\*A-Za-z0-9]*[\*]*[\-]*[A-Za-z0-9]*'
   * 
   * and prunes the empty trees remaining from the sed operation. The parseParse
   * function also removes function tags by default.
   * 
   * @param inputTrees
   * @return
   */
  // TODO add the sed regexp to this function
  private static String normalizeParse(List<String> inputTrees) {
    StringBuilder parsedDoc = new StringBuilder();
    for (String parseSent : inputTrees) {
      Parse parse = Parse.parseParse(parseSent);
      Parse.pruneParse(parse);
      StringBuffer sentBuilder = new StringBuffer();
      parse.show(sentBuilder);
      parsedDoc.append(sentBuilder.toString()).append("\n");
    }
    return parsedDoc.toString();
  }

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
      Path outfile = Files.createFile(Paths.get(inFile.toString() + ".tok.naf"));
      kaf = KAFDocument.createFromFile(inFile.toFile());
      //kaf.removeLayer(Layer.entities); kaf.removeLayer(Layer.constituency);
      //kaf.removeLayer(Layer.coreferences); kaf.removeLayer(Layer.chunks);
      //kaf.removeLayer(Layer.deps);
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
  public static void createMonosemicDictionary(Path lemmaDict) throws IOException {
    // process one file
    if (Files.isRegularFile(lemmaDict)) {
      List<String> inputLines = Files.readAllLines(lemmaDict, StandardCharsets.UTF_8);
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
      List<String> inputLines = Files.readAllLines(lemmaDict, StandardCharsets.UTF_8);
      Path outFile = Files.createFile(Paths.get(lemmaDict.toString() + ".xml"));
      POSDictionary posTagDict = getPOSTaggerDict(inputLines);
      OutputStream outputStream = Files.newOutputStream(outFile);
      posTagDict.serialize(outputStream);
      outputStream.close();
      System.err
          .println(">> Serialized Apache OpenNLP POSDictionary format to "
              + outFile);
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
      System.err
          .println(">> Serialized Apache OpenNLP POSDictionary format to "
              + outFile);
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
  private static void addPOSTaggerDict(List<String> inputLines, POSDictionary tagDict) {
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

  public static void nafToCoNLL2002(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir)) {
      KAFDocument kaf = KAFDocument.createFromFile(dir.toFile());
      Path outfile = Files.createFile(Paths.get(dir.toString() + ".conll02"));
      String outKAF = nafToCoNLLConvert2002(kaf);
      Files.write(outfile, outKAF.getBytes(StandardCharsets.UTF_8));
      System.err.println(">> Wrote CoNLL document to " + outfile);
    } else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            nafToCoNLL2002(file);
          } else {
            Path outfile = Files.createFile(Paths.get(file.toString() + ".conll02"));
            KAFDocument kaf = KAFDocument.createFromFile(file.toFile());
            String outKAF = nafToCoNLLConvert2002(kaf);
            Files.write(outfile, outKAF.getBytes(StandardCharsets.UTF_8));
            System.err.println(">> Wrote CoNLL02 document to " + outfile);
          }
        }
      }
    }
  }

  /**
   * Output Conll2002 format.
   * 
   * @param kaf
   *          the kaf document
   * @return the annotated named entities in conll02 format
   */
  public static String nafToCoNLLConvert2002(KAFDocument kaf) {
    List<Entity> namedEntityList = kaf.getEntities();
    Map<String, Integer> entityToSpanSize = new HashMap<String, Integer>();
    Map<String, String> entityToType = new HashMap<String, String>();
    for (Entity ne : namedEntityList) {
      List<ixa.kaflib.Span<Term>> entitySpanList = ne.getSpans();
      for (ixa.kaflib.Span<Term> spanTerm : entitySpanList) {
        Term neTerm = spanTerm.getFirstTarget();
        entityToSpanSize.put(neTerm.getId(), spanTerm.size());
        entityToType.put(neTerm.getId(), ne.getType());
      }
    }

    List<List<WF>> sentences = kaf.getSentences();
    StringBuilder sb = new StringBuilder();
    for (List<WF> sentence : sentences) {
      int sentNumber = sentence.get(0).getSent();
      List<Term> sentenceTerms = kaf.getSentenceTerms(sentNumber);

      for (int i = 0; i < sentenceTerms.size(); i++) {
        Term thisTerm = sentenceTerms.get(i);
        //System.err.println("--> thisterm: "  + thisTerm.getForm() + " " + thisTerm.getId());

        if (entityToSpanSize.get(thisTerm.getId()) != null) {
          int neSpanSize = entityToSpanSize.get(thisTerm.getId());
          //System.err.println("--> neSpanSize:  " + neSpanSize);
          String neClass = entityToType.get(thisTerm.getId());
          //String neType = convertToConLLTypes(neClass);
          if (neSpanSize > 1) {
            for (int j = 0; j < neSpanSize; j++) {
              thisTerm = sentenceTerms.get(i + j);
              sb.append(thisTerm.getForm());
              sb.append("\t");
              sb.append(thisTerm.getLemma());
              sb.append("\t");
              sb.append(thisTerm.getMorphofeat());
              sb.append("\t");
              if (j == 0) {
                sb.append(BIO.BEGIN.toString());
              } else {
                sb.append(BIO.IN.toString());
              }
              sb.append(neClass);
              sb.append("\n");
            }
          } else {
            sb.append(thisTerm.getForm());
            sb.append("\t");
            sb.append(thisTerm.getLemma());
            sb.append("\t");
            sb.append(thisTerm.getMorphofeat());
            sb.append("\t");
            sb.append(BIO.BEGIN.toString());
            sb.append(neClass);
            sb.append("\n");
          }
          i += neSpanSize - 1;
        } else {
          sb.append(thisTerm.getForm());
          sb.append("\t");
          sb.append(thisTerm.getLemma());
          sb.append("\t");
          sb.append(thisTerm.getMorphofeat());
          sb.append("\t");
          sb.append(BIO.OUT);
          sb.append("\n");
        }
      }
      sb.append("\n");// end of sentence
    }
    return sb.toString();
  }

  public static void nafToCoNLL2003(Path dir) throws IOException {
 // process one file
    if (Files.isRegularFile(dir)) {
      KAFDocument kaf = KAFDocument.createFromFile(dir.toFile());
      Path outfile = Files.createFile(Paths.get(dir.toString() + ".conll03"));
      String outKAF = nafToCoNLLConvert2003(kaf);
      Files.write(outfile, outKAF.getBytes(StandardCharsets.UTF_8));
      System.err.println(">> Wrote CoNLL document to " + outfile);
    } else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            nafToCoNLL2003(file);
          } else {
            Path outfile = Files.createFile(Paths.get(file.toString() + ".conll02"));
            KAFDocument kaf = KAFDocument.createFromFile(file.toFile());
            String outKAF = nafToCoNLLConvert2003(kaf);
            Files.write(outfile, outKAF.getBytes());
            System.err.println(">> Wrote CoNLL03 document to " + outfile);
          }
        }
      }
    }
  }

  /**
   * Output Conll2003 format.
   * 
   * @param kaf
   *          the kaf document
   * @return the annotated named entities in conll03 format
   */
  public static String nafToCoNLLConvert2003(KAFDocument kaf) {
    List<Entity> namedEntityList = kaf.getEntities();
    Map<String, Integer> entityToSpanSize = new HashMap<String, Integer>();
    Map<String, String> entityToType = new HashMap<String, String>();
    for (Entity ne : namedEntityList) {
      List<ixa.kaflib.Span<Term>> entitySpanList = ne.getSpans();
      for (ixa.kaflib.Span<Term> spanTerm : entitySpanList) {
        Term neTerm = spanTerm.getFirstTarget();
        // create map from term Id to Entity span size
        entityToSpanSize.put(neTerm.getId(), spanTerm.size());
        // create map from term Id to Entity type
        entityToType.put(neTerm.getId(), ne.getType());
      }
    }

    List<List<WF>> sentences = kaf.getSentences();
    StringBuilder sb = new StringBuilder();
    for (List<WF> sentence : sentences) {
      int sentNumber = sentence.get(0).getSent();
      List<Term> sentenceTerms = kaf.getSentenceTerms(sentNumber);
      boolean previousIsEntity = false;
      String previousType = null;

      for (int i = 0; i < sentenceTerms.size(); i++) {
        Term thisTerm = sentenceTerms.get(i);
        // if term is inside an entity span then annotate B-I entities
        if (entityToSpanSize.get(thisTerm.getId()) != null) {
          int neSpanSize = entityToSpanSize.get(thisTerm.getId());
          String neClass = entityToType.get(thisTerm.getId());
          String neType = Convert.convertToConLLTypes(neClass);
          // if Entity span is multi token
          if (neSpanSize > 1) {
            for (int j = 0; j < neSpanSize; j++) {
              thisTerm = sentenceTerms.get(i + j);
              sb.append(thisTerm.getForm());
              sb.append("\t");
              sb.append(thisTerm.getLemma());
              sb.append("\t");
              sb.append(thisTerm.getMorphofeat());
              sb.append("\t");
              if (j == 0 && previousIsEntity
                  && previousType.equalsIgnoreCase(neType)) {
                sb.append(BIO.BEGIN.toString());
              } else {
                sb.append(BIO.IN.toString());
              }
              sb.append(neType);
              sb.append("\n");
            }
            previousType = neType;
          } else {
            sb.append(thisTerm.getForm());
            sb.append("\t");
            sb.append(thisTerm.getLemma());
            sb.append("\t");
            sb.append(thisTerm.getMorphofeat());
            sb.append("\t");
            if (previousIsEntity && previousType.equalsIgnoreCase(neType)) {
              sb.append(BIO.BEGIN.toString());
            } else {
              sb.append(BIO.IN.toString());
            }
            sb.append(neType);
            sb.append("\n");
          }
          previousIsEntity = true;
          previousType = neType;
          i += neSpanSize - 1;
        } else {
          sb.append(thisTerm.getForm());
          sb.append("\t");
          sb.append(thisTerm.getLemma());
          sb.append("\t");
          sb.append(thisTerm.getMorphofeat());
          sb.append("\t");
          sb.append(BIO.OUT);
          sb.append("\n");
          previousIsEntity = false;
          previousType = BIO.OUT.toString();
        }
      }
      sb.append("\n");// end of sentence
    }
    return sb.toString();
  }
  
  
  public static void trivagoAspectsToCoNLL02(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir)) {
      KAFDocument kaf = KAFDocument.createFromFile(dir.toFile());
      System.err.println(">> Processing " + dir.toString());
      Path outfile = Paths.get(dir.toString() + ".conll02");
      String outKAF = trivagoAspectsToCoNLLConvert02(kaf);
      Files.write(outfile, outKAF.getBytes());
      System.err.println(">> Wrote CoNLL document to " + outfile);
    } else {
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            trivagoAspectsToCoNLL02(file);
          } else {
            Path outfile = Files.createFile(Paths.get(file.toString() + ".conll02"));
            KAFDocument kaf = KAFDocument.createFromFile(file.toFile());
            String outKAF = trivagoAspectsToCoNLLConvert02(kaf);
            Files.write(outfile, outKAF.getBytes());
            System.err.println(">> Wrote CoNLL2002 to " + outfile);
          }
        }
      }
    }
  }

  /**
   * Output Conll2002 format.
   * 
   * @param kaf
   *          the kaf document
   * @return the annotated named entities in conll02 format
   */
  public static String trivagoAspectsToCoNLLConvert02(KAFDocument kaf) {
    List<Entity> namedEntityList = kaf.getEntities();
    Map<String, Integer> entityToSpanSize = new HashMap<String, Integer>();
    Map<String, String> entityToType = new HashMap<String, String>();
    for (Entity ne : namedEntityList) {
      List<ixa.kaflib.Span<Term>> entitySpanList = ne.getSpans();
      for (ixa.kaflib.Span<Term> spanTerm : entitySpanList) {
        List<Term> neTerms = spanTerm.getTargets();
        for (Term neTerm: neTerms) {
          if (!entityToSpanSize.containsKey(neTerm.getId())) {
            entityToSpanSize.put(neTerm.getId(), spanTerm.size());
            entityToType.put(neTerm.getId(), ne.getType());
          } else {
            //TODO this is not ideal, but these overlappings spans here are a mess
            break;
          }
        }
      }
    }

    List<List<WF>> sentences = kaf.getSentences();
    StringBuilder sb = new StringBuilder();
    for (List<WF> sentence : sentences) {
      int sentNumber = sentence.get(0).getSent();
      List<Term> sentenceTerms = kaf.getSentenceTerms(sentNumber);

      for (int i = 0; i < sentenceTerms.size(); i++) {
        Term thisTerm = sentenceTerms.get(i);

        if (entityToSpanSize.get(thisTerm.getId()) != null) {
          int neSpanSize = entityToSpanSize.get(thisTerm.getId());
          String neClass = entityToType.get(thisTerm.getId());
          if (neSpanSize > 1) {
            for (int j = 0; j < neSpanSize; j++) {
              thisTerm = sentenceTerms.get(i + j);
              sb.append(thisTerm.getForm());
              sb.append("\t");
              sb.append(thisTerm.getLemma());
              sb.append("\t");
              sb.append(thisTerm.getMorphofeat());
              sb.append("\t");
              if (j == 0) {
                sb.append(BIO.BEGIN.toString());
              } else {
                sb.append(BIO.IN.toString());
              }
              sb.append(neClass);
              sb.append("\n");
            }
          } else {
            sb.append(thisTerm.getForm());
            sb.append("\t");
            sb.append(thisTerm.getLemma());
            sb.append("\t");
            sb.append(thisTerm.getMorphofeat());
            sb.append("\t");
            sb.append(BIO.BEGIN.toString());
            sb.append(neClass);
            sb.append("\n");
          }
          i += neSpanSize - 1;
        } else {
          sb.append(thisTerm.getForm());
          sb.append("\t");
          sb.append(thisTerm.getLemma());
          sb.append("\t");
          sb.append(thisTerm.getMorphofeat());
          sb.append("\t");
          sb.append(BIO.OUT);
          sb.append("\n");
        }
      }
      sb.append("\n");// end of sentence
    }
    return sb.toString();
  }

  /**
   * Convert Entity class annotation to CoNLL formats.
   * 
   * @param neType
   *          named entity class
   * @return the converted string
   */
  public static String convertToConLLTypes(String neType) {
    String conllType = null;
    if (neType.startsWith("PER") || neType.startsWith("ORG")
        || neType.startsWith("LOC") || neType.startsWith("GPE") || neType.length() == 3) {
      conllType = neType.substring(0, 3);
    } else {
      conllType = neType;
    }
    return conllType;
  }

  /**
   * Enumeration class for CoNLL 2003 BIO format
   */
  private static enum BIO {
    BEGIN("B-"), IN("I-"), OUT("O");
    String tag;

    BIO(String tag) {
      this.tag = tag;
    }

    public String toString() {
      return this.tag;
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
    BufferedReader breader = new BufferedReader(new InputStreamReader(
        inputStream, Charset.forName("UTF-8")));
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
    Files.write(outfile, precleantext.toString().getBytes(StandardCharsets.UTF_8));
    System.err.println(">> Wrote clean document to " + outfile);
    breader.close();
  }
  
  /**
   * Takes a text file and put the contents in a NAF document.
   * It creates the WF elements.
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
          //wf.setSent(noSents);
        }
      }
    }
  }

}
