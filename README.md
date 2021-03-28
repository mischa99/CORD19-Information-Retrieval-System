# CORD19-Information-Retrieval-System

The goal of the project is to create a search engine using Apache Lucene for
the COVID-19 Open Research Dataset (CORD-19, https://www.kaggle.com/allen-institute-for-ai/CORD-19-research-challenge). The collection contains
141.000 json files with metadata about the papers in the dataset. Two applica-
tions are developed, the first is used to create a Lucene Index, and the second
is used to run multiple types of queries on the index, such as   
• Free Text Query,  
• Boolean Query,  
• Phrase Query,  
• Search by facets,  
• drill down facet query.

## Usage
The code can be run in the command line. Indexing:


![Usage schema](./images/indexing.jpg)  
The user is asked to provide the path to the documents
directory, as well as paths to where to store the index and side index. After
that the indexing process begins.  

![Searching schema](./images/boolean search.jpg)  
More examples can be found in the documentation

## Technologies
Project is created with:
* Apache Lucene version: 8.6.2
* json-simple version: 1.1.1
* maven plugin version: 3.8.1
