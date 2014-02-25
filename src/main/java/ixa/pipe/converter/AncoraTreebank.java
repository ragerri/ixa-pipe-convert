package ixa.pipe.converter;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class AncoraTreebank extends DefaultHandler {

  List<String> constituents = new ArrayList<String>();

  private XMLReader xmlReader;

  public AncoraTreebank(XMLReader xmlReader) {
    this.xmlReader = xmlReader;
  }

  public void printTree() {
    for (String constituent : constituents) {
      if (constituent.matches("\\(\\S\\)")) {
        continue;
      }
      else { 
        System.out.print(constituent);
      }
      
    }
  }

  // this method is called every time the parser gets an open tag '<'
  public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {

    // do not print this elements/constituents
    if (!qName.equals("article") && !qName.equals("spec")) {
 
      if (attributes.getValue("pos") != null) {
        if (attributes.getValue("wd").equalsIgnoreCase("(") || attributes.getValue("wd").equalsIgnoreCase(")")) {
          String wordForm = attributes.getValue("wd").replace("(", "-LRB-")
              .replace(")", "-RRB");
          constituents.add(" (" + wordForm + " " + attributes.getValue("wd"));
        } else {
          constituents.add(" (" + attributes.getValue("pos").toUpperCase()
              + " " + attributes.getValue("wd"));
        }
      } else if (attributes.getValue("pos") == null && attributes.getValue("wd") != null) {
        constituents.add(" (" + qName.toUpperCase() + " "
            + attributes.getValue("wd"));
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

    if (!qName.equals("article") && !qName.equals("spec")) {

      if (qName.equals("sentence")) {
        constituents.add(")\n");
      } else {
        constituents.add(")");
      }

    }
  }
  

}
