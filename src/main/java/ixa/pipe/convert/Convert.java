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

package ixa.pipe.convert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import opennlp.tools.parser.Parse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;

/**
 * 
 * 
 * @author ragerri
 * 
 */
public class Convert {
  
  
  /**
   * Process the ancora constituent XML annotation into 
   * Penn Treebank bracketing style
   * 
   * @param inXML
   * @throws IOException
   */
  public String ancora2treebank(File inXML) throws IOException { 
    String filteredTrees = null;
    if (inXML.isFile()) {
      
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      SAXParser saxParser;
      try {
        saxParser = saxParserFactory.newSAXParser();
        AncoraTreebank ancoraParser = new AncoraTreebank();
        saxParser.parse(inXML,ancoraParser);
        String trees = ancoraParser.getTrees();
        // remove empty trees created by "missing" and "elliptic" attributes
        filteredTrees = trees.replaceAll("\\(\\SN\\)","");
        // format correctly closing brackets
        filteredTrees = filteredTrees.replace(") )","))");
        // remove double spaces
        filteredTrees = filteredTrees.replaceAll("  "," ");
        //remove empty sentences created by <sentence title="yes"> elements
        filteredTrees = filteredTrees.replaceAll("\\(SENTENCE \\)\n","");
      } catch (ParserConfigurationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (SAXException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    else { 
      System.out.println("Please choose a valid file as input");
    }
    return filteredTrees;
  }
  
  /**
   * It call the ancorat2treebank function to generate 
   * Penn Treebank trees from Ancora XML constituent parsing
   * annotation
   * 
   * @param dir
   * @throws IOException
   */
  public void processAncoraConstituentXMLCorpus(File dir)
      throws IOException {
    // process one file
    if (dir.isFile()) {
      File outfile = new File(FilenameUtils.removeExtension(dir.getPath())+ ".th");
      String outTree = ancora2treebank(dir);
      FileUtils.writeStringToFile(outfile, outTree, "UTF-8");
      System.err.println(">> Wrote XML ancora file to Penn Treebank in " + outfile);
    } else {
      // recursively process directories
      File listFile[] = dir.listFiles();
      if (listFile != null) {
        for (int i = 0; i < listFile.length; i++) {
          if (listFile[i].isDirectory()) {
            processAncoraConstituentXMLCorpus(listFile[i]);
          } else {
            try {
              File outfile = new File(FilenameUtils.removeExtension(listFile[i].getPath()) + ".th");
              String outTree = ancora2treebank(listFile[i]);
              FileUtils.writeStringToFile(outfile, outTree, "UTF-8");
              System.err.println(">> Wrote XML Ancora file Penn treebank format in " + outfile);
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
   * @param treebankFile the input file
   * @throws IOException
   */
  public void treebank2tokens(File treebankFile)
      throws IOException {
    // process one file
    if (treebankFile.isFile()) {
      List<String> inputTrees = FileUtils.readLines(
          new File(treebankFile.getCanonicalPath()), "UTF-8");
      File outfile = new File(FilenameUtils.removeExtension(treebankFile.getPath())
          + ".tok");
      String outFile = getTokensFromTree(inputTrees);
      FileUtils.writeStringToFile(outfile, outFile, "UTF-8");
      System.err.println(">> Wrote tokens to " + outfile);
    } else {
          System.out
              .println("Please choose a valid file as input.");
          System.exit(1);
    }
  }
  
  /**
   * Reads a list of Parse trees and calls 
   * {@code getTokens} to create tokenized oneline text
   * 
   * @param inputTrees
   * @return the tokenized document
   */
  private String getTokensFromTree(List<String> inputTrees) {
    
    StringBuilder parsedDoc = new StringBuilder();
    for (String parseSent : inputTrees) {
      Parse parse = Parse.parseParse(parseSent);
      StringBuilder sentBuilder = new StringBuilder();
      getTokens(parse,sentBuilder);        
      parsedDoc.append(sentBuilder.toString()).append("\n");  
    }
    return parsedDoc.toString();
  }
  
  /**
   * It converts a penn treebank constituent tree into 
   * tokens oneline form
   * 
   * @param parse
   * @param sb
   */
  private void getTokens(Parse parse, StringBuilder sb) {
      if (parse.isPosTag()) {
        if (!parse.getType().equals("-NONE-")) { 
          sb.append(parse.getCoveredText()).append(" ");
        }
      }
    else {
      Parse children[] = parse.getChildren();
      for (int i = 0; i < children.length; i++) {
        getTokens(children[i],sb);
      }
    }
  }
  
  
  /**
   * Takes a file containing Penn Treebank oneline annotation and creates 
   * Word_POS sentences for POS tagger training, saving it to a file 
   * with the *.pos extension.
   * 
   * @param treebankFile the input file
   * @throws IOException
   */
  public void treebank2WordPos(File treebankFile)
      throws IOException {
    // process one file
    if (treebankFile.isFile()) {
      List<String> inputTrees = FileUtils.readLines(
          new File(treebankFile.getCanonicalPath()), "UTF-8");
      File outfile = new File(FilenameUtils.removeExtension(treebankFile.getPath())
          + ".pos");
      String outFile = getPreTerminals(inputTrees);
      FileUtils.writeStringToFile(outfile, outFile, "UTF-8");
      System.err.println(">> Wrote Apache OpenNLP POS training format to " + outfile);
    } else {
          System.out
              .println("Please choose a valid file as input.");
          System.exit(1);
    }
  }
  
  /**
   * Reads a list of Parse trees and calls 
   * {@code getWordType} to create POS training data
   * in Word_POS form 
   * 
   * @param inputTrees
   * @return the document with Word_POS sentences
   */
  private String getPreTerminals(List<String> inputTrees) {
    
    StringBuilder parsedDoc = new StringBuilder();
    for (String parseSent : inputTrees) {
      Parse parse = Parse.parseParse(parseSent);
      StringBuilder sentBuilder = new StringBuilder();
      getWordType(parse,sentBuilder);        
      parsedDoc.append(sentBuilder.toString()).append("\n");  
    }
    return parsedDoc.toString();
  }
  
  /**
   * It converts a penn treebank constituent tree into 
   * Word_POS form
   * 
   * @param parse
   * @param sb
   */
  private void getWordType(Parse parse, StringBuilder sb) {
      if (parse.isPosTag()) {
        if (!parse.getType().equals("-NONE-")) { 
          sb.append(parse.getCoveredText()).append("_").append(parse.getType()).append(" ");
        }
      }
    else {
      Parse children[] = parse.getChildren();
      for (int i = 0; i < children.length; i++) {
        getWordType(children[i],sb);
      }
    }
  }
  
  /**
   * It normalizes a oneline Penn treebank style tree removing 
   * trace nodes (-NONE-) and pruning the empty trees created by 
   * removing the trace nodes.
   * 
   * @param treebankFile
   * @throws IOException
   */
  public void getCleanPennTrees(File treebankFile) throws IOException { 
    if (treebankFile.isFile()) {
      List<String> inputTrees = FileUtils.readLines(
          new File(treebankFile.getCanonicalPath()), "UTF-8");
      File outfile = new File(FilenameUtils.removeExtension(treebankFile.getPath())
          + ".treeNormalize");
      String outFile = normalizeParse(inputTrees);
      FileUtils.writeStringToFile(outfile, outFile, "UTF-8");
      System.err.println(">> Wrote normalized parse to " + outfile);
    } else {
          System.out
              .println("Please choose a valid file as input.");
          System.exit(1);
    }
  }
  
  /**
   * It takes as input a semi-pruned penn treebank tree (e.g., with 
   * -NONE- traces removed) via 
   * sed 's/-NONE-\s[\*A-Za-z0-9]*[\*]*[\-]*[A-Za-z0-9]*'
   * 
   * and prunes the empty trees remaining from the sed operation
   * 
   * @param inputTrees
   * @return
   */
  //TODO add the sed regexp to this function
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
  

}
