package Indexing.Index;

import java.io.Serializable;


/**
 * Data type containing the information pertaining to a single Term in the main dictionary.
 */
public class IndexEntry implements Serializable {
    private int totalTF;
    private int df;
    private int postingPointer;


    public IndexEntry(int postingPointer , int df ,int totalTF) {
        this.postingPointer = postingPointer;
        this.df = df;
        this.totalTF = totalTF;
    }

    public IndexEntry(int totalTF , int df ) {
        this.totalTF = totalTF;
        this.df = df;
    }

    public void setTotalTF(int totalTF) {
        this.totalTF = totalTF;
    }


    @Override
    public String toString() {
        return "totalTF=" + totalTF +
                ", df=" + df;
    }

    public void setDf(int df) {
        this.df = df;
    }

    public void setPostingPointer(int postingPointers) {
        this.postingPointer = postingPointers;
    }

    public int getTotalTF() {
        return totalTF;
    }

    public int getDf() {
        return df;
    }

    public int getPostingPointer() {
        return postingPointer;
    }

}
