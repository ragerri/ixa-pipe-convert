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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Converts Ancora XML constituent parsing document into a Penn Treebank
 * formatted document:
 * 
 * <ol>
 * <li>The first if statement in the startElement function filters the
 * elements/constituents not to be used to construct the Penn Treebank parse
 * tree.
 * <li>The startElement function also normalizes ( and ) with -LRB- and -RRB-
 * following Penn Treebank conventions.
 * <li>The getTrees() function prints the Penn Treebank formatted Ancora trees.
 * IMPORTANT:
 * <ol>
 * <li>these trees will contain ancora elements with "elliptic" and "missing"
 * attributes (in ancora 2.0 they are all SN) which in Penn Treebank format
 * create empty parse trees such as (SN). These elements need to be removed from
 * the output of getTrees() with a regexp such as "\\(\\SN\\)". Also remove
 * doubles spaces to make sure the tree is correctly formatted.
 * <li>The trees also contain <sentence title=yes elements, which create empty
 * (SENTENCE ) parse trees. These are removed using the regexp "\\(SENTENCE
 * \\)\n
 * </ol>
 * <li>the endElement adds a closing bracket ) for each constituent used in
 * startElement except for SENTENCE, which being the last bracket of the tree,
 * is added a \n to separate full sentence trees.
 * <li>Corpus typos Ancora 2.0:
 * <ol>
 * <li>3LB-CAST/a15-5.tbf.xml, line 1015 pos=fpa added.
 * <li>CESS-CAST-A/12432_20000416.tbf.xml line 1631 pos=fpt added.
 * <ol>
 * CESS-CAST-P/132_20010301.tbf.xml line 1748 pos=fpt added.
 * <ol>
 * CESS-CAST-P/179_20010401.tbf.xml line 1069 pos=fpt added.
 * </ol>
 * 
 * @author ragerri
 * @version 2014-02-25
 *
 */
public class AncoraTreebankReader extends DefaultHandler {

  List<String> constituents = new ArrayList<String>();

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
      System.err
          .println(">> Wrote XML ancora file to Penn Treebank in " + outfile);
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      SAXParser saxParser;
      try {
        saxParser = saxParserFactory.newSAXParser();
        AncoraTreebankReader ancoraParser = new AncoraTreebankReader();
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
  public static void processAncoraConstituentXMLCorpus(Path dir)
      throws IOException {
    // process one file
    if (Files.isRegularFile(dir) && !dir.endsWith(".th")) {
      ancora2treebank(dir);
    } else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            processAncoraConstituentXMLCorpus(file);
          } else {
            if (!file.endsWith(".th")) {
              ancora2treebank(file);
            }
          }
        }
      }
    }
  }

  /**
   * Prints the trees. Note there are "elliptic" and "missing" elements which
   * create empty trees to be removed with a regexp such as "\\(\\S+\\)
   * 
   * @return the trees in penn treebank format
   */
  public String getTrees() {
    StringBuilder sb = new StringBuilder();
    for (String constituent : constituents) {
      sb.append(constituent);
    }
    return sb.toString();
  }

  // this method is called every time the parser gets an open tag '<'
  public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {

    // do not print/use these elements/constituents
    if (!qName.equals("article") && !qName.equals("spec")) {

      if (attributes.getValue("pos") != null) {
        if (attributes.getValue("wd").equalsIgnoreCase("(")
            || attributes.getValue("wd").equalsIgnoreCase(")")) {

          // normalize ( and ) with -LRB- and -RRB- following Penn Treebank
          // conventions
          String wordForm = attributes.getValue("wd").replace("(", "-LRB-")
              .replace(")", "-RRB-");
          constituents.add(
              " (" + attributes.getValue("pos").toUpperCase() + " " + wordForm);
        } else {
          constituents.add(" (" + attributes.getValue("pos").toUpperCase() + " "
              + attributes.getValue("wd"));
        }
      } else if (attributes.getValue("pos") == null
          && attributes.getValue("wd") != null) {
        constituents
            .add(" (" + qName.toUpperCase() + " " + attributes.getValue("wd"));
      } else if (qName.equals("sentence")) {
        constituents.add("(" + qName.toUpperCase());
      } else {
        constituents.add(" (" + qName.toUpperCase());
      }
    }
  }

  // calls by the parser whenever '>' end tag is found in xml
  public void endElement(String uri, String localName, String qName)
      throws SAXException {

    // do not close or use these elements
    if (!qName.equals("article") && !qName.equals("spec")) {

      if (qName.equals("sentence")) {
        constituents.add(")\n");
      } else {
        constituents.add(")");
      }
    }
  }

}
