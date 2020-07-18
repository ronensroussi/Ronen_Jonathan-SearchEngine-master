package Indexing.Index.IO;

import Indexing.Index.Posting;
import javafx.geometry.Pos;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PostingInputStreamTest {


    static final String path = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\PostingInputStreamTest";
    IPostingOutputStream out;
    IPostingInputStream in;


    public PostingInputStreamTest() throws IOException {

        try {
            out = new PostingOutputStream(path);
            in = new PostingInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void shortTest() throws IOException {
        int numPostings = 2;
        ArrayList<Posting> postingsForOneTerm = new ArrayList<>(numPostings);
        Posting p1 = new Posting(66, (short)13, true, false);
        Posting p2 = new Posting(129, (short)28, false, false);
        postingsForOneTerm.add(p1);
        postingsForOneTerm.add(p2);

        long pointer = out.write(postingsForOneTerm);
        out.flush();
        out.close();

        List<Posting> postingsIn = in.readTermPostings(pointer);

        for (int i = 0; i < postingsForOneTerm.size() ; i++) {
            assertEquals(postingsForOneTerm.get(i), postingsIn.get(i));
            System.out.println(postingsIn.get(i));
        }

    }


    @Test
    void multiTerm() throws IOException {
        Random random = new Random();
        int numTerms = 4;
        ArrayList<ArrayList<Posting>> termsOut = new ArrayList<>(4);
        for (int i = 0; i <  numTerms ; i++) {
            int numPostings = random.nextInt(100);
            termsOut.add(new ArrayList<>());
            for (int j = 0; j < numPostings ; j++) {
                termsOut.get(i).add(getRandomPosting(random));
            }
        }

        long[] pointers = new long[numTerms];

        for (int i = 0; i < numTerms ; i++) {
            pointers[i] = out.write(termsOut.get(i));
        }

        out.flush();
        out.close();

        List<Posting>[] termsIn = new List[numTerms];
        for (int i = 0; i < numTerms ; i++) {
            termsIn[i] = in.readTermPostings(pointers[i]);
        }

        for (int i = 0; i < numTerms ; i++) {
            assertEqualsOnTermPostings(termsOut.get(i), termsIn[i]);
        }
    }

    @Test
    void multiTermRandomReads() throws IOException {
        Random random = new Random();
        int numTerms = 4;
        ArrayList<ArrayList<Posting>> termsOut = new ArrayList<>(4);
        for (int i = 0; i <  numTerms ; i++) {
            int numPostings = random.nextInt(100);
            termsOut.add(new ArrayList<>());
            for (int j = 0; j < numPostings ; j++) {
                termsOut.get(i).add(getRandomPosting(random));
            }
        }

        long[] pointers = new long[numTerms];

        for (int i = 0; i < numTerms ; i++) {
            pointers[i] = out.write(termsOut.get(i));
        }

        out.flush();
        out.close();

        List<Posting>[] termsIn = new List[numTerms];
        for (int i = 0; i < numTerms ; i++) {
            termsIn[i] = in.readTermPostings(pointers[i]);
        }

        int i = 2;
        assertEqualsOnTermPostings(termsOut.get(i), termsIn[i]);
        i = 3;
        assertEqualsOnTermPostings(termsOut.get(i), termsIn[i]);
        i = 1;
        assertEqualsOnTermPostings(termsOut.get(i), termsIn[i]);
        i = 0;
        assertEqualsOnTermPostings(termsOut.get(i), termsIn[i]);

    }

    private Posting getRandomPosting(Random random) {
        return new Posting((short)random.nextInt(Integer.MAX_VALUE), (short)random.nextInt(Short.MAX_VALUE), random.nextBoolean(), random.nextBoolean());
    }

    private void assertEqualsOnTermPostings(ArrayList<Posting> x, List<Posting> postingsIn1) {
        List<Posting> postingsIn = postingsIn1;
        System.out.println(x);
        System.out.println(postingsIn1);
        assertArrayEquals(postingsIn.toArray(), x.toArray());
    }

    @Test
    void testTime() throws IOException {
        int numPostings = 100;
        ArrayList<Posting> postingsForOneTerm = new ArrayList<>(numPostings);
        for (int i = 0; i <numPostings ; i++) {
            postingsForOneTerm.add(getRandomPosting(new Random()));
        }

        int numTerms = 100000;

        long[] pointers = new long[numTerms];

        long startTime = System.currentTimeMillis();

        for (int j = 0; j < numTerms ; j++) {
            pointers[j] = out.write(postingsForOneTerm);
        }

        out.flush();

        long time = (System.currentTimeMillis() - startTime);

        System.out.println("time to write 100,000 terms with 100 postings each (ms): " + (time));
        System.out.println("time to write 100,000 terms with 100 postings each, fifty times (m): " + ((time)*50/1000)/60);

        startTime = System.currentTimeMillis();

        for (int i = 0; i < numPostings ; i++) {
            in.readTermPostings(pointers[i]);
        }

        time = (System.currentTimeMillis() - startTime);

        System.out.println("time to read 100,000 terms with 100 postings each (ms): " + (time));
        System.out.println("time to read 100,000 terms with 100 postings each, fifty times (m): " + ((time)*50/1000)/60);

    }

//    @Test
//    void references() {
//        Object o1 = new Object();
//        Object o2 = new Object();
//        Object o3 = new Object();
//
//        ArrayList<Object> container = new ArrayList<>();
//
//        container.add(o1);
//        container.add(o2);
//        container.add(o3);
//
//        o3 = null;
//
//        // here everyone is still alive
//
//        letsClearIt(container);
//
//        //now o3 can be collected by GC. o1, o2 and container are still alive.
//
//        o2 = null;
//
//        // o2 can be collected
//
//        container = null;
//
//        // container can be collected
//
//        ArrayList<Object> container2 = new ArrayList<>();
//
//        container2.add(o1);
//
//        o1 = null;
//
//        //no effect
//
//        container2 = null;
//
//        // now container 2 and o1 can be collected
//    }
//
//    @Test
//    void letsClearIt(ArrayList<Object> container) {
//        container.clear();
//    }
}