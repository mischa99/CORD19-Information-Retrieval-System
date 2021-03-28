import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * A class to parse fields of interest from a JSON file using the JSON.simple library
 * Used for the pdf_json files of the CORD-19 Dataset.
 * Retrieves the following fields:
 * - title
 * - authors full name
 * - authors institution
 * - abstract
 * - body text (full text)
 *
 * create an instance of the class and call setJSONObject method to obtain the result
 */
public class ParseJSON
{
    private JSONObject jsonObject = null;

    public ParseJSON () { }

    /**
     * sets given json file as jsonObject
     * @param file
     */
    public  void setJSONObject(File file) {
        try (FileReader reader = new FileReader(file))
        {
            //JSON parser object to parse read file
            JSONParser jsonParser = new JSONParser();

            //Read JSON file
             Object obj =  jsonParser.parse(reader);
             jsonObject = (JSONObject) obj;


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (jsonObject==null) {
            System.out.println("Could not create JSON object from file");
            System.exit(1);
        }

    }

    public  String parseTitle() {
        //Get metadata object within list
        JSONObject metadata = (JSONObject) jsonObject.get("metadata");

        //GET TITLE
        String title = (String) metadata.get("title");
        return title;
    }

    /**
     * gets Author names and institutions from the JSON file
     * @return an ArrayList; Index 0: ArrayList(String) containing author names; Index 1: HashMap(String, String) containing authors and their institutions
     */
    public HashMap<String, String> parseAuthors() {
        //Get metadata object
        JSONObject metadata = (JSONObject) jsonObject.get("metadata");

        //create objects to append & save values in
        ArrayList<String> AUTHORS = new ArrayList<String>();
        HashMap<String,String> AUTHORS_INSTITUTIONS = new HashMap<>();

        JSONArray authors = (JSONArray) metadata.get("authors");
        Iterator<JSONObject> iterator = authors.iterator();
        while (iterator.hasNext()) {
            JSONObject first_author = iterator.next();

            String first = (String) first_author.get("first");

            JSONArray mid = (JSONArray) first_author.get("middle");
            String middle = "";
            if(!(mid.isEmpty()) && mid.get(0) != null) {
                middle = (String) mid.get(0);
            }

            String last = (String) first_author.get("last");

            String name = first + " " + middle + " " + last;
            AUTHORS.add(name);

            // get the institution
            JSONObject affiliation = (JSONObject) first_author.get("affiliation");
            String institution = "";
            if(!(affiliation.isEmpty()) && affiliation.get("institution") != null) {
                institution = (String)  affiliation.get("institution");
            }
            AUTHORS_INSTITUTIONS.put(name,institution); //add author name and  his institution
        }
        return AUTHORS_INSTITUTIONS;
    }

    public Set<String> parseCountry() {


        Set<String> countries = new HashSet<String>();

        //Get metadata object
        JSONObject metadata = (JSONObject) jsonObject.get("metadata");
        JSONArray authors = (JSONArray) metadata.get("authors");

        Iterator<JSONObject> iterator = authors.iterator();
        while (iterator.hasNext()) {
            JSONObject author = iterator.next();
            // get the location of the author
            JSONObject affiliation = (JSONObject) author.get("affiliation");
            if(affiliation.get("location") != null) {
                JSONObject location = (JSONObject) affiliation.get("location");
                if (!(location.isEmpty()) && location.get("country") != null) {
                    countries.add((String) location.get("country"));
                }
            }
        }

        return countries;
    }

    public String parseAbstract()
    {
        JSONArray abs = (JSONArray) jsonObject.get("abstract");
        String text = "";

        Iterator<JSONObject> iterator = abs.iterator();
        while (iterator.hasNext()) {
            JSONObject first_part = iterator.next();
            text += (String) first_part.get("text") + " ";
        }
        return text;
    }


    public String parseBodyText() {
        JSONArray body_text = (JSONArray) jsonObject.get("body_text");
        String text = "";

        Iterator<JSONObject> iterator = body_text.iterator();
        while (iterator.hasNext()) {
            JSONObject first_part = iterator.next();
            text += (String) first_part.get("text") + " ";
        }
        return text;
    }

}