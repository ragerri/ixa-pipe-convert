#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
This script takes as input two datasets in CoNLL format and creates a new dataset 
which can be used to evaluate results using conlleval.txt script for the chunking and 
NER CoNLL shared evaluation tasks. 

Example Usage: python conll-2-eval.py -test file.txt -ref esp.testb [-o esp.eval]

Rodrigo Agerri (rodrigo.agerri@ehu.es)

22/11/2012
"""

import argparse
import re
import sys


def get_eval_corpus(refset,testset):
    inb = [line.split(" ") for line in refset]
    words = [line[0] for line in inb]
    tags = [line[-1] for line in inb]
    tags = [line.strip() for line in tags]
    ine = testset[1:]
    ine = [line.split(" ") for line in ine]
    tagsne = [line[-1] for line in ine]
    tagsne = [line.strip() for line in tagsne]
    tags2 = zip(tags,tagsne)
    tags2 = [" ".join(line) for line in tags2]
    words_tags = zip(words,tags2)
    evalb = [list(line) for line in words_tags]
    evalb = [" ".join(line) for line in evalb]
    evalb = [re.sub(r'\n  ','',line) for line in evalb]
    evalb = "\n".join(evalb)
    return evalb

def main(arguments):
    print "processing files: {0} ...".format(arguments.refset[0] + " " + arguments.testset[0])
    inref = open(arguments.refset[0],'r').readlines()
    intest = open(arguments.testset[0],'r').readlines()
    if len(inref) != len(intest):
        print "testset and refset need to be of the same length!"
        parser.print_usage()
        sys.exit(1) 
    evalb = get_eval_corpus(inref,intest)
    if arguments.outfile:
        outeval = open(arguments.outfile[0],'w')
        print "saving evaluation set to: {0} ".format(arguments.outfile[0])
    else:
        outeval = open(arguments.testset[0]+".eval",'w')
        print "saving evaluation set to: {0} ".format(arguments.testset[0]+".eval")
    outeval.write(evalb)
    outeval.close()
    print "END"

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="takes as input a reference and a testset in \
    conll format and generates an evaluation set ready to be used by conlleval.txt evaluation script")
    parser.add_argument('-r','--refset',nargs=1,help="specify reference set")
    parser.add_argument('-t','--testset',nargs=1,help='specify your testset')
    parser.add_argument('-o','--outfile',nargs=1,help='specify file to save your evaluation dataset')
    parsed_arguments = parser.parse_args()
    if len(sys.argv)== 1:
        parser.print_usage()
        sys.exit(1)
    main(parsed_arguments)


