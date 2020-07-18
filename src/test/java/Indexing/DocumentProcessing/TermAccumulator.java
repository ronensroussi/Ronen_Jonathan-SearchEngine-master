package Indexing.DocumentProcessing;

import java.util.SortedSet;
import java.util.concurrent.ArrayBlockingQueue;

public class TermAccumulator implements Runnable{
    SortedSet<Term> terms;
    ArrayBlockingQueue<TermDocument> termDocs;

    public TermAccumulator(SortedSet<Term> terms, ArrayBlockingQueue<TermDocument> termDocs) {
        this.terms = terms;
        this.termDocs = termDocs;
    }

    @Override
    public void run() {
        try {
            boolean done = false;
            while( !done){
                TermDocument termDoc = termDocs.take();
                if(termDoc.getText() == null){
                    done = true;
                }
                else{
                    terms.addAll(termDoc.getText());
                    terms.addAll(termDoc.getTitle());
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

