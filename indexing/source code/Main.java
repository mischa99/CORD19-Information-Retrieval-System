import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {

        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(System.in,
                StandardCharsets.UTF_8));
        System.out.println("Welcome to the Indexing program. You can create an lucene index.");
        String dir = ""; String indexPath = ""; String facetPath= "";
        try {
            System.out.println("Please provide the path to the documents directory: ");
            dir = getUserInput(in);
            System.out.println("Please provide the path where the index should be stored: ");
            indexPath = getUserInput(in);
            System.out.println("Please provide the path where the side index with the facets should be stored: ");
            facetPath = getUserInput(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            SimpleIndexing index = new SimpleIndexing(dir, indexPath, facetPath);
            index.indexDocuments();
            index.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String getUserInput(BufferedReader in) throws IOException {
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
}
