

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class Main {


    public static void main(String[] args) {

        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(System.in,
                StandardCharsets.UTF_8));

        String indexPath = null;
        String facetPath = null;
        try {
            System.out.println("Path to index:");
            indexPath = Searching.getUserInput(in);
            System.out.println("Path to sidecar index (containing the facets):");
            facetPath = Searching.getUserInput(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Searching search = new Searching(indexPath, facetPath);


        System.out.println("What type of search do you want to do?");
        System.out.println("Type -t for free search by field. Type -b for boolean search. " +
                "Type -p for phrase proximity search. Type -f for facets search or -df for a drill down facets search:");

        String operation = null;
        String arg = null;
        boolean searchBySize = false;
        try {
            operation = search.getUserInput(in);
            System.out.println("Do you want the results to be sorted by size (default=similarity score)?[y/n]");
            arg = search.getUserInput(in);
            if(arg==null) {
                System.out.println("Type 'y' if sort by size and 'n' if not sort by size");
                arg = search.getUserInput(in);
            }
            if(arg==null) System.exit(0);
            searchBySize = arg.equals("y");
         } catch (IOException e) {
            e.printStackTrace();
        }

        boolean searchByRelevance = false;
        //find out if facet labels should contain relevance or size as value
        if(operation.equals("-f") || operation.equals("-df")) {
            try {
                System.out.println("Do you want the facet values ordered by size (#n of docs with that value) or relevance (sum of similarity score for all docs with that value)?[s/r]");
                arg = search.getUserInput(in);
                if (arg == null) {
                    System.out.println("Type 's' for size or 'r' for relevance");
                    arg = search.getUserInput(in);
                }
                if (arg == null) System.exit(0);
                searchByRelevance = arg.equals("r");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

            switch (operation) {
            case "-t":
                search.fieldIndexSearch(searchBySize);
                break;

            case "-b":
                search.booleanIndexSearch(searchBySize);
                break;


                case "-p":
                    search.phraseSearch(searchBySize);
                    break;

            case "-f":
                try {
                    search.facetSearch(searchByRelevance, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "-df":
                try {
                    search.facetDrillDownSearch(searchByRelevance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;


                default:
                System.out.println("no valid operation selected");
                printHelp();

        }

    }


    private static void printHelp() {
        System.out.println("-f: let's you do a search by a field you type in the next step. Can be one of the following fields: " +
                "title, abstract, body, filename, author, country, institution. The query can be a single term or a phrase (only for title, abstract, body)");
        System.out.println("-b: let's you do a boolean search by the two fields title and country");
        System.out.println("-p: let's you do a proximity search by any field, that is, searching for a phrase and a possible distance between the words.");
        System.out.println("-f: let's you do a facet search, possible facets: Author, Country, Institution");
        System.out.println("-df: let's you do a drill down search by facets to explore their contents (possible facets: Author, Country, Institution). " +
                "You can provide exact values for the facets, e.g. Country = Poland shows all documents being in that facet.");
        System.exit(1);
    }
}
