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

package ixa.pipe.convert;


import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.xml.sax.SAXException;

/**
 * ixa-pie-converter
 * 
 * @author ragerri
 * @version 1.0
 * 
 */

public class CLI {

  /**
   * 
   * @param args
   * @throws IOException
   * @throws SAXException 
   * @throws ParserConfigurationException 
   */

  public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {

    Namespace parsedArguments = null;

    // create Argument Parser
    ArgumentParser parser = ArgumentParsers.newArgumentParser(
        "ixa-pipe-converter-1.0.jar").description(
        "ixa-pipe-converter-1.0 converts corpora from one format into another.\n");
    
    parser.addArgument("--ancora2treebank").help("Converts ancora constituent parsing annotation into Penn Treebank bracketing format");

    parser.addArgument("--treebank2tokens").help("Converts treebank into tokenized oneline text\n.");
    
    parser.addArgument("--treebank2WordPos").help("Converts reebank into Apache OpenNLP POS training format.\n");
    
    parser.addArgument("--normalizePennTreebank").help("Normalizes Penn Treebank removing -NONE- nodes and funcional tags.\n");
    
    /*
     * Parse the command line arguments
     */

    // catch errors and print help
    try {
      parsedArguments = parser.parseArgs(args);
      System.err.println("CLI options: " + parsedArguments);
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.out
          .println("Run java -jar target/ixa-pipe-converter-1.0.jar -help for details");
      System.exit(1);
    }
    
    if (parsedArguments.getString("treebank2WordPos") != null) {
      File inputTree = new File(parsedArguments.getString("treebank2WordPos"));
      Convert converter = new Convert();
      converter.treebank2WordPos(inputTree);
    }
    
    else if (parsedArguments.get("ancora2treebank") != null) { 
      File inputXML = new File(parsedArguments.getString("ancora2treebank"));
      Convert converter = new Convert();
      converter.processAncoraConstituentXMLCorpus(inputXML);
    }
    
    else if (parsedArguments.getString("treebank2tokens") != null) {
      File inputTree = new File(parsedArguments.getString("treebank2tokens"));
      Convert converter = new Convert();
      converter.treebank2tokens(inputTree);
    }
    else if (parsedArguments.get("normalizePennTreebank") != null) {
      File inputTree = new File(parsedArguments.getString("normalizePennTreebank"));
      Convert converter = new Convert();
      converter.getCleanPennTrees(inputTree);
    }

  }
}
