package ixa.pipe.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AncoraTreebank extends DefaultHandler {
  
  List<String> constituents = new ArrayList<String>();
  
  private void printTree() {
    for (String constituent: constituents) {
      System.out.print(constituent);
    }
  }

  
  public void readAncoraConstituents(String inFile) throws IOException {

    try {

      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      SAXParser saxParser = saxParserFactory.newSAXParser();
      DefaultHandler defaultHandler = new DefaultHandler() {
        
        
        
        // this method is called every time the parser gets an open tag '<'
        public void startElement(String uri, String localName, String qName,
            Attributes attributes) {
          
          if (!qName.equalsIgnoreCase("article") && !qName.equalsIgnoreCase("spec") && 
              attributes.getIndex("elliptic") == -1 && attributes.getIndex("missing") == -1) {
            
           if (attributes.getValue("pos") != null) {
             if (attributes.getValue("wd").equalsIgnoreCase("(") || attributes.getValue("wd").equalsIgnoreCase(")")) {
               String wordForm = attributes.getValue("wd").replace("(","-LRB-").replace(")","-RRB");
               constituents.add(" (" + wordForm + " " + attributes.getValue("wd"));
             }
             constituents.add(" (" + attributes.getValue("pos").toUpperCase() + " " + attributes.getValue("wd"));
           }
           else if (attributes.getValue("pos") == null && attributes.getValue("wd") != null) { 
             constituents.add(" (" + qName.toUpperCase() + " " + attributes.getValue("wd"));
           }
           else if (qName.equalsIgnoreCase("sentence")) { 
             constituents.add("(" + qName.toUpperCase());
           }
           else { 
             constituents.add(" (" + qName.toUpperCase());
           }
          }
        }
        
        // calls by the parser whenever '>' end tag is found in xml
        
        public void endElement(String uri, String localName, String qName) {
          
          if (!qName.equalsIgnoreCase("article") && !qName.equalsIgnoreCase("spec")) {
            
            if (qName.equalsIgnoreCase("sentence")) { 
              constituents.add(")\n");
            }
            else { 
              constituents.add(")");
            }
            
          }
        }

      };// end of defaultHandler

      saxParser.parse(inFile, defaultHandler);
      printTree();
      

    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
