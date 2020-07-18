package Indexing.Index.IO;

import Indexing.Index.Posting;

import java.io.*;
import java.nio.MappedByteBuffer;

/**
 * abstract class for posting output streams.
 */
public abstract class APostingOutputStream implements IPostingOutputStream{

    long filePointer = 0;
    OutputStream postingsFile;
//    MappedByteBuffer postingsFile;

    /**
     * if the file doesn't exist, creates it.
     * if the file exists, clears it!
     * @param pathToFile
     * @throws IOException
     */
    public APostingOutputStream(String pathToFile) throws IOException {
        this.postingsFile = new FileOutputStream(pathToFile);
    }

    /**
     * see interface PostingOutputStream
     * @return
     */
    @Override
    public long getCursor() {
        return filePointer;
    }



    static short[] extractShortFields(Posting posting){
        return posting.getShortFields();
    }

    static int[] extractIntFields(Posting posting){
        return posting.getIntegerFields();
    }
    static int[] extractIntegerFields(Posting posting){
        return posting.getIntegerFields();
    }
    static boolean[] extractBooleanFields(Posting posting){
        return posting.getBooleanFields();
    }


    static byte short8MSB(short s){
        return (byte)(s >> 8);
    }

    static byte short8LSB(short s){
        return (byte)s;
    }

    /**
     * big endian:  In this order, the bytes of a multibyte value are ordered from most significant to least significant.
     * @param i
     * @param bytes
     * @param startIdx
     * @throws IndexOutOfBoundsException
     */
    static void intToByteArray(int i, byte[] bytes, int startIdx) throws IndexOutOfBoundsException{
        bytes[startIdx] = (byte)(i >> 24);
        startIdx++;
        bytes[startIdx] = (byte)(i >> 16);
        startIdx++;
        bytes[startIdx] = (byte)(i >> 8);
        startIdx++;
        bytes[startIdx] = (byte)(i);
    }


}
