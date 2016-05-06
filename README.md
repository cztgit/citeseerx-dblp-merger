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

Known Issues:
 * We used to have a problem in which after indexing about 250,000, the program quits with an error message "ERROR while parsing DBLP file". Below is the error stack trace:

    at com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.handleStreamException(UnmarshallerImpl.java:470)
    at com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.unmarshal0(UnmarshallerImpl.java:448)
    at com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.unmarshal(UnmarshallerImpl.java:420)
    at main.SolrHandler.indexDblp(SolrHandler.java:113)
    at main.MainClass.main(MainClass.java:52)
Caused by: javax.xml.stream.XMLStreamException: ParseError at [row,col]:[1,1]
Message: JAXP00010001: The parser has encountered more than "64000" entity expansions in this document; this is the limit imposed by the JDK.
    at com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl.next(XMLStreamReaderImpl.java:596)
    at com.sun.xml.internal.stream.XMLEventReaderImpl.peek(XMLEventReaderImpl.java:276)
    at com.sun.xml.internal.bind.v2.runtime.unmarshaller.StAXEventConnector.handleCharacters(StAXEventConnector.java:164)
    at com.sun.xml.internal.bind.v2.runtime.unmarshaller.StAXEventConnector.bridge(StAXEventConnector.java:126)
    at com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.unmarshal0(UnmarshallerImpl.java:445)
    ... 3 more
 The work-around was to increase the default by specifying "-DentityExpansionLimit=2500000" as a VM argument.
 
 * Error messages like at certain documents:
 ERROR while processing file '/data/2016_merge-csx-dblp/CSXDataset_serial_v2-result/10.1.1.71.5577.xml'
	Server at http://localhost:8983/solr/collection1 returned non ok status:413, message:FULL head

This is not an error of Solr but Jetty, the servlet container. It happens when the input query is too long. The solution is to increase the default "requestHeaderSize" in etc/jetty.xml. I increased it to 8192 and the error disappears. The statement in jetty.xml looks like
>   <Call name="addConnector">
>       <Arg>
>           <New class="org.eclipse.jetty.server.bio.SocketConnector">
>             <Set name="host"><SystemProperty name="jetty.host" /></Set>
>             <Set name="port"><SystemProperty name="jetty.port" default="8983"/></Set>
>             <Set name="maxIdleTime">50000</Set>
>             <Set name="lowResourceMaxIdleTime">1500</Set>
**>             <Set name="requestHeaderSize">8192</Set>
>             <Set name="statsOn">false</Set>
>           </New>
>       </Arg>
>     </Call>

