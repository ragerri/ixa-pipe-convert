# coding=utf-8
""" Process a Ancora style xml into CONLL 2011 format
"""

from __future__ import unicode_literals

__author__ = 'Josu Bermudez <josu.bermudez@deusto.es>'

from xml import sax
import sys
import re
FALSE = False
TRUE = "yes"
FILL = "-"
ARTICLE = "article"
SENTENCE = "sentence"
POS = "pos"
FORM = "wd"
LEMMA = "lem"
PARSE_ROW = 5
ELLIPTIC = "elliptic"
ELLIPTIC_POS = "e"
ELLIPTIC_FORM = ""
ELLIPTIC_LEMMA = ""

NAMED_ENTITY = "ne"
NAMED_ENTITY_ROW = 11
NO_NAMED_ENTITY = False

ENTITY = "entity"
COREF_ROW = 13
NO_ENTITY = False
COREF_TYPE = "coreftype"
VALID_COREF_TYPE = ("", "ident")
MISSING = "missing"
CORPUS = "corpus"

class AncoraToConll(sax.ContentHandler):
    """ Process a Ancora type XML into a Conll format.
    """

    def __init__(self, name="file"):
        sax.ContentHandler.__init__(self)
        self.name = name
        self._conll = []
        self._part = 0
        self._sentence = 0
        self._token = 0
        self._tree = ""
        self._last_is_leaf = False
        self._ne_stack = []
        self._ne = ""
        self._coref_stack = []
        self._coref = ""
        self._entity_list = []
        self.all_tree = []
        self._skip = 0

    def get_conll(self):
        """Generate a string of the conll result of the document.
        """
        return "   ".join(((unicode(element) for line in self._conll for element in line))).replace("\n   ", "\n")

    def startElement(self, name, attrs):
        """ SAX parser API: Called each element start.

        :param name: The tag name
        :param attrs: The attributes of the element
        """
        if name in ("spec",):
            return None
        if attrs.get(MISSING, False) or attrs.get(ELLIPTIC, FALSE):
            self._elliptic(name, attrs)
            return None
        try:
            if name == CORPUS:
                return None
            elif name == ARTICLE:
                self._start_article(attrs)
            elif name == SENTENCE:
                self._start_sentence(attrs)
            else:
                if FORM in attrs:
                    self._last_is_leaf = True
                    self._start_leaf(name, attrs)
                else:
                    self._start_constituent(name, attrs)
        except Exception as ex:
            print "ERROR", self._sentence, self._token, ex

    def endElement(self, name):
        """ SAX parser API: Called each element end.

        :param name: The tag name
        """
        if self._skip:
            self._skip -= 1
            return None
        elif name in ("spec",):
            return None
        try:
            if name == CORPUS:
                return None
            elif name == ARTICLE:
                self._end_article()
            elif name == SENTENCE:
                self._end_sentence()
            else:
                if self._last_is_leaf:
                    self._last_is_leaf = False
                    self._end_leaf()
                else:
                    self._end_constituent()
        except Exception as ex:
            print "ERROR", self._sentence, self._token, ex

    def _start_article(self, attrs):
        """Mark the start of a part and reset inter sentence trackers and counters.

        :param attrs: The attributes of the element
        """
        pass

    def _start_sentence(self, attrs):
        """Reset all intra sentence trackers and counters,

        :param attrs: The attributes of the element.
        """
        self.all_tree.append("(TOP")

    def _start_constituent(self, name, attrs):
        """ Create the marks of the constituent start token for NE and entities. Also,
         create the tracking for NE and entities.


        :param name: The tag name
        :param attrs: The attributes of the element
        """

        self.all_tree[-1] += " (" + name.upper()

    def _start_leaf(self, name, attrs):
        """ Create the token registry in the document

        :param name: The tag name
        :param attrs: The attributes of the element
        """
        form = attrs.get(FORM).replace("(", "-LRB-")\
            .replace(")", "-RRB-").replace("{", "-LCB-").replace("}", "-RCB-").replace("[", "-LSB-").replace("]", "-RSB-")
        pos = (attrs.get(POS, False) or name).upper()
        self.all_tree[-1] += " ({0} {1})" .format(pos, form)

    def _end_leaf(self):
        """Increment the token counter.
        """
        pass

    def _end_constituent(self):
        """Set the constituent last token attributes:
            NE closing parenthesis and entity closing values.
        """
        self.all_tree[-1] += ")"

    def _end_sentence(self):
        """Close the sentence with a sentence mark, increment the counter.
        """
        self.all_tree[-1] += ")"
        if self.all_tree[-1] == "(TOP)":
            self.all_tree.pop(-1)

    def _end_article(self):
        """Close the article and increment the part counter

        """
    pass

    def _elliptic(self, name, attrs):
        """ A elliptic element is encountered. Create a artificial token for it.

        :param name: The tag name
        :param attrs: The attributes of the element
        """

        self._skip = 1
        #self.all_tree[-1] = re.sub("\((\s|\w)+$", "", self.all_tree[-1])


if __name__ == "__main__":
    input_stream = sys.stdin
    content_handler = AncoraToConll()
    sax.parse(input_stream, content_handler)
    sys.stdout.write("\n".join(content_handler.all_tree).encode("utf-8"))