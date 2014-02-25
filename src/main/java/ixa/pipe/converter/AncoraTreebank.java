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


package ixa.pipe.converter;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Converts Ancora XML constituent parsing document into a Penn Treebank
 * formatted document
 * 
 * <ol>
 * <li> The first if statement in the startElement function filters the 
 *      elements/constituents not to be used to construct the Penn Treebank parse tree.
 * <li> The startElement function also normalizes ( and ) with -LRB- and -RRB- following
 *      Penn Treebank conventions. 
 * <li> The getTrees() function prints the Penn Treebank formatted Ancora trees. 
 *      IMPORTANT: these trees will contain ancora elements with "elliptic" and "missing"
 *      attributes (in ancora 2.0 they are all SN) which in Penn Treebank format create
 *      empty parse trees such as (SN). These elements need to be removed from the output
 *      of getTrees() with a regexp such as "\\(\\S+\\)".  
 * <li> the endElement adds a closing bracket ) for each constituent used in startElement
 *      except for SENTENCE, which being the last bracket of the tree, is added a \n to separate
 *      full sentence trees.
 * </ol>
 *       
 * @author ragerri
 *
 */
public class AncoraTreebank extends DefaultHandler {

  List<String> constituents = new ArrayList<String>();
  
  
  /**
   * Prints the trees. Note there are "elliptic" and 
   * "missing" elements which create empty trees to be removed with a regexp
   * such as "\\(\\S+\\)
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
        if (attributes.getValue("wd").equalsIgnoreCase("(") || attributes.getValue("wd").equalsIgnoreCase(")")) {
          
          // normalize ( and ) with -LRB- and -RRB- following Penn Treebank conventions  
          String wordForm = attributes.getValue("wd").replace("(", "-LRB-").replace(")", "-RRB");
          constituents.add(" (" + wordForm + " " + attributes.getValue("wd"));
        } 
        else {
          constituents.add(" (" + attributes.getValue("pos").toUpperCase() + " " + attributes.getValue("wd"));
        }
      } else if (attributes.getValue("pos") == null && attributes.getValue("wd") != null) {
        constituents.add(" (" + qName.toUpperCase() + " " + attributes.getValue("wd"));
      } else if (qName.equals("sentence")) {
        constituents.add("(" + qName.toUpperCase());
      }
      else {
        constituents.add(" (" + qName.toUpperCase());
      }
    }
  }

  // calls by the parser whenever '>' end tag is found in xml
  public void endElement(String uri, String localName, String qName) throws SAXException {

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
