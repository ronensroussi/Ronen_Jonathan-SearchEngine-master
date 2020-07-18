package Indexing.Index;

import Indexing.DocumentProcessing.Term;
import Indexing.DocumentProcessing.TermDocument;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * abstract class for index makers.
 */
public abstract class AIndexMaker {

    protected Map<Term,IndexEntry> index;

    public AIndexMaker(){
        this.index= new LinkedHashMap<>();

    }

    /**
     * this method receive a Term doc and adds the relevant data to the index
     * @param doc - termDoc object which we want to index
     */
    abstract public void  addToIndex(TermDocument doc);


}
