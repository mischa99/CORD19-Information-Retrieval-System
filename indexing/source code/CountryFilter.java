import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a customized Token Filter that transforms different versions of writing
 * USA, UK and China encountered in the collection into a single token.
 * This way, searching for "United States" or "U.S.A" will retrieve any document
 * that has one of the many different versions of writing USA as its country field value,
 * not just the ones containing "United States" only.
 *
 * @author mikhail
 */
public class CountryFilter extends TokenFilter {

    private Map<String,String> countryMap = new HashMap<String,String>();

    private CharTermAttribute termAttr;

    public CountryFilter(TokenStream input) {
        super(input);
        termAttr = addAttribute(CharTermAttribute.class);
        //USA
        countryMap.put("United States", "USA");
        countryMap.put("United States of America", "USA");
        countryMap.put("USA, USA", "USA");
        countryMap.put("U.S.A", "USA");
        countryMap.put("California", "USA"); //??


        //UK
        countryMap.put("United Kingdom", "UK");
        countryMap.put("UK, UK", "UK");
        countryMap.put("U.K", "UK");

        //China
        countryMap.put("PR China", "China");
        countryMap.put("People's Republic of China", "China");
        countryMap.put("China, China", "China");
        countryMap.put("P.R. China", "China");
        countryMap.put("China., China", "China");



    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken())
            return false;

        String token = termAttr.toString();
        if(countryMap.containsKey(token)) {
            termAttr.setEmpty().append(countryMap.get(token));
        }
        return true;
    }
}
