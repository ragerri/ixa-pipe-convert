#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
It creates an opennlp formatted POS dataset from ancora-dep corpus

Rodrigo Agerri (rodrigo.agerri@ehu.es)

19/0/2013
"""

import argparse
import os
import re

def clean_wsj(infile):
    inb = [line for line in infile if line.startswith("#") == False]
    inb = [line.replace(" ","\t") for line in inb]
    inb = [line.replace("\n","\n\t\n\t\n\t\n") for line in inb]
    inb = [line.replace("__elliptic__","") for line in inb]
    inb = [line.replace("_","#") for line in inb]
    inb = [re.sub("#_","",line) for line in inb]
    inb = [line.split("\t") for line in inb]
    inb = [[line[1],line[3]] for line in inb]
    inb = ["_".join(line) for line in inb]
    inb = [line.replace("\n_\n","\n") for line in inb]
    inc = " ".join(inb)
    inc = re.sub(r'\n ','\n',inc)
    return inc

def main(arguments):
    "removing first line of files ..."
    if arguments.dir:
        for folder in arguments.dir:
            print "processing folder: {0} ...".format(folder)
            for dirpath,dirs,docs in sorted(os.walk(folder)):
                for doc in sorted(docs):
                    arguments.file.append(os.path.join(dirpath,doc))

    for elem in arguments.file:
         pathname,basename = os.path.split(elem)
         basefile,extension = os.path.splitext(basename) # get extension of files
         if arguments.ext:
             infile = open(elem,'r').readlines()
             infile1 = clean_wsj(infile)
             outfile = open(basefile+arguments.ext,'w')
             for line in infile1:
                 outfile.write(line)
                 outfile.close()
         else:
             print "processing file: {0} ...".format(elem)
             infile = open(elem,'r').readlines()
             infile1 = clean_wsj(infile)
             outfile = open(elem + '.open','w')
             for line in infile1:
                 outfile.write(line)
             outfile.close()

    print "END"

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="creates opennlp POS dataset from WSJ pos corpus")
    parser.add_argument('file',nargs='*',help="processes one or more files given as positional argument")
    parser.add_argument('--dir',nargs='*',help="processes one or more dirs listed after --dir")
    parser.add_argument('--ext',nargs="*",help="specify the extensions of files to be processed: .txt, .csv ...")
    parsed_arguments = parser.parse_args()
    main(parsed_arguments)


