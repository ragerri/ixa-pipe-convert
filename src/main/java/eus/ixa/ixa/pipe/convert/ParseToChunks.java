package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.parser.Parse;

public class ParseToChunks {

  private ParseToChunks() {
  }

  public static void parseToChunks(Path inFile) throws IOException {
    final List<String> inputTrees = Files.readAllLines(inFile,
        StandardCharsets.UTF_8);
    final Path outfile = Files
        .createFile(Paths.get(inFile.toString() + ".chunks"));
    final String outTree = parseToChunks(inputTrees);
    Files.write(outfile, outTree.getBytes(StandardCharsets.UTF_8));
    System.err.println(">> Wrote chunks to " + outfile);
  }

  private static String parseToChunks(List<String> inputTrees)
      throws IOException {

    StringBuilder sb = new StringBuilder();
    for (final String parseSent : inputTrees) {
      final Parse parse = Parse.parseParse(parseSent);
      if (parse != null) {
        Parse[] chunks = getInitialChunks(parse);
        List<String> toks = new ArrayList<String>();
        List<String> tags = new ArrayList<String>();
        List<String> preds = new ArrayList<String>();
        for (int ci = 0, cl = chunks.length; ci < cl; ci++) {
          Parse c = chunks[ci];
          if (c.isPosTag()) {
            toks.add(c.getCoveredText());
            tags.add(c.getType());
            preds.add("O");
          } else {
            boolean start = true;
            String ctype = c.getType();
            Parse[] kids = c.getChildren();
            for (int ti = 0, tl = kids.length; ti < tl; ti++) {
              Parse tok = kids[ti];
              toks.add(tok.getCoveredText());
              tags.add(tok.getType());
              if (start) {
                preds.add("B-" + ctype);
                start = false;
              } else {
                preds.add("I-" + ctype);
              }
            }
          }
        }
        for (int i = 0; i < preds.size(); i++) {
          sb.append(toks.get(i)).append("\t").append(tags.get(i)).append("\t")
              .append(preds.get(i)).append("\n");
        }
      }
      sb.append("\n");
    }

    return sb.toString();
  }

  private static Parse[] getInitialChunks(Parse p) {
    List<Parse> chunks = new ArrayList<Parse>();
    getInitialChunks(p, chunks);
    return chunks.toArray(new Parse[chunks.size()]);
  }

  private static void getInitialChunks(Parse p, List<Parse> ichunks) {
    if (p.isPosTag()) {
      ichunks.add(p);
    } else {
      Parse[] kids = p.getChildren();
      boolean allKidsAreTags = true;
      for (int ci = 0, cl = kids.length; ci < cl; ci++) {
        if (!kids[ci].isPosTag()) {
          allKidsAreTags = false;
          break;
        }
      }
      if (allKidsAreTags) {
        ichunks.add(p);
      } else {
        for (int ci = 0, cl = kids.length; ci < cl; ci++) {
          getInitialChunks(kids[ci], ichunks);
        }
      }
    }
  }

}
