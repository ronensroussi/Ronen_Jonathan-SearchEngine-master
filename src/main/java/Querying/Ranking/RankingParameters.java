package Querying.Ranking;

/**
 * A set of weights for the {@link Ranker Ranker} class.
 * Is used to give different weights to the various factors considered when ranking a document's relevancy to a query.
 */
public class RankingParameters {

    public final double titleWeight;
    public final double beginningWeight;
    public final double frequencyWeight;
    public final double implicitTermMatchWeight;
    public final double recencyWeight;
    public final double k_BM25;
    public final double b_BM25;

    public RankingParameters(double titleWeight, double beginningWeight, double frequencyWeight, double implicitTermMatchWeight, double recencyWeight, double k_BM25, double b_BM25) {
        this.titleWeight = titleWeight;
        this.beginningWeight = beginningWeight;
        this.frequencyWeight = frequencyWeight;
        this.implicitTermMatchWeight = implicitTermMatchWeight;
        this.recencyWeight = recencyWeight;
        this.k_BM25 = k_BM25;
        this.b_BM25 = b_BM25;
    }

    public RankingParameters(double titleWeight, double beginningWeight, double frequencyWeight, double implicitTermMatchWeight, double recencyWeight) {
        this.titleWeight = titleWeight;
        this.beginningWeight = beginningWeight;
        this.frequencyWeight = frequencyWeight;
        this.implicitTermMatchWeight = implicitTermMatchWeight;
        this.recencyWeight = recencyWeight;
        this.k_BM25 = 1.5;
        this.b_BM25 = 0.75;
    }

    @Override
    public String toString() {
        return "RankingParameters{" +
                "titleWeight=" + titleWeight +
                ", beginningWeight=" + beginningWeight +
                ", frequencyWeight=" + frequencyWeight +
                ", implicitTermMatchWeight=" + implicitTermMatchWeight +
                ", k_BM25=" + k_BM25 +
                ", b_BM25=" + b_BM25 +
                '}';
    }
}
