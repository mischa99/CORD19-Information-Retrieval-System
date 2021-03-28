import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyFacetSumValueSource;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class Searching {

   private  String indexPath = "/Users/mikhail/index";
   private  String facetPath = "/Users/mikhail/facets";
   private final int RETURN_SIZE = 20; //number of documents returned for a query

   private Similarity similarity; //similarity function to compare document and query
   private IndexSearcher searcher;
   private IndexReader indexReader;
   private TaxonomyReader taxoReader; //to read side index containing facets
   private FacetsConfig fconfig;
   private BufferedReader in =  new BufferedReader(new InputStreamReader(System.in,
                            StandardCharsets.UTF_8)); //read user input


   public Searching(String _indexPath, String _facetPath) {
       //TO-DO: SET INDEX PATH
       indexPath = _indexPath;
       facetPath = _facetPath;
       //similarity = new ClassicSimilarity();
       similarity = new BM25Similarity();

       fconfig = new FacetsConfig();
       //configure the facets the same way as during indexing
       fconfig.setMultiValued("Author", true);
       fconfig.setMultiValued("Institution", true);
       fconfig.setMultiValued("Country", true);
   }

    /**
     * initilizes the IndexReader and IndexSearcher, sets Similarity
     * @throws IOException
     */
    public void setUpReaderAndSearcher() throws IOException {
        indexReader = DirectoryReader.open(FSDirectory.open(
                Paths.get(indexPath)
        ));
        searcher = new IndexSearcher(indexReader);
        //assign similarity calculation method
        searcher.setSimilarity(similarity);
    }

    /**
     * Performs a free text query on the field given by the user input
     * @param sortBySize if we want to change the result order to be sorted by title
     */
    public void fieldIndexSearch(boolean sortBySize) {

        try{

            setUpReaderAndSearcher();

            while(true) {
                System.out.println("Field?: ");

                String input_field = getUserInput(in);
                String search = "";
                //specify query for size field later on, all other fields specify query now
                if(!input_field.equals("size")) {
                    System.out.println("Query?: ");
                    search = getUserInput(in);
                }

                //create lucene query from user input
                Query query;
                try {
                    query = getQuery(input_field, search);
                } catch (ParseException e) {
                    System.out.println("Query String Error");
                    continue;
                }
                if(query==null) {
                    System.out.println("Field does not exist.");
                    break;
                }

                if(sortBySize==false) {
                    //search index with query
                    TopDocs results = searcher.search(query, RETURN_SIZE);
                    if (input_field.equals("size")) {
                        displayResults(results, false, true);
                    } else {
                        displayResults(results, false, false);
                    }
                } else {
                    //search index and sort by title
                    TopFieldDocs results2 = sortBySizeSearch(query);
                    displayResults(results2, true, true);
                }

                if (getUserInput(in)== null) {
                    break;
                }

            }

            indexReader.close();

        } catch (IOException e) {
            try{
                indexReader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    /**
     * Performs a boolean query on the fields title and country, operator: AND
     * @param sortBySize sort result docs by size or similarity score
     */
    public void booleanIndexSearch(boolean sortBySize) {

        try{

            setUpReaderAndSearcher();

            while(true) {
                System.out.println("Title: ");
                String search1 = getUserInput(in);
                System.out.println("AND");
                System.out.println("Country: ");
                String search2 = getUserInput(in);

                if(search1 == null || search2 == null) break;

                //create lucene query from user input
                Query query;
                try {
                    query = getBooleanQuery("title", search1, "country", search2);
                } catch (ParseException e) {
                    System.out.println("Query String Error");
                    continue;
                }
                if (query == null) {
                    System.out.println("Field does not exist.");
                    break;
                }

                if (sortBySize == false) {
                    //search index with query
                    TopDocs results = searcher.search(query, RETURN_SIZE);
                    displayResults(results, false,false);
                }
                else {
                    //search index and sort by size
                    TopFieldDocs results2 = sortBySizeSearch(query);
                    displayResults(results2, true, true);
                }

                if (getUserInput(in)==null) {
                    break;
                }

            }

            indexReader.close();

        } catch (IOException e) {
            try{
                indexReader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    /**
     * Performs a PhraseQuery
     * @param sortBySize sort result docs by size or similarity score
     */
    public void phraseSearch(boolean sortBySize) {
        //TO-DO think how to use score variable (have default false or have as parameter)
        try{
            setUpReaderAndSearcher();
            while(true) {
                System.out.println("Field?: ");
                String input_field = getUserInput(in);

                System.out.println("Query?: ");
                String search = getUserInput(in);

                if(input_field == null || search == null) break;

                System.out.println("Distance between each word in phrase?");
                int slop = (int) getUserLongInput();

                //create lucene query from user input
                Query query;
                try {
                    query = getPhraseQuery(input_field, search, new EnglishAnalyzer(), slop);;
                } catch (IOException e) {
                    System.out.println("Query String Error");
                    continue;
                }
                if(query==null) {
                    System.out.println("Field does not exist.");
                    break;
                }

                if(input_field==null) {
                    System.out.println("Field does not exist.");
                    break;
                }

                if(sortBySize==false) {
                    //search index with query
                    TopDocs results = searcher.search(query, RETURN_SIZE);
                    displayResults(results, true,false);
                } else {
                    //search index and sort by size
                    TopFieldDocs results2 = sortBySizeSearch(query);
                    displayResults(results2, true, true);
                }

                if (getUserInput(in) == null) {
                    break;
                }

            }

            indexReader.close();

        } catch (IOException e) {
            try{
                indexReader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }


    /**
     * Do a search that returns also the facet values besides the matching documents
     * @param searchByRelevance if the facet labels show their number of appearance or relevance as their value
     * @param score if the score is displayed for the retrieved docs
     * @throws IOException
     */
    public void facetSearch(boolean searchByRelevance, boolean score) throws IOException {

        Directory taxoDir = FSDirectory.open(Paths.get(facetPath));
        taxoReader = new DirectoryTaxonomyReader(taxoDir);
        setUpReaderAndSearcher();

        while(true) {
            System.out.println("Field?: ");
            String input_field = getUserInput(in);

            System.out.println("Query?: ");
            String search = getUserInput(in);

            //create lucene query from user input
            Query query;
            try {
                query = getQuery(input_field, search);
            } catch (ParseException e) {
                System.out.println("Query String Error");
                continue;
            }
            if (query == null) {
                System.out.println("Field does not exist.");
                break;
            }
            FacetsCollector fc = (searchByRelevance) ? new FacetsCollector(true) : new FacetsCollector();
            TopDocs tdc = FacetsCollector.search(searcher, query, RETURN_SIZE, fc);

            displayFacetsResults(tdc, fc, searchByRelevance, score);

            if (getUserInput(in) == null) {
                break;
            }

        }
        taxoReader.close();
    }

    /**
     * Performs a drill down search through the facets specified by user
     * @param searchByRelevance if the facet labels show their number of appearance or relevance as their value
     * @throws IOException
     */
    public void facetDrillDownSearch(boolean searchByRelevance) throws IOException {

        setUpReaderAndSearcher();

        Directory taxoDir = FSDirectory.open(Paths.get(facetPath));
        taxoReader = new DirectoryTaxonomyReader(taxoDir);

        Query q1 = new MatchAllDocsQuery();
        DrillDownQuery ddq = new DrillDownQuery(fconfig, q1);

        System.out.println("Following Facets are available: Author / Institution / Country" );


        System.out.println("Please provide the facets and their specific values you want to search. Type NO if finished");
        String facet = ""; String value = "";

        while(true) {
            System.out.println("Facet?: ");
            facet = getUserInput(in);
            if(facet.equals("NO")) break;
            System.out.println("Value?: ");
            value = getUserInput(in);
            if(value.equals("NO")) break;
            System.out.println(facet + " " + value);
            ddq.add(facet, value); //add facet restriction to drill down query
        }

        System.out.println("Filtering query[ "+ddq.toString() + "]");

        DrillSideways ds = new DrillSideways(searcher, fconfig, taxoReader);
        DrillSideways.DrillSidewaysResult dsresult = ds.search(ddq, RETURN_SIZE);
        System.out.println("drill sideways hits: " + dsresult.hits.totalHits);
        //display facet result
        ScoreDoc [] sc = dsresult.hits.scoreDocs;
        if (sc.length>0) {
            System.out.println(dsresult.facets.getAllDims(RETURN_SIZE).toString());
        }

        if (getUserInput(in) == null) {
            //break;
        }

        taxoReader.close();

    }


    /**
     * A function to print the results of a facet search in the CLI. Prints either how many times each label appears or the relevance for each label of all facet categories.
     * @param results lucene search results
     * @param fc
     * @param relevance if true, then print mean similarity score for each label of a facet (otherwise just how many times the label appears in the result)
     * @param score
     * @throws IOException
     */
    private void displayFacetsResults(TopDocs results, FacetsCollector fc, boolean relevance, boolean score) throws IOException {

        displayResults(results,score,false);

        Facets facets = (relevance) ? new TaxonomyFacetSumValueSource(taxoReader, fconfig, fc, DoubleValuesSource.SCORES)
            : new FastTaxonomyFacetCounts(taxoReader, fconfig, fc);

        //Iterate over obtained facets
        System.out.println("Facet Score Summary");
        List<FacetResult> allDims = facets.getAllDims(100);
        System.out.println("Total categories: " + allDims.size());

        for (FacetResult fr : allDims) {
            System.out.println("Category " + fr.dim);
            for(LabelAndValue lv : fr.labelValues) {
                System.out.println("    Label: " + lv.label + ", value (#n)->" +lv.value);
            }
        }

        /**
        if (score = true) {
            for(ScoreDoc scoreDoc : results.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                System.out.println("Doc Score ->" + scoreDoc.score +
                        "::" + doc.get());
            }
        }
         */


    }

    /**
     * A function to print the Search results in the CLI. Shows filename, title, and if true, score and size for each retrieved document
     * @param results Lucene search results
     * @param score prints doc similarity score if true
     * @param size prints doc size if true
     * @throws IOException
     */
    private void displayResults(TopDocs results, boolean score, boolean size) throws IOException {
        ScoreDoc[] hits = results.scoreDocs;
        TotalHits totalHits = results.totalHits;
        long numTotalHits = totalHits.value;
        System.out.println(numTotalHits + " document(s) found");

        for(int i=0; i<hits.length; i++){
            Document doc = searcher.doc(hits[i].doc);
            String title = doc.get("title_bool"); //????
            String filename = String.valueOf(doc.get("filename"));

            System.out.println("-------------------------------------");
            System.out.println("Filename: " + filename);

            System.out.println("Title: " + title);
            System.out.println();

            if(score==true) {
                System.out.println("Score= " + hits[i].score);
            }
            if (size==true) {
                System.out.println("Size= " + doc.get("size"));
            }

        }
    }

    /**
     * allows a search that sorts the results by the field size
     * @param q Lucene Query Object
     * @throws IOException
     * @return Top Documents that match the Query
     */
    private TopFieldDocs sortBySizeSearch(Query q) throws IOException {
       //sort by string value in decreasing order (reverse = true)
       SortField sf = new SortField("size", SortField.Type.LONG, true);

       long num = 0;
        sf.setMissingValue(num); //value if docs does not contain the field

        Sort order = new Sort(sf);

        TopFieldDocs tfds;
        tfds = searcher.search(q, RETURN_SIZE, order, true);

        System.out.println("Order by the field: " + tfds.fields[0].getField());
        return tfds;
    }

    /**
     * Create own Query Objects based on the searched field of the lucene document
     * @param field lucene document field
     * @param search search query for field
     * @return
     */
    private Query getQuery(String field, String search) throws ParseException, IOException {
        Query res = null;
        if (field.equals("filename") || field.equals("institutions")) {
            if(field.equals("institutions")) {
                search = search.toLowerCase(); //lowercase because the field was when indexed
                Term term = new Term(field, search);
                res = new TermQuery(term);
            }
            else { //filename query
                Term term = new Term(field, search);
                res = new TermQuery(term);
            }
        }
        else if(field.equals("size")) {
            System.out.println("Do you want to search for documents having the exact size, or search for documents in a range of sizes? Type [exact/range]");
            String arg = getUserInput(in);

            if (arg.equals("exact")) {
                System.out.println("What is the exact value for doc size?");
                long val = getUserLongInput();
                res = LongPoint.newExactQuery(field, val);
            }
            if (arg.equals("range")) {
                System.out.println("What is the lower bound for doc size?");
                long low = getUserLongInput();
                System.out.println("What is the upper bound for doc size?");
                long up = getUserLongInput();
                res = LongPoint.newRangeQuery(field, low, up);
            }
        }
        else if(field.equals("author")) { //Standard Analyzer Text Field
            QueryParser parser = new QueryParser(field, new StandardAnalyzer());
            res = parser.parse(search);
        }
        else if(field.equals("country")) { //CountryAnalyzer TextField
            QueryParser parser = new QueryParser(field, new CountryAnalyzer());
            res = parser.parse(search);
        }

        else if (field.equals("title") || field.equals("abstract") || field.equals("body")){ //EnglishAnalyzer TextFields
            res = getPhraseQuery(field, search, new EnglishAnalyzer(), 0); //default slop 0 for free text field query
        }

        return res;
    }

    /**
     * Creates a Lucene PhraseQuery Object
     * @param field field to search
     * @param search query for this field
     * @param analyzer Analyzer used at Indexing Time for the field
     * @param slop distance between each word in query
     * @return PhraseQuery Object
     * @throws IOException
     */
    private PhraseQuery getPhraseQuery(String field, String search, Analyzer analyzer, int slop) throws IOException {
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        builder.setSlop(slop); //words of phrase follow one another, if 0>, then it allows to contain x words in between

        TokenStream stream = analyzer.tokenStream(null, search);
        stream.reset();
        while(stream.incrementToken()) {
            CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);
            String token = charTermAttribute.toString();
            Term term = new Term(field, token);
            builder.add(term); //add Terms to Phrase Query Object
        }
        stream.end();
        stream.close();

        return builder.build();
    }

    /**
     * Creates a Lucene BooleanQuery Object
     * @param field1 field to be searched
     * @param search1 query for this field
     * @param field2
     * @param search2
     * @return BooleanQuery Object
     * @throws IOException
     * @throws ParseException
     */
    private BooleanQuery getBooleanQuery(String field1, String search1, String field2, String search2) throws IOException, ParseException {
        ArrayList<Document> documents;

        Query q1 = getQuery(field1, search1);
        Query q2 = getQuery(field2 ,search2);

        BooleanClause bc1 = new BooleanClause(q1,BooleanClause.Occur.MUST);
        BooleanClause bc2 = new BooleanClause(q2,BooleanClause.Occur.MUST);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(bc1);
        builder.add(bc2);

        return builder.build();
    }

    /**
     * gets Command Line Input
     * @param in
     * @return
     * @throws IOException
     */
    public static String getUserInput(BufferedReader in) throws IOException {
        String input = in.readLine();

        if(input == null || input.length()==-1) {
            return null;
        }
        //eliminate whitespace at beginning and end
        input= input.trim();

        if(input.length()==0) {
            return null;
        }
        return input;
    }

    public long getUserLongInput() throws IOException {
        return Long.parseLong(this.in.readLine());
    }
}
