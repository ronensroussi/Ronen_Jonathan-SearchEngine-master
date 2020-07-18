package Indexing.Index.IO;

import Indexing.Index.Posting;

import java.io.*;
import java.util.List;

/**
 * an advanced implementation for writing postings to disk, meant to be efficient and mobile.
 * it writes the posting information as binary information, in a compact manner.
 * it uses a bufferedOutputStream to increase efficiency.
 * format per array (term postings):<br>
 * 1 int - number of postings<br>
 * then per posting:<br>
 * all int fields.<br>
 * all short fields.<br>
 * all boolean fields.<br>
 */
public class PostingOutputStream extends APostingOutputStream {


    /**
     * if the file doesn't exist, creates it.
     * if the file exists, clears it!
     * @param pathToFile
     * @throws IOException
     */
    public PostingOutputStream(String pathToFile) throws IOException {
        super(pathToFile);
        postingsFile = new BufferedOutputStream(new FileOutputStream(pathToFile));
    }

    /**
     * see interface PostingOutputStream
     * @param postings  - an array of postings to write.
     * @return
     * @throws NullPointerException
     * @throws IOException
     */
    @Override
    public long write(List<Posting> postings) throws NullPointerException, IOException {
        long startIdx = getCursor();

        byte[] outBytes = postingsArrayToByteArray(postings);
//        buffer.add(outBytes);
        postingsFile.write(outBytes);

        filePointer += outBytes.length;

        return startIdx;
    }


    /**
     * see interface PostingOutputStream
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        postingsFile.flush();
    }

    protected byte[] postingsArrayToByteArray(List<Posting> postings){
        int numPostings = postings.size();
        byte[] data = new byte[4 + numPostings *
                (Posting.getNumberOfShortFields()*2 + Posting.getNumberOfIntFields()*4 + 1 /*holds 8 bools*/  )];

        intToByteArray(numPostings, data, 0);

        int dataIdx = 4;

        for (Posting p :postings
             ) {
            int[] intFieldsFori = extractIntegerFields(p);
            for (int integer: intFieldsFori
                    ) {
                intToByteArray(integer, data, dataIdx);
                dataIdx += 4;
            }

            short[] shortFieldsFori = extractShortFields(p);
            for (short s: shortFieldsFori
                    ) {
                data[dataIdx] = short8MSB(s);
                dataIdx++;
                data[dataIdx] = short8LSB(s);
                dataIdx++;
            }

//            boolean[] booleanFieldsFori = extractBooleanFields(postings.get(i));
            // THIS HAS TO CHANGE IF POSTING CHANGES //
            data[dataIdx] = extractBoolsAsByte(p);
            dataIdx++;
            // THIS HAS TO CHANGE IF POSTING CHANGES //
        }

        return data;
    }





    @Override
    public void close() throws IOException {
        postingsFile.close();
    }

    protected static byte extractBoolsAsByte(Posting p){
        byte isInTitle = p.isInTitle() ? Byte.MIN_VALUE+1 : Byte.MIN_VALUE;
        byte isInBeggining = p.isInBeginning() ? Byte.MIN_VALUE+2 : Byte.MIN_VALUE;
        // next will have +4

        return (byte)(isInTitle | isInBeggining);
    }


}
