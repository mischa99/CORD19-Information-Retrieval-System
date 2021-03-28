import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONObject;


import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class creates a lucene index using the @ParseJSON class to get document field values
 */
public class SimpleIndexing {

   private String indexPath = "/Users/mikhail/index";
   private String facetPath = "/Users/mikhail/facets";
   private String docPath = "/Users/mikhail/Downloads/archive/document_parses/pdf_json/";
   private Map<String, Analyzer> analyzerPerField;
   private PerFieldAnalyzerWrapper analyzer = null;
   private IndexWriter writer;
   private DirectoryTaxonomyWriter taxoWriter; //writes in the sidecar index where facets will be located
   private FacetsConfig fconfig;
   private Similarity similarity;


   public SimpleIndexing(String dir, String idxPath, String fctPath) throws IOException {

       indexPath = idxPath;
       docPath = dir;
       facetPath = fctPath;

       analyzerPerField = new HashMap<String, Analyzer>();
       //StringFields, Analyzer parameter doesn't matter
       analyzerPerField.put("title_exact", new WhitespaceAnalyzer());
       analyzerPerField.put("institution",new WhitespaceAnalyzer());
       //TextFields, define analyzer used by IndexWriter on this field
       analyzerPerField.put("title", new EnglishAnalyzer());
       analyzerPerField.put("abstract", new EnglishAnalyzer());
       analyzerPerField.put("body", new EnglishAnalyzer());
       analyzerPerField.put("author", new StandardAnalyzer()); //see if results get better w/ standard
       analyzerPerField.put("country", new CountryAnalyzer());


       similarity = new ClassicSimilarity();
       analyzer = new PerFieldAnalyzerWrapper(new WhitespaceAnalyzer(), analyzerPerField); //StandardAnalyzer will be used as a default
       configureIndex();
       configureFacetsIndex();
   }

   public void configureIndex() throws IOException {

      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
      iwc.setRAMBufferSizeMB(64); // better performance, default is 16 MB
      iwc.setSimilarity(similarity);

      //creates a new index each time it gets executed
      iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
      //add docs to existing index
      //iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);

      //Localize the index
      Directory dir = FSDirectory.open(Paths.get(indexPath));

      //Create index
      writer = new IndexWriter(dir,iwc);
   }

   public void configureFacetsIndex() throws IOException {
        //locate facet index
       Directory taxoDir = FSDirectory.open(Paths.get(facetPath));

       fconfig = new FacetsConfig();
       //configure the facets
       fconfig.setMultiValued("Author", true);
       fconfig.setMultiValued("Institution", true);
       fconfig.setMultiValued("Country", true);

       taxoWriter = new DirectoryTaxonomyWriter(taxoDir, IndexWriterConfig.OpenMode.CREATE);

   }

   public void indexDocuments() throws IOException {

       File filepath = new File(docPath);
       File[] listOfFiles = null;
       //make sure filepath contains a directory
       if(filepath.isDirectory()) {
           listOfFiles = filepath.listFiles();
       }

       //create new Field Type to work with author data
       FieldType authorType = new FieldType();
       authorType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
       authorType.setStored(true);
       authorType.setOmitNorms(true);
       authorType.setTokenized(false);
       authorType.setStoreTermVectors(true);

       int i=1;

           for (File file : listOfFiles) {

               //if(i==1000) break;

               //FIELD VALUES
               String TITLE = "";
               String ABSTRACT= "";
               String BODY_TEXT= "";
               //save each country or institution ONCE per document
               Set<String> COUNTRIES= new HashSet<String>();
               Set<String> INSTITUTIONS= new HashSet<String>();
               long SIZE = file.length();



               ArrayList<String> AUTHORS = new ArrayList<String>();
               HashMap<String,String> AUTHORS_INSTITUTIONS = new HashMap<>();
               System.out.println(i + " documents indexed!");
               i++;


               //EXTRACT FIELD VALUES FROM JSON FILE
               ParseJSON parser = new ParseJSON();
               parser.setJSONObject(file);

               TITLE = parser.parseTitle();

               AUTHORS_INSTITUTIONS = parser.parseAuthors();
               for (String key : AUTHORS_INSTITUTIONS.keySet()) {
                   AUTHORS.add(key);
               }
               for (String value : AUTHORS_INSTITUTIONS.values()) {
                   INSTITUTIONS.add(value);
               }
               ABSTRACT  = parser.parseAbstract();
               BODY_TEXT = parser.parseBodyText();
               COUNTRIES = parser.parseCountry();



               //DEBUG ParseJson results
               /**
                System.out.println(TITLE);
               System.out.println(ABSTRACT);
                System.out.println(BODY_TEXT);

                for (String a : AUTHORS) {
                System.out.println(a);
                }

                for (String c : COUNTRIES) {
                System.out.println(c);
                }
                for (String c : INSTITUTIONS) {
                System.out.println(c);
                }
                **/


               //create lucene document
               Document doc = new Document();


               //save values to fields in lucene document
               doc.add(new StringField("filename", file.getName(), Field.Store.YES)); //not tokenized
                //index file size
               doc.add(new LongPoint("size",SIZE)); //for range queries
               doc.add(new StoredField("size",SIZE)); //for storing val in index
               doc.add(new NumericDocValuesField("size", SIZE)); //doc value for DIFFERENT SORTING OPTIONs of retrieval results


               doc.add(new StringField("title_exact", TITLE, Field.Store.YES)); //not tokenized for showing full title in results and boolean queries
               doc.add(new TextField("title", TITLE, Field.Store.NO));
               doc.add(new TextField("abstract", ABSTRACT, Field.Store.NO));
               doc.add(new TextField("body", BODY_TEXT, Field.Store.NO));
               for (String country : COUNTRIES) {
                   doc.add(new TextField("country", country, Field.Store.NO));
               }
               for (String inst : INSTITUTIONS) {
                   inst = inst.toLowerCase();
                   doc.add(new StringField("institution", inst, Field.Store.NO));
               }

               //add authors
               for (String author : AUTHORS) {
                   doc.add(new TextField("author", author,Field.Store.YES));
               }



               //ADD FACETS TO INDEX
               for (String country : COUNTRIES) {
                   if (country!=null && !country.isEmpty()) doc.add(new FacetField("Country", country));
               }
               for (String inst : INSTITUTIONS) {
                   inst = inst.toLowerCase();
                   if (inst!=null && !inst.isEmpty()) doc.add(new FacetField("Institution", inst));
               }

               //add authors
               for (String author : AUTHORS) {
                   if (author!=null&& !author.isEmpty()) doc.add(new FacetField("Author", author));
               }




               //insert the lucene document to the index
               //writer.addDocument(doc);

               //index the facets
               writer.addDocument(fconfig.build(taxoWriter, doc));

           }

   }

   public void close() {
       try{
           taxoWriter.commit();
           writer.commit();


           taxoWriter.close();
           System.out.println("Taxonomy Writer closed");
           writer.close();
           System.out.println("Index Writer closed");
       }
       catch (IOException e) {
           System.out.println("Error closing the index.");
       }
   }
}
