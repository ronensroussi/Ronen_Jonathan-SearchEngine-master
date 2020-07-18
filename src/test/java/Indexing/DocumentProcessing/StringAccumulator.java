package Indexing.DocumentProcessing;

import java.util.SortedSet;
import java.util.concurrent.ArrayBlockingQueue;

public class StringAccumulator implements Runnable{
    SortedSet<String> terms;
    ArrayBlockingQueue<TermDocument> termDocs;

    public StringAccumulator(SortedSet<String> terms, ArrayBlockingQueue<TermDocument> termDocs) {
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
                    for (Term t: termDoc.getText())
                    {
                        terms.add(t.toString());

                    }
                    for (Term t: termDoc.getTitle()
                         ) {
                        terms.add(t.toString());
                    }
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
