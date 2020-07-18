package Indexing.Index.IO;

import Indexing.Index.Posting;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PostingOutputStreamTest {

    static final String outputPath = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\PostingOutputStreamTest";
    IPostingOutputStream out;


    public PostingOutputStreamTest() throws IOException {

        try {
            out = new PostingOutputStream(outputPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testTime() throws IOException {
        int numPostings = 100;
        ArrayList<Posting> postingsForOneTerm = new ArrayList<>(numPostings);
        Posting p1 = new Posting(66, (short)13, true, false);
        Posting p2 = new Posting(129, (short)28, false, false);
        for (int i = 0; i <50 ; i++) {
            postingsForOneTerm.add(i, p1);
        }
        for (int i = 50; i <numPostings ; i++) {
            postingsForOneTerm.add(i, p2);
        }


        long startTime = System.currentTimeMillis();

        for (int j = 0; j <100000 ; j++) {
                out.write(postingsForOneTerm);
        }

        out.flush();

        long time = (System.currentTimeMillis() - startTime);

        System.out.println("time for 100,000 terms with 100 postings each (ms): " + (time));
        System.out.println("time for 100,000 terms with 100 postings each, fifty times (m): " + ((time)*50/1000)/60);

    }


    @Test
    void testBoolsToByte() {
        Posting p1 = new Posting(129, (short)28, false, false);
        Posting p2 = new Posting(66, (short)13, true, false);
        Posting p3 = new Posting(932, (short)1522, false, true);
        Posting p4 = new Posting(932, (short)1522, true, true);

        System.out.println(PostingOutputStream.extractBoolsAsByte(p1));
        System.out.println(PostingOutputStream.extractBoolsAsByte(p2));
        System.out.println(PostingOutputStream.extractBoolsAsByte(p3));
        System.out.println(PostingOutputStream.extractBoolsAsByte(p4));
    }

    @Test
    void shortTest() throws IOException {
        int numPostings = 2;
        ArrayList<Posting> postingsForOneTerm = new ArrayList<>(numPostings);
        Posting p1 = new Posting(66, (short)13, true, false);
        Posting p2 = new Posting(129, (short)28, false, false);
        postingsForOneTerm.add(p1);
        postingsForOneTerm.add(p2);

        out.write(postingsForOneTerm);

        out.flush();

        out.close();
    }
}