package Indexing.Index;

import javafx.geometry.Pos;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Data type containing the information pertaining to a single Term in the temporary dictionary.
 */
public class TempIndexEntry {
    private int tfTotal;
    private int df;
    private List<Posting> posting;
    private int [] pointerList;

    public TempIndexEntry(){
        tfTotal = 0;
        df=0;
        posting=new ArrayList<>();
        pointerList = new int[1]; // initial size
        pointerList[0]=-1;
    }

    public int getDf(){
        return df;
    }

    public void increaseDf(){
        df++;
    }

    /**
     * this metho increase the TotalTF field by an n value
     * @param n - the value that we want to add to TotalTF
     */
    public void increaseTfByN(int n){

        tfTotal+=n;
    }

    /**
     * this method add a posting to the Posting list
     * @param pos - Posting Object to add
     */
    public void addPosting(Posting pos){
        posting.add(pos);
        df++;
    }

    public List<Posting> getPosting(){
        return posting;
    }

    /**
     * sort the posting list by the tf value of each posting  the max tf will be first
     */
    public void sortPosting(){
        Collections.sort(posting, new Comparator<Posting>() {
            @Override
            public int compare(Posting o1, Posting o2) {
                if(o1.getTf()>o2.getTf()){
                    return -1;
                }else if(o1.getTf()==o2.getTf()){
                    return 0;
                }else
                    return 1;
            }
        });
    }
//
//    public void addPointer(byte fileIndex, long pointer){
//        int size = pointerList.size();
//        int i=size;
//        if(size>fileIndex){
//            pointerList.add(fileIndex,pointer);
//        }
//        else {
//            while (i<fileIndex){
//                pointerList.add(i,new Long(-1));
//                i++;
//            }
//            pointerList.add(i,pointer);
//        }
//
//    }


    public int[] getPointerList(){
        return pointerList;
    }

    public void deletePostingList(){
        posting.clear();
    }
    public int getPostingSize(){
        return posting.size();
    }
    public int getTfTotal(){return tfTotal;}

    /**
     * simulates a dynamic array of int that only grows.
     * inserts into the given array and returns it if it is large enough.
     * if it isn't large enough to be inserted to ( {@code index} >= array.length, will copy array contents
     * into a new array with double the size, insert, and return the new array.
     * @param index index to insert on.
     * @param intToInsert - int to insert.
     */
    public void addPointer(int index, int intToInsert){
        int [] array = this.pointerList;
        int length = array.length;
        if(index <length){
            array[index] = intToInsert;
        }
        else { // index >= array.length
            int size = index+1;
            int [] newArray = new int[size];
            int i;
            for (i = 0; i <array.length ; i++) {
                newArray[i]=array[i];
            }
            for (;i<size;i++){
                if(i!=index){
                    newArray[i]=-1;
                }else {
                    newArray[i]=intToInsert;
                }
            }
            this.pointerList=newArray;
        }
    }


}
