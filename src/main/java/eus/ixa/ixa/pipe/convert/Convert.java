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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;

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
  public String ancora2treebank(File inXML) throws IOException {
    String filteredTrees = null;
    if (inXML.isFile()) {

      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      SAXParser saxParser;
      try {
        saxParser = saxParserFactory.newSAXParser();
        AncoraTreebank ancoraParser = new AncoraTreebank();
        saxParser.parse(inXML, ancoraParser);
        String trees = ancoraParser.getTrees();
        // remove empty trees created by "missing" and "elliptic" attributes
        filteredTrees = trees.replaceAll("\\(\\SN\\)", "");
        // format correctly closing brackets
        filteredTrees = filteredTrees.replace(") )", "))");
        // remove double spaces
        filteredTrees = filteredTrees.replaceAll("  ", " ");
        // remove empty sentences created by <sentence title="yes"> elements
        filteredTrees = filteredTrees.replaceAll("\\(SENTENCE \\)\n", "");
      } catch (ParserConfigurationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (SAXException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      System.out.println("Please choose a valid file as input");
    }
    return filteredTrees;
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
  public void processAncoraConstituentXMLCorpus(File dir) throws IOException {
    // process one file
    if (dir.isFile()) {
      File outfile = new File(Files.getNameWithoutExtension(dir.getPath())
          + ".th");
      String outTree = ancora2treebank(dir);
      Files.write(outTree, outfile, Charsets.UTF_8);
      System.err.println(">> Wrote XML ancora file to Penn Treebank in "
          + outfile);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            processAncoraConstituentXMLCorpus(listFile[i]);
          } else {
            try {
              File outfile = new File(
                  Files.getNameWithoutExtension((listFile[i].getPath()) + ".th"));
              String outTree = ancora2treebank(listFile[i]);
              Files.write(outTree, outfile, Charsets.UTF_8);
              System.err
                  .println(">> Wrote XML Ancora file Penn treebank format in "
                      + outfile);
            } catch (FileNotFoundException noFile) {
              continue;
            }
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
  public void treebank2tokens(File treebankFile) throws IOException {
    // process one file
    if (treebankFile.isFile()) {
      List<String> inputTrees = Files.readLines(
          new File(treebankFile.getCanonicalPath()), Charsets.UTF_8);
      File outfile = new File(Files.getNameWithoutExtension(treebankFile
          .getPath() + ".tok"));
      String outFile = getTokensFromTree(inputTrees);
      Files.write(outFile, outfile, Charsets.UTF_8);
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
  private String getTokensFromTree(List<String> inputTrees) {

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
  private void getTokens(Parse parse, StringBuilder sb) {
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
  public void treebank2WordPos(File treebankFile) throws IOException {
    // process one file
    if (treebankFile.isFile()) {
      List<String> inputTrees = Files.readLines(
          new File(treebankFile.getCanonicalPath()), Charsets.UTF_8);
      File outfile = new File(Files.getNameWithoutExtension(treebankFile
          .getPath()) + ".pos");
      String outFile = getPreTerminals(inputTrees);
      Files.write(outFile, outfile, Charsets.UTF_8);
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
  private String getPreTerminals(List<String> inputTrees) {

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
  private void getWordType(Parse parse, StringBuilder sb) {
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
  public void getCleanPennTrees(File treebankFile) throws IOException {
    if (treebankFile.isFile()) {
      List<String> inputTrees = Files.readLines(
          new File(treebankFile.getCanonicalPath()), Charsets.UTF_8);
      File outfile = new File(Files.getNameWithoutExtension(treebankFile
          .getPath()) + ".treeN");
      String outFile = normalizeParse(inputTrees);
      Files.write(outFile, outfile, Charsets.UTF_8);
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
  private String normalizeParse(List<String> inputTrees) {
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
  public void removeEntities(File dir) throws IOException {
    // process one file
    if (dir.isFile()) {
      File outfile = new File(Files.getNameWithoutExtension(dir.getPath())
          + ".kaf.tok");
      String outKAF = removeEntityLayer(dir);
      Files.write(outKAF, outfile, Charsets.UTF_8);
      System.err
          .println(">> Wrote KAF document without entities to " + outfile);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            removeEntities(listFile[i]);
          } else {
            try {
              File outfile = new File(Files.getNameWithoutExtension(listFile[i]
                  .getPath()) + ".naf");
              String outKAF = removeEntityLayer(listFile[i]);
              Files.write(outKAF, outfile, Charsets.UTF_8);
              System.err.println(">> Wrote KAF document without entities to "
                  + outfile);
            } catch (FileNotFoundException noFile) {
              continue;
            }
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
   * @return the NAF document without the removed layers
   * @throws IOException
   *           if io problems
   */
  private String removeEntityLayer(File inFile) throws IOException {
    KAFDocument kaf = KAFDocument.createFromFile(inFile);
    /*
     * kaf.removeLayer(Layer.entities); kaf.removeLayer(Layer.constituency);
     * kaf.removeLayer(Layer.coreferences); kaf.removeLayer(Layer.chunks);
     * kaf.removeLayer(Layer.deps);
     */
    return kaf.toString();
  }
  
  /**
   * Extract entities that contain a link to an external resource in NAF.
   * 
   * @param dir
   *          the directory containing the NAF documents
   * @throws IOException
   *           if io problems
   */
  public void getNERFromNAF(File dir) throws IOException {
    // process one file
    if (dir.isFile()) {
      printEntities(dir);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            getNERFromNAF(listFile[i]);
          } else {
            try {
              printEntities(listFile[i]);
            } catch (FileNotFoundException noFile) {
              continue;
            }
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
  public void printEntities(File inFile) throws IOException {
    KAFDocument kaf = KAFDocument.createFromFile(inFile);
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
  public void getNEDFromNAF(File dir) throws IOException {
    // process one file
    if (dir.isFile()) {
      printEntities(dir);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            getNEDFromNAF(listFile[i]);
          } else {
            try {
              printNEDEntities(listFile[i]);
            } catch (FileNotFoundException noFile) {
              continue;
            }
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
  public void printNEDEntities(File inFile) throws IOException {
    KAFDocument kaf = KAFDocument.createFromFile(inFile);
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
  public void createMonosemicDictionary(File lemmaDict) throws IOException {
    // process one file
    if (lemmaDict.isFile()) {
      List<String> inputLines = Files.readLines(lemmaDict, Charsets.UTF_8);
      getMonosemicDict(inputLines);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }

  private void getMonosemicDict(List<String> inputLines) {
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
  public void convertLemmaToPOSDict(File lemmaDict) throws IOException {
    // process one file
    if (lemmaDict.isFile()) {
      List<String> inputLines = Files.readLines(lemmaDict, Charsets.UTF_8);
      File outFile = new File(Files.getNameWithoutExtension(lemmaDict
          .getCanonicalPath()) + ".xml");
      POSDictionary posTagDict = getPOSTaggerDict(inputLines);
      OutputStream outputStream = new FileOutputStream(outFile);
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
  private POSDictionary getPOSTaggerDict(List<String> inputLines) {
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
  public void addLemmaToPOSDict(File lemmaDict, File posTaggerDict)
      throws IOException {
    // process one file
    if (lemmaDict.isFile() && posTaggerDict.isFile()) {
      InputStream posDictInputStream = new FileInputStream(posTaggerDict);
      POSDictionary posDict = POSDictionary.create(posDictInputStream);
      List<String> inputLines = Files.readLines(lemmaDict, Charsets.UTF_8);
      File outFile = new File(Files.getNameWithoutExtension(lemmaDict
          .getCanonicalPath()) + ".xml");
      addPOSTaggerDict(inputLines, posDict);
      OutputStream outputStream = new FileOutputStream(outFile);
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
  private void addPOSTaggerDict(List<String> inputLines, POSDictionary tagDict) {
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

  public void nafToCoNLL02(File dir) throws IOException {
    // process one file
    if (dir.isFile()) {
      KAFDocument kaf = KAFDocument.createFromFile(dir);
      File outfile = new File(dir.getCanonicalFile() + ".conll02");
      String outKAF = nafToCoNLLConvert02(kaf);
      Files.write(outKAF, outfile, Charsets.UTF_8);
      System.err.println(">> Wrote CoNLL document to " + outfile);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            nafToCoNLL02(listFile[i]);
          } else {
            try {
              File outfile = new File(listFile[i].getCanonicalFile()
                  + ".conll02");
              KAFDocument kaf = KAFDocument.createFromFile(listFile[i]);
              String outKAF = nafToCoNLLConvert02(kaf);
              Files.write(outKAF, outfile, Charsets.UTF_8);
              System.err.println(">> Wrote CoNLL02 document to " + outfile);
            } catch (FileNotFoundException noFile) {
              continue;
            }
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
  public String nafToCoNLLConvert02(KAFDocument kaf) {
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

        if (entityToSpanSize.get(thisTerm.getId()) != null) {
          int neSpanSize = entityToSpanSize.get(thisTerm.getId());
          String neClass = entityToType.get(thisTerm.getId());
          String neType = convertToConLLTypes(neClass);
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
              sb.append(neType);
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
            sb.append(neType);
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

  public void nafToCoNLL03(File dir) throws IOException {
    // process one file
    if (dir.isFile()) {
      KAFDocument kaf = KAFDocument.createFromFile(dir);
      File outfile = new File(dir.getCanonicalFile() + ".conll03");
      String outKAF = nafToCoNLLConvert03(kaf);
      Files.write(outKAF, outfile, Charsets.UTF_8);
      System.err.println(">> Wrote CoNLL document to " + outfile);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            nafToCoNLL03(listFile[i]);
          } else {
            try {
              File outfile = new File(listFile[i].getCanonicalFile()
                  + ".conll03");
              KAFDocument kaf = KAFDocument.createFromFile(listFile[i]);
              String outKAF = nafToCoNLLConvert03(kaf);
              Files.write(outKAF, outfile, Charsets.UTF_8);
              System.err.println(">> Wrote CoNLL03 document to " + outfile);
            } catch (FileNotFoundException noFile) {
              continue;
            }
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
  public String nafToCoNLLConvert03(KAFDocument kaf) {
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
          String neType = this.convertToConLLTypes(neClass);
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
  
  
  public void trivagoAspectsToCoNLL02(File dir) throws IOException {
    // process one file
    if (dir.isFile()) {
      KAFDocument kaf = KAFDocument.createFromFile(dir);
      System.err.println(">> Processing " + dir.getName());
      File outfile = new File(dir.getCanonicalFile() + ".conll02");
      String outKAF = this.trivagoAspectsToCoNLLConvert02(kaf);
      Files.write(outKAF, outfile, Charsets.UTF_8);
      System.err.println(">> Wrote CoNLL document to " + outfile);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            trivagoAspectsToCoNLL02(listFile[i]);
          } else {
            try {
              System.err.println(">> Processing " + listFile[i].getName());
              File outfile = new File(listFile[i].getCanonicalFile()
                  + ".conll02");
              KAFDocument kaf = KAFDocument.createFromFile(listFile[i]);
              String outKAF = this.trivagoAspectsToCoNLLConvert02(kaf);
              Files.write(outKAF, outfile, Charsets.UTF_8);
              System.err.println(">> Wrote CoNLL02 document to " + outfile);
            } catch (FileNotFoundException noFile) {
              continue;
            }
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
  public String trivagoAspectsToCoNLLConvert02(KAFDocument kaf) {
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
          String neType = convertToConLLTypes(neClass);
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
              sb.append(neType);
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
            sb.append(neType);
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
  public String convertToConLLTypes(String neType) {
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

  public void brownClusterClean(File dir) throws IOException {
    // process one file
    if (dir.isFile()) {
      File outfile = new File(dir.getCanonicalFile() + ".clean");
      String outKAF = brownCleanUpperCase(dir);
      Files.write(outKAF, outfile, Charsets.UTF_8);
      System.err.println(">> Wrote clean document to " + outfile);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            brownClusterClean(listFile[i]);
          } else {
            try {
              File outfile = new File(listFile[i].getCanonicalFile() + ".clean");
              String outKAF = brownCleanUpperCase(listFile[i]);
              Files.write(outKAF, outfile, Charsets.UTF_8);
              System.err.println(">> Wrote pre-clean document to " + outfile);
            } catch (FileNotFoundException noFile) {
              continue;
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
   * @return the list of sentences that contain more than 90% lowercase
   *         characters
   * @throws IOException
   */
  private String brownCleanUpperCase(File inFile) throws IOException {
    InputStream inputStream = CmdLineUtil.openInFile(inFile);
    StringBuilder precleantext = new StringBuilder();
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
    breader.close();
    return precleantext.toString();
  }

}
