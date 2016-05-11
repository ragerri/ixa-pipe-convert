package eus.ixa.ixa.pipe.convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import opennlp.tools.parser.Parse;

public class ParseToTabulated {

  private ParseToTabulated() {
  }

  public static void parseToTabulated(File inFile) throws IOException {
    final List<String> inputTrees = Files.readLines(
        new File(inFile.getCanonicalPath()), Charsets.UTF_8);
    final File outfile = new File(Files.getNameWithoutExtension(inFile
        .getPath()) + ".tsv");
    final String outTree = parseToTabulated(inputTrees);
    Files.write(outTree, outfile, Charsets.UTF_8);
    System.err.println(">> Wrote Tabulated POS format to " + outfile);
  }

  private static String parseToTabulated(List<String> inputTrees)
      throws IOException {

    StringBuilder sb = new StringBuilder();
    for (final String parseSent : inputTrees) {
      final Parse parse = Parse.parseParse(parseSent);
      if (parse != null) {
        List<String> toks = new ArrayList<String>();
        List<String> tags = new ArrayList<String>();
        Parse[] nodes = parse.getTagNodes();
        for (int i = 0; i < nodes.length; i++) {
          Parse token = nodes[i];
          toks.add(token.getCoveredText());
          tags.add(token.getType());
        }
        for (int i = 0; i < tags.size(); i++) {
          sb.append(toks.get(i)).append("\t").append(tags.get(i)).append("\n");
        }
      }
      sb.append("\n");
    }
    return sb.toString();
  }
}
