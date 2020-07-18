package Querying.Ranking;

import Indexing.Index.Posting;

import java.util.Date;

/**
 * adds more information to the {@link Posting Posting} to allow ranking of documents for queries.
 */
public class ExpandedPosting {

    public final Posting posting;
    public final int totalTF_term;
    public final int df_term;
    public final int numOfUniqueWords_doc;
    public final int maxTF_doc;
    public final int doc_length;
    public final String term;
    public Date date;


    public ExpandedPosting(Posting posting, int totalTF_term, int df_term, int numOfUniqueWords_doc, int maxTF_doc,int doc_length, String term , Date date) {
        this.posting = posting;
        this.totalTF_term = totalTF_term;
        this.df_term = df_term;
        this.numOfUniqueWords_doc = numOfUniqueWords_doc;
        this.maxTF_doc = maxTF_doc;
        this.doc_length = doc_length;
        this.term = term;
        this.date=date;
    }
}
