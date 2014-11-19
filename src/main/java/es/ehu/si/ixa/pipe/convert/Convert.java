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

package es.ehu.si.ixa.pipe.convert;

import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.KAFDocument.Layer;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.parser.Parse;
import opennlp.tools.postag.POSDictionary;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;

/**
 * Convert functions.
 * 
 * @author ragerri
 * @version 2014-10-28
 * 
 */
public class Convert {
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
   * Calls the ancorat2treebank function to generate Penn Treebank trees from
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
   * Extract only some name entities types in opennlp format.
   * 
   * @param infile
   *          the document in opennlp format for named entities
   * @param neTypes
   *          the types to be extracted, separated by a comma
   * @throws IOException
   *           if io problems
   */
  public void filterNameTypes(String infile, String neTypes) throws IOException {
    FilterNameByType filter = null;
    try {
      filter = new FilterNameByType(infile, neTypes);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    filter.getNamesByType();
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
                  .getPath()) + ".kaf.tok");
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
    kaf.removeLayer(Layer.entities);
    kaf.removeLayer(Layer.constituency);
    kaf.removeLayer(Layer.coreferences);
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
      if (entity.getExternalRefs().size() > 0)
        System.out.println(entity.getExternalRefs().get(0).getReference());
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
   * Generates {@code POSDictionary} from a list of words and its postag.
   * 
   * @param inputLines
   *          the list of words and postag per line
   * @return the POSDictionary
   */
  public POSDictionary getPOSTaggerDict(List<String> inputLines) {
    POSDictionary posTaggerDict = new POSDictionary();
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
  public void addPOSTaggerDict(List<String> inputLines, POSDictionary tagDict) {
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

  public void getYelpText(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    BufferedReader breader = new BufferedReader(new FileReader(fileName));
    String line;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        String text = (String) jsonObject.get("text");
        System.out.println(text); 
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    breader.close();
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
   * @return the annotated named entities in conll03 format
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
      boolean previousIsEntity = false;

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
              if (j == 0 || previousIsEntity) {
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
          previousIsEntity = true;
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
              if (j == 0 && previousIsEntity) {
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
            if (previousIsEntity) {
              sb.append(BIO.BEGIN.toString());
            } else {
              sb.append(BIO.IN.toString());
            }
            sb.append(neType);
            sb.append("\n");
          }
          previousIsEntity = true;
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
        || neType.startsWith("LOC") || neType.startsWith("GPE")) {
      conllType = neType.substring(0, 3);
    } else if (neType.equalsIgnoreCase("MISC")) {
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

  public void absaSemEvalToNER(String fileName) {
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      XPathExpression<Element> expr = xFactory.compile("//sentence",
          Filters.element());
      List<Element> sentences = expr.evaluate(doc);
      for (Element sent : sentences) {

        StringBuilder sb = new StringBuilder();
        String sentString = sent.getChildText("text");
        sb = sb.append(sentString);
        Element aspectTerms = sent.getChild("aspectTerms");
        if (aspectTerms != null) {
          List<List<Integer>> offsetList = new ArrayList<List<Integer>>();
          List<Integer> offsets = new ArrayList<Integer>();
          List<Element> aspectTermList = aspectTerms.getChildren();
          if (!aspectTermList.isEmpty()) {
            for (Element aspectElem : aspectTermList) {
              Integer offsetFrom = Integer.parseInt(aspectElem
                  .getAttributeValue("from"));
              Integer offsetTo = Integer.parseInt(aspectElem
                  .getAttributeValue("to"));
              offsets.add(offsetFrom);
              offsets.add(offsetTo);
            }
          }
          Collections.sort(offsets);
          for (int i = 0; i < offsets.size(); i++) {
            List<Integer> offsetArray = new ArrayList<Integer>();
            offsetArray.add(offsets.get(i++));
            if (offsets.size() > i) {
              offsetArray.add(offsets.get(i));
            }
            offsetList.add(offsetArray);
          }
          int counter = 0;
          for (List<Integer> offsetSent : offsetList) {
            Integer offsetFrom = offsetSent.get(0);
            Integer offsetTo = offsetSent.get(1);
            String aspectString = sentString.substring(offsetFrom, offsetTo);
            sb.replace(offsetFrom + counter, offsetTo + counter,
                "<START:term> " + aspectString + " <END>");
            counter += 19;
          }
        }
        System.out.println(sb.toString());
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }

  public void absaSemEvalToNER2015(String fileName) {
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      XPathExpression<Element> expr = xFactory.compile("//sentence",
          Filters.element());
      List<Element> sentences = expr.evaluate(doc);
      for (Element sent : sentences) {
        
        String sentString = sent.getChildText("text");
        StringBuilder sb = new StringBuilder();
        sb = sb.append(sentString);
        Element opinionsElement = sent.getChild("Opinions");
        if (opinionsElement != null) {
          List<List<Integer>> offsetList = new ArrayList<List<Integer>>();
          List<Integer> offsets = new ArrayList<Integer>();
          List<Element> oteList = opinionsElement.getChildren();
          for (Element aspectElem : oteList) {
            if (!aspectElem.getAttributeValue("target").equals("NULL")) {
              Integer offsetFrom = Integer.parseInt(aspectElem
                  .getAttributeValue("from"));
              Integer offsetTo = Integer.parseInt(aspectElem
                  .getAttributeValue("to"));
              offsets.add(offsetFrom);
              offsets.add(offsetTo);
            }
          }
          List<Integer> offsetsWithoutDuplicates = new ArrayList<Integer>(
              new HashSet<Integer>(offsets));
          Collections.sort(offsetsWithoutDuplicates);

          for (int i = 0; i < offsetsWithoutDuplicates.size(); i++) {
            List<Integer> offsetArray = new ArrayList<Integer>();
            offsetArray.add(offsetsWithoutDuplicates.get(i++));
            if (offsetsWithoutDuplicates.size() > i) {
              offsetArray.add(offsetsWithoutDuplicates.get(i));
            }
            offsetList.add(offsetArray);
          }
          int counter = 0;
          for (List<Integer> offsetSent : offsetList) {
            Integer offsetFrom = offsetSent.get(0);
            Integer offsetTo = offsetSent.get(1);
            String aspectString = sentString.substring(offsetFrom, offsetTo);
            sb.replace(offsetFrom + counter, offsetTo + counter,
                "<START:target> " + aspectString + " <END>");
            counter += 21;
          }
          System.out.println(sb.toString());
        }
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public void absaSemEvalToMultiClassNER2015(String fileName) {
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      XPathExpression<Element> expr = xFactory.compile("//sentence",
          Filters.element());
      List<Element> sentences = expr.evaluate(doc);
      for (Element sent : sentences) {
        
        String sentString = sent.getChildText("text");
        StringBuilder sb = new StringBuilder();
        sb = sb.append(sentString);
        Element opinionsElement = sent.getChild("Opinions");
        if (opinionsElement != null) {
          List<List<Integer>> offsetList = new ArrayList<List<Integer>>();
          HashSet<String> targetClassSet = new LinkedHashSet<String>();
          List<Integer> offsets = new ArrayList<Integer>();
          List<Element> opinionList = opinionsElement.getChildren();
          for (Element opinion : opinionList) {
            if (!opinion.getAttributeValue("target").equals("NULL")) {
              String className = opinion.getAttributeValue("category");
              String targetString = opinion.getAttributeValue("target");
              Integer offsetFrom = Integer.parseInt(opinion
                  .getAttributeValue("from"));
              Integer offsetTo = Integer.parseInt(opinion
                  .getAttributeValue("to"));
              offsets.add(offsetFrom);
              offsets.add(offsetTo);
              targetClassSet.add(targetString + "JAR!" + className + opinion.getAttributeValue("from") + opinion.getAttributeValue("to"));
            }
          }
          List<Integer> offsetsWithoutDuplicates = new ArrayList<Integer>(
              new HashSet<Integer>(offsets));
          Collections.sort(offsetsWithoutDuplicates);
          List<String> targetClassList = new ArrayList<String>(targetClassSet);

          for (int i = 0; i < offsetsWithoutDuplicates.size(); i++) {
            List<Integer> offsetArray = new ArrayList<Integer>();
            offsetArray.add(offsetsWithoutDuplicates.get(i++));
            if (offsetsWithoutDuplicates.size() > i) {
              offsetArray.add(offsetsWithoutDuplicates.get(i));
            }
            offsetList.add(offsetArray);
          }
          int counter = 0;
          for (int i = 0; i < offsetList.size(); i++) {
            Integer offsetFrom = offsetList.get(i).get(0);
            Integer offsetTo = offsetList.get(i).get(1);
            String className = targetClassList.get(i);
            String aspectString = sentString.substring(offsetFrom, offsetTo);
            sb.replace(offsetFrom + counter, offsetTo + counter,
                "<START:"+ className.split("JAR!")[1].substring(0, 3) + "> " + aspectString + " <END>");
            counter += 18;
          }
          System.out.println(sb.toString());
        }
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }

  public void absaSemEvalText(Reader reader) {
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(reader);
      XPathExpression<Element> expr = xFactory.compile("//sentence",
          Filters.element());
      List<Element> sentences = expr.evaluate(doc);
      for (Element sent : sentences) {
        String sentString = sent.getChildText("text");
        System.out.println(sentString);
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
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
