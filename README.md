# Efficient Range and Top-k Twin Subsequence Search in Time Series

## Overview
This repo contains the source code our paper:

- Georgios Chatzigeorgakidis, Dimitrios Skoutas, Kostas Patroumpas, Themis Palpanas, Spiros Athanasiou
and Spiros Skiadopoulos, “Efficient Range and Top-k Twin Subsequence Search in Time Series”, TKDE (under review).

It also contains the source code, JAR files and corresponding instructions for a large-scale experiment on the [UCR Archive](https://www.cs.ucr.edu/%7Eeamonn/time_series_data_2018/). 

## Execution
To run the experiment, first of all download the UCR Archive and extract the datasets at a folder of your preference. Note that there is a folder named "Missing_value_and_variable_length_datasets_adjusted", which must be removed first. Then clone the repo open a terminal and navigate to the ts_index folder. To run the experiment for each approach (TS-Index, iSAX, KV-Index or Sweepline), issue the following commands:

TS-Index:
```
java -cp "indices/*" src/mainapp/RunTSIndex.java arg1 arg2 arg3 arg4 arg5 arg6 arg7 "/path-to-UCR-archive/" "/path-to-index-store-location/" "path-to-results"
```
where arg1 is the query type (0 range, 1 kNN), arg2 is epsilon for range queries or k for kNN, arg3 is the minimum number of objects per node, arg4 is the maximum number of objects per node, arg5 is the number of segments, arg6 is the number of queries to run and arg7 is the subsequence length.

iSAX:
```
java -cp "indices/*" src/mainapp/RunISAX.java arg1 arg2 arg3 arg4 arg5 arg6 "/path-to-UCR-archive/" "/path-to-index-store-location/" "path-to-results"
```
where arg1 is the query type (0 range, 1 kNN), arg2 is epsilon for range queries or k for kNN, arg3 is the maximum number of objects per node, arg4 is the number of segments (wordlength), arg5 is the number of queries to run and arg6 is the subsequence length.

KV-Index:
```
java -cp "indices/*" src/mainapp/RunKVIndex.java arg1 arg2 arg3 "/path-to-UCR-archive/" "/path-to-index-store-location/" "path-to-results"
```
where arg1 is epsilon for range queries, arg2 is the number of queries to run and arg3 is the subsequence length.

Sweepline
```
java -cp "indices/*" src/mainapp/RunSweepLine.java arg1 arg2 arg3 "/path-to-UCR-archive/" "path-to-results"
```
where arg1 is epsilon for range queries, arg2 is the number of queries to run and arg3 is the subsequence length.

## Results on UCR Archive
The results we obtained for epsilon=0.25 can be found in the following tables:

* [Range query response time](https://github.com/chgeorgakidis/ts_index/blob/main/ResponseTimeResults.md)
* [Index build time](https://github.com/chgeorgakidis/ts_index/blob/main/BuildTimeResults.md)
* [Index size](https://github.com/chgeorgakidis/ts_index/blob/main/IndexSizeResults.md)
