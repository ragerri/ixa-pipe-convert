#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Script to create training data from CoNLL training set:

1. To obtain training data for tokenization in OpenNLP format, use the -tok option.

2. To obtain sentence segmentation training data in OpenNLP format
    2.1 Use the -seg option
    2.2 Run tokenizer/detokenizer.perl script (from Moses SMT system) to detokenize text.

Example Usage: python conll_tok_training.py (-tok|-seg) file.txt

Rodrigo Agerri (rodrigo.agerri@ehu.es)

22/11/2012
"""

import argparse
import re
import sys

def clean_conll(testset):
    inb = [line.split(" ") for line in testset]
    inb = [line[0] for line in inb]
    # remove ,.:"() if at the end of a line
    inb = [re.sub(r'(?<=[a-z0-9])(,|\.+|:|\"|\(|\))$','',line) for line in inb]
    # substitute .. with . at the of the line if preceded by a capital character
    tok = [re.sub(r'(?<=[A-Z])\..$','.',line) for line in inb]
    tok = [re.sub(r'=+','=',line) for line in tok]
    # change ` to '
    tok = [re.sub(r'\`','\'',line) for line in tok]
    # change '' to "
    tok = [re.sub(r'\'\'','\"',line) for line in tok]
    return tok

def evalita_clean(testset):
    clean = re.sub(r'; \n','; ',testset)
    clean = re.sub(r': \n',': ',clean)
    clean = re.sub(r'(?<!(\.|\!|\?|-)) \n','.\n',clean)
    return clean

def seg_training(testset):
    tok = clean_conll(testset)
    tok = " ".join(tok)
    tok = evalita_clean(tok)
    tok = tok.split("\n")
    tok = [line.strip() for line in tok]
    return tok

def tok_training(testset):
    tok = seg_training(testset)
    # substitute space with <SPLIT> if followed by [.,:;?!)]}]
    tok = [re.sub(r' (?=(\.|,|:|;|\?|\!|\)|\]|\}))','<SPLIT>',line) for line in tok]
    # substitute space with <SPLIT> if preceded by ['({<]]
    tok = [re.sub(r'(?<=[\'¿¡\(\{\<\\[]) ','<SPLIT>',line) for line in tok]
    # substitute space between quotes " words " with <SPLIT>words<SPLIT>
    tok = [re.sub(r'(\") ([^\"]+) (\")','\g<1><SPLIT>\g<2><SPLIT>\g<3>',line) for line in tok]
    tok = [re.sub(r'(\') ([^\']+) (\')','\g<1><SPLIT>\g<2><SPLIT>\g<3>',line) for line in tok]
    # substitute space between words and « with <SPLIT>
    tok = [re.sub(r'« ','«<SPLIT>',line) for line in tok]
    tok = [re.sub(r' »','<SPLIT>»',line) for line in tok]
    return tok

def main(arguments):
    for elem in arguments.file:
        print "processing file: {0} ...".format(elem)
        infile = open(elem,'r').readlines()
        if arguments.segmented:
            newb = seg_training(infile)
            for i in range(0,len(newb),10):
                newb[i] = newb[i] + '\n'
            newb = "\n".join(newb)
            #newb = re.sub(r'-\n','-\n\n',newb)
        if arguments.tokenized:
            newb = tok_training(infile)
            newb = "\n".join(newb)
        if arguments.outfile:
            outtok = open(arguments.outfile[0],'w')
            print "saving to file: {0} ".format(arguments.outfile[0])
        elif arguments.outfile == None and arguments.tokenized:
            outtok = open(elem + '.tok','w')
            print "saving to file: {0} ".format(elem + '.tok')
        elif arguments.outfile == None and arguments.segmented:
            outtok = open(elem + '.seg','w')
            print "saving to file: {0} ".format(elem + '.seg')
        outtok.write(newb)
        outtok.close()
    print "END"

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="processes conll format datasets to generate \
     training sets for Apache OpenNLP tokenizer and sentence segmenter. It also helps to generate \
     a cleaner version (without tokenizing errors) of a conll format testset.")
    parser.add_argument('file',nargs='*',help="processes one or more files given as positional \
    argument")
    parser.add_argument('-o','--outfile',nargs=1,help='specify output file; if this option is \
     not specified, output file will default to inputfile + option chosen')
    parser.add_argument('-tok','--tokenized',help='generate tokenized training set for training \
    tokenizer with Apache OpenNLP',action='store_true')
    parser.add_argument('-seg','--segmented',help='generate sentence segmented training set for \
    training sentence segmenter with Apache OpenNLP',action='store_true')
    parsed_arguments = parser.parse_args()
    if len(sys.argv)== 1:
        parser.print_usage()
        sys.exit(1)
    main(parsed_arguments)


