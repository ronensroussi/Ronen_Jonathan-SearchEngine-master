package Querying.Ranking;

import Indexing.Index.Posting;

import java.util.Calendar;
import java.util.Date;

/**
 * Extends {@link BM25Ranker BM25Ranker} by considering additional information about a term's appearance in a document.
 */
public class WeightedBM25Ranker extends BM25Ranker{

    public WeightedBM25Ranker(RankingParameters rankingParameters, int numDocsInCorpus, double averageDocumentLengthInCorpus) {
        super(rankingParameters, numDocsInCorpus, averageDocumentLengthInCorpus);
    }

    @Override
    protected double calculateRankForExplicitPosting(ExpandedPosting ePosting) {
        return calculateRankForPosting(ePosting, true);
    }

    @Override
    protected double calculateRankForImplicitPosting(ExpandedPosting ePosting) {
        return calculateRankForPosting(ePosting, false);
    }

    /**
     * calculates a rank with the BM25 algorithm's step (the inside of the sum loop)
     * @param ePosting the posting to calculate for
     * @param isExplicit whether or not a term was explicitly in a query
     * @return a rank score for one posting, according to the BM25 algorithm.
     */
    double calculateRankForPosting(ExpandedPosting ePosting, boolean isExplicit) {
        return ((super.calculateRankForPosting(ePosting) /*BM25*/ * rankingParameters.frequencyWeight)
                + getMetadataBonuses(ePosting) + getRecencyBonus(ePosting)) *
                getExplicitness(ePosting, isExplicit);
    }

    /**
     * calculate additional bonuses to a posting's relevance by whether a posting appeared in the title of a document,
     * or in the document's beginning.
     * @param ePosting information about a term's appearance in a document.
     * @return bonuses to a posting's relevance ranking.
     */
    protected double getMetadataBonuses(ExpandedPosting ePosting){
        Posting p = ePosting.posting;
        return rankingParameters.titleWeight * (p.isInTitle() ? 1 : 0) +
                rankingParameters.beginningWeight * (p.isInBeginning() ? 1 : 0);
    }

    /**
     * return a bonus 0<=double<=1 where bonus<=1 for document recency.
     * calculation is base on difference in months from current date.
     * Therefore, documents from the same month will always receive the same bonus.
     * Gives a 0 bonus for documents happening in the future.
     * Gives a negative bonus for documents more than 40 years old.
     * @param expandedPosting information about a term's appearance in a document.
     * @return bonuses to a posting's relevance ranking, based on recency. 0 if expandedPosting.date is null.
     */
    protected double getRecencyBonus(ExpandedPosting expandedPosting){
        if(expandedPosting.date == null) return 0;
        Date currDate = Calendar.getInstance().getTime();
        int monthDelta = currDate.getMonth() - expandedPosting.date.getMonth();
        int yearDelta = currDate.getYear() - expandedPosting.date.getYear();
        int totalMonthDelta = yearDelta*12 + monthDelta;
        double normalizedTimeDelta = (double)totalMonthDelta/360.0; //normalize to the number of months in 30 years
        return (normalizedTimeDelta < 0 ? 0 /*a negative value indicates an invalid, future date*/ :
                1 - normalizedTimeDelta /*better bonuses for documents with a smaller delta (newer)*/) * rankingParameters.recencyWeight;
    }

    /**
     * returns a ranking for how close a term is to being explicitly in the query
     * @param ePosting information about a term's appearance in a document.
     * @param isExplicit whether or not a term was explicitly in a query
     * @return 1 if the term is in the query, 0<=value<1 if the term was derived semantically from the query
     */
    protected double getExplicitness(ExpandedPosting ePosting, boolean isExplicit){
        return
        /* weight of 1 for explicit, calculated weight (<=1) for implicit*/
        isExplicit ? 1 :
        /* slightly higher weight for closer neighbors*/
        (rankingParameters.implicitTermMatchWeight + 0.1*((double)1-Math.abs(queryNeighbors.get(ePosting.term.toLowerCase()))));
    }
}
