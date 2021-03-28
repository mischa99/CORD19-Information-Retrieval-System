import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;

public class CountryAnalyzer extends Analyzer{

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String s) {
        Tokenizer tokenizer = new KeywordTokenizer();
        TokenStream result = new CountryFilter(tokenizer);
        result = new LowerCaseFilter(result);
        return new Analyzer.TokenStreamComponents(tokenizer, result);
    }
}
