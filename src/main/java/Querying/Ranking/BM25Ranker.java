package Querying.Ranking;

/**
 * A {@link Ranker Ranker} implementation that uses the BM25+ formula to rank documents.
 */
public class BM25Ranker extends Ranker {

    private static double delta = 1.0;

    public BM25Ranker(RankingParameters rankingParameters, int numDocsInCorpus, double averageDocumentLengthInCorpus) {
        super(rankingParameters, numDocsInCorpus, averageDocumentLengthInCorpus);
    }

    /**
     * implements a simple sum (the sum loop in BM25 algorithm).
     * @param existingRank the rank that is currently assigned to the document.
     * @param newPostingRank the rank that was calculated for the new posting. should be a posting for the same document.
     * @return the new rank that should be assigned to the document.
     */
    @Override
    protected double addNewPostingRankToExistingDocRank(double existingRank, double newPostingRank) {
        return existingRank + newPostingRank;
    }

    /**
     * calculates a rank with the BM25 algorithm's step (the inside of the sum loop)
     * @param ePosting the posting to calculate for
     * @return a rank score for one posting, according to the BM25 algorithm.
     */
    double calculateRankForPosting(ExpandedPosting ePosting){
        //compute numerator
        double numerator = getBM25Numerator(ePosting);
        //compute denominator
        double denominator = getMB25Denominator(ePosting);

        //the delta is from BM25+ implementation
        return getIDF(ePosting)*((numerator/denominator) + delta);
    }

    /**
     * calculate the denominator part of the BM25 formula.
     * @param ePosting - the posting to calculate for.
     * @return The denominator part of the BM25 formula
     */
    protected double getMB25Denominator(ExpandedPosting ePosting) {
        return (double)ePosting.posting.getTf() + rankingParameters.k_BM25 * ((double)1 - rankingParameters.b_BM25 + rankingParameters.b_BM25*((double)ePosting.numOfUniqueWords_doc / averageDocumentLengthInCorpus));
    }

    /**
     * calculate the numerator part of the BM25 formula.
     * @param ePosting - the posting to calculate for.
     * @return The numerator part of the BM25 formula
     */
    protected double getBM25Numerator(ExpandedPosting ePosting) {
        return (double)ePosting.posting.getTf() * (rankingParameters.k_BM25 +1);
    }


    /**
     * see parent class
     * @param ePosting information about the term and the document it appears in.
     * @return
     */
    @Override
    protected double calculateRankForExplicitPosting(ExpandedPosting ePosting) {
        return calculateRankForPosting(ePosting);
    }

    /**
     * see parent class
     * @param ePosting information about the term and the document it appears in.
     * @return
     */
    @Override
    protected double calculateRankForImplicitPosting(ExpandedPosting ePosting) {
        return calculateRankForPosting(ePosting);
    }

}
