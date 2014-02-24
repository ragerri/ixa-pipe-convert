/*
 * Copyright 2013 Rodrigo Agerri

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

package ixa.pipe.converter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.parser.Parse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * 
 * Class to provide parsing annotation in various forms: KAF, Penn style, and
 * with headWords marked. It also loads the right model for each language.
 * 
 * @author ragerri
 * 
 */
public class Convert {
  
  
  public void ancora2treebank(String inXML) throws IOException { 
    if (!inXML.isEmpty()) { 
      AncoraTreebank ancoraParser = new AncoraTreebank();
      ancoraParser.readAncoraConstituents(inXML);
    }
    else { 
      System.out.println("Please choose a valid file as input");
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

}
