# citeseerx-dblp-merger
This code is developed to match CiteSeerX against DBLP databases. The procedure
is to build a Solr index of DBLP metadata and match CiteSeerX metadata, e.g.,
title, author, year, venue, pages etc. against the index. 

To run the program, you need the following tools:
 1.  Apache Solr 4+
 2.  MySQL 5.5+
 3.  Java 1.7+
 4.  Eclipse IDE Juno+

and the following datasets
 1.  CiteSeerX database 
 2.  dblp.xml downloaded from 
     http://dblp.uni-trier.de/xml/
 3.  A file containing page numbers of each CiteSeerX papers (if page feature 
     is used for matching, otherwise, just an empty file named pages.txt)

To use the program, follow the steps below:

 1.  Import CiteSeerX database (only "citeseerx" database is required) to 
     a MySQL server.

 2.  Generate XML files by dumping metadata from CiteSeerX database
     using CSXDataset_serial_v2.java, assuming the outputs are saved to
     /data/CSXDataset_serial_v1-output/

 3.  Setup an instance of Apache Solr using the schema.xml file in 
     solr/conf/ folder. 

 4.  Import the codebase in Eclipse. Edit input/config.txt, set BUILD_INDEX 
     to true, making sure that DBLP_PATH is correct, run the program to get
     DBLP metadata indexed.

 5.  Edit input/config.txt and set BUILD_INDEX to false. Configure other 
     parameters and run. The results are saved in OUTPUT_FOLDER_PATH.

You may raise an issue on github and we will try to fix it as soon as possible.

If you use this codes for your project, we hope you to cite the following work:

Cornelia Caragea, Jian Wu, Alina Ciobanu, Kyle Williams, Juan Fernandez-Ramrez, Hung-Hsuan Chen, Zhaohui Wu and C. Lee Giles. "CiteSeerX: A Scholarly Big Dataset". In: Proceedings of the 36th European Conference on Information Retrieval (ECIR 2014), Amsterdam,Netherlands.

To get the bibtex, go to 
http://link.springer.com/chapter/10.1007%2F978-3-319-06028-6_26
and export citation under "reference tools".


