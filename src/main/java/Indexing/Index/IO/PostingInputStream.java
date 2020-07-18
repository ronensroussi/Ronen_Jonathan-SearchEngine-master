package Indexing.Index.IO;

import Indexing.Index.Posting;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * reads postings from a file. meant to read postings in the format that {@link PostingOutputStream PostingOutputStream} writes.
 * To increase efficiency, reads {@value #prefetchAmount} postings in advance, reducing the amount of reads to 1 when possible.
 * If more there are more than {@value #prefetchAmount} postings to read, will perform 1 more read operation (2 total reads).
 */
public class PostingInputStream implements IPostingInputStream {

    private static final int prefetchAmount = 11;
    private RandomAccessFile postingsFile;
    private int cursor = 0; //cursor for location on the array of bytes that were read from the disk. is reset with every read.

    public PostingInputStream(String pathToFile) throws FileNotFoundException {
        this.postingsFile = new RandomAccessFile(pathToFile, "r");
    }

    protected Posting fieldsToPosting(short[] shortFields, int[] ints , String[] stringFields, boolean[] booleanFields){
        return new Posting(ints, shortFields, booleanFields);
    }

    /**
     * see interface IPostingInputStream.
     * @param pointerToStartOfPostingArray
     * @return
     * @throws IOException
     */
    @Override
    public List<Posting> readTermPostings(long pointerToStartOfPostingArray) throws IOException {
        return readTermPostings(pointerToStartOfPostingArray, Integer.MAX_VALUE);
    }

    /**
     * see interface IPostingInputStream.
     * @param pointerToStartOfPostingArray
     * @param maxNumPostings
     * @return
     * @throws IOException
     */
    @Override
    public List<Posting> readTermPostings(long pointerToStartOfPostingArray, int maxNumPostings) throws IOException {
        //setup
        postingsFile.seek(pointerToStartOfPostingArray);
        cursor = 0;
        //many terms will have no more than a certain small number of postings, pre-fetching them cuts the number of reads for such a term from 2 to 1.
        byte[] firstRead = new byte[4 + byteLengthOfSinglePosting()*prefetchAmount];
        postingsFile.read(firstRead);

        int numPostingsForTerm = readFourBytesAsInt(firstRead, 0);
        int numPostingsToRead = Math.min(numPostingsForTerm, maxNumPostings);

        byte[] bytesFromDisk;

        //if not all postings are buffered, read all needed bytes in one read
        if(numPostingsToRead > prefetchAmount){
            bytesFromDisk = new byte[numPostingsToRead * byteLengthOfSinglePosting()];
            postingsFile.seek(pointerToStartOfPostingArray + 4);
            postingsFile.read(bytesFromDisk, 0, bytesFromDisk.length);
        }
        else{ //all postings have already been read
            bytesFromDisk = Arrays.copyOfRange(firstRead, 4, 4 + numPostingsToRead * byteLengthOfSinglePosting());
        }

        //convert bytes to postings and return
        return readNPostings(bytesFromDisk, numPostingsForTerm);
    }

    /**
     * reads {@code numberOfPostingsToRead} postings from the array of input bytes.
     * assumes the array contains enough bytes for {@code numberOfPostingsToRead} postings.
     * @param input - array of bytes that should be parsed as Postings
     * @param numberOfPostingsToRead - number of postings to read.
     * @return - a list of Postings parsed from the input byte array.
     */
    private List<Posting> readNPostings(byte[] input, int numberOfPostingsToRead)  {
        List<Posting>  postings = new ArrayList<>(numberOfPostingsToRead);
        cursor = 0;
        for (int i = 0; i < numberOfPostingsToRead ; i++) {
            postings.add(readSinglePosting(input));
        }
        return postings;
    }

    /**
     * reads a single posting.
     * is responsible for incrementing the field cursor according to the number of bytes read.
     * @param input
     * @return
     */
    private Posting readSinglePosting(byte[] input)  {
        int[] intFields = new int[Posting.getNumberOfIntFields()];
        short[] shortFields = new short[Posting.getNumberOfShortFields()];
        for (int i = 0; i < intFields.length ; i++) {
            intFields[i] = readFourBytesAsInt(input, cursor);
            cursor += 4;
        }
        for (int i = 0; i < shortFields.length ; i++) {
            shortFields[i] = readTwoBytesAsShort(input, cursor);
            cursor += 2;
        }

        // THIS HAS TO CHANGE IF POSTING CHANGES //
        boolean[] boolFields = new boolean[2];
        boolFields[0] = (input[cursor] & (0b00000001)) > 0b00000000;
        boolFields[1] = (input[cursor] & (0b00000010)) > 0b00000000;
        cursor += 1;
        // THIS HAS TO CHANGE IF POSTING CHANGES //

        return new Posting(intFields, shortFields, boolFields);
    }

    private int readFourBytesAsInt(byte[] input, int offset)  {
        return  (input[offset]<<24) & 0xff000000|
                (input[offset+1]<<16) & 0x00ff0000|
                (input[offset+2]<< 8) & 0x0000ff00|
                (input[offset+3]) & 0x000000ff;
    }
    //                (bytes[3]<< 0) & 0x000000ff;
    //                (bytes[2]<< 8) & 0x0000ff00|
    //                (bytes[1]<<16) & 0x00ff0000|
    //        return  (bytes[0]<<24) & 0xff000000|
    //        input.read(bytes, 0, 4);
    //        byte[] bytes = new byte[4];
//    private static int readFourBytesAsInt(InputStream input) throws IOException {

//    }

    private short readTwoBytesAsShort(byte[] input, int cursor)  {
        int res = (((int)input[cursor]) << 8) & 0x0000ff00; //add MSBs
        int LSBs = ((int)input[cursor+1] & 0x000000ff); //this makes sure that after casting to int, all bits except those in 8 LSBs are off.
        res = res | LSBs ; // add LSBs
        return (short)res;
    }

    private int byteLengthOfSinglePosting(){
        return Posting.getNumberOfShortFields()*2 + Posting.getNumberOfIntFields()*4 +  1 /*holds 8 bools*/  ;
    }

    public void close(){
        try {
            postingsFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
