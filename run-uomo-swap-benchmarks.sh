#!/bin/bash

gradle jar

# python run-units-on-corpus.py --corpus-file=projects.yml
time python run-units-on-corpus.py --corpus-file uomo-swap-benchmarks.yml