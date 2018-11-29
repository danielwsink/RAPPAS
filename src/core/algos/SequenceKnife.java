/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.algos;

import core.QueryWord;
import core.States;
import etc.Infos;
import etc.exceptions.NonSupportedStateException;
import inputs.Fasta;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * to easily derive mers from a query sequence,\n
 * the class in intended to retrieve mer in a ordered or stochastic way\n
 * this should allow to abandon the read comparison after a certain number\n
 * of matches and mismatches.
 * @author ben
 */
public class SequenceKnife {
    
    /**
     * done through the shuffling of the table merOrder
     */
    final public static int SAMPLING_STOCHASTIC=1; 
    /**
     * ie, [0,k],[1,k+1],[2,k+2], ... [n,k+n]
     */
    final public static int SAMPLING_LINEAR=2;
    /**
     * ie, [0,k],[k,2k],[2k,3k], ... ,[1,k+1],[k+1,2K+1], ... ,[2,k+2],[k+2,2k+2]
     */
    final public static int SAMPLING_SEQUENTIAL=3;
    /**
     * ie, [0,k],[k+1,2k],[2k+1,3k],...
     */
    final public static int SAMPLING_NON_OVERLAPPING=4;
    
    final public static byte[] AMBIGUOUS_KMER=new byte[1]; //reserved value x00000000 to describe an ambigous kmer
    
    private Long seed=null;
    
    private int k=-1;
    private int minK=-1;
    private int iterator=0; //last returned mer, as index of the merOrder table
    private byte[] sequence=null; //the inital sequence itself
    private int[] merOrder=null; //to define the order in which the mer are returned
    private States s=null;
    private int step=-1;
    private boolean[] ambiguousKmer=null; //when true, kmer starting at this position would contain an ambiguous state, so skipped.
    
    /**
     * Basic constructor, will return mers in linear order
     * @param f
     * @param k
     * @param minK
     * @param s 
     */
    public SequenceKnife(Fasta f, int k, int minK, States s) {
        this.k=k;
        this.minK=minK;
        this.s=s;
        String seq = f.getSequence(false);
        try {
            initTables(seq, SAMPLING_LINEAR);
        } catch (NonSupportedStateException ex) {
            Logger.getLogger(SequenceKnife.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
   
    /**
     * Basic constructor, will return mers in linear order
     * @param seq
     * @param k
     * @param minK
     * @param s 
     */
    public SequenceKnife(String seq, int k, int minK, States s) {
        this.k=k;
        this.minK=minK;
        this.s=s;
        try {
            initTables(seq, SAMPLING_LINEAR);
        } catch (NonSupportedStateException ex) {
            Logger.getLogger(SequenceKnife.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * constructor setting the mer order through SAMPLING_* static variables
     * @param seq
     * @param k
     * @param minK
     * @param s
     * @param samplingMode 
     */
    public SequenceKnife(String seq, int k, int minK, States s, int samplingMode) {
        this.k=k;
        this.minK=minK;
        this.s=s;
        try {
            initTables(seq, samplingMode);
        } catch (NonSupportedStateException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * constructor setting the mer order through SAMPLING_* static variables
     * @param f
     * @param k
     * @param minK
     * @param s
     * @param samplingMode 
     */
    public SequenceKnife(Fasta f, int k, int minK, States s, int samplingMode) {
        this.k=k;
        this.minK=minK;
        this.s=s;
        String seq = f.getSequence(false);
        try {
            initTables(seq, samplingMode);
        } catch (NonSupportedStateException ex) {
            ex.printStackTrace(System.err);
            System.out.println("Query sequence contains not yet supported states. ("+f.getHeader()+")");
            System.exit(1);
        }

    }
    
    private void initTables(String seq, int samplingMode) throws NonSupportedStateException {
        sequence=new byte[seq.length()];
        ambiguousKmer=new boolean[seq.length()];
        for (int i = 0; i < seq.length(); i++) {
            
            try {
                sequence[i]=s.stateToByte(seq.charAt(i));
                if (s.isAmbiguous(seq.charAt(i))) { //expected states?
                    Infos.println("Ambiguous state in position "+(i+1)+" (char='"+seq.charAt(i)+"'), overlapping k-mers are ignored.");
                    for (int j=i-k+1; j<i+1; j++) {
                        if (j>-1 && j<seq.length()+1) {
                            ambiguousKmer[j]=true;
                        }
                    }
                }
            } catch (NonSupportedStateException ex) {
                ex.printStackTrace(System.err);
                System.out.println("Query contains a non supported state.");
                System.exit(1);
            }
              
        }
        //Infos.println("Binary seq: "+Arrays.toString(sequence));
        switch (samplingMode) {
            case SAMPLING_LINEAR:
                merOrder=new int[seq.length()];
                for (int i = 0; i < merOrder.length; i++) {
                    merOrder[i]=i;
                }
                this.step=1;
                break;
            case SAMPLING_NON_OVERLAPPING:
                merOrder=new int[(seq.length()/k)+1];
                for (int i = 0; i < seq.length(); i++) {
                    if (i%k==0) {
                        merOrder[i/k]=i;
                    }
                }
                this.step=k;
                break;
            case SAMPLING_STOCHASTIC:
                merOrder=new int[seq.length()];
                shuffledMerOrder();
                this.step=1;
                break;
            case SAMPLING_SEQUENTIAL:
                merOrder=new int[seq.length()];
                sequencialMerOrder();
                this.step=1;
                break;
            default:
                Infos.println("Sampling mode not_recognized !");
                break;
        }
    }
    
    /**
     * get a word targeted through its 1st residue position
     * @param queryPosition
     * @return 
     */
    @Deprecated
    public QueryWord getWordAt(int queryPosition) {
        assert queryPosition>-1;
        assert queryPosition<(sequence.length-k+1);
        //this needs optimization !!! to avoid the copy
        return new QueryWord(Arrays.copyOfRange(sequence, queryPosition, queryPosition+k),queryPosition);
    }
    
    /**
     * a table representing the order in which mers are returned \n
     * each value is the 1st position of the mer which will sample the \n
     * sequence from this position to position+k (excepted if < to the  min k)
     * @return 
     */
    public int[] getMerOrder() {
       return merOrder; 
    }
    
    /**
     * number of mers that will be provided by this knife (sampling method dependant)
     * @return 
     */
    public int getMerCount() {
        return merOrder.length;
    }
    
    /**
     * max number of words that can be built from this sequence (length-k+1)/s
     * @return 
     */
    public int getMaxMerCount() {
        return (this.sequence.length-this.k+1)/this.step;
    }
    
    
    
    /**
     * must be called to retireve mers one by one
     * @return the next mer as a @Word, null is no more mers to return
     */
    public QueryWord getNextWord() {
        if (iterator>merOrder.length-1) {
            return null;
        }
        int currentPosition=merOrder[iterator];
        int charactersLeft=sequence.length-currentPosition;
        if (charactersLeft>=minK) {
            byte[] word=null;
            if (charactersLeft<k) {
                word=Arrays.copyOfRange(sequence, currentPosition, currentPosition+charactersLeft);
            } else {
                word=Arrays.copyOfRange(sequence, currentPosition, currentPosition+k);
            }
            iterator++;
            return new QueryWord(word, currentPosition);
            
        } else {
            //Infos.println("Skip word on position "+currentPosition+": length < minK !");
            //this allow to skip words that are too short but in the middle
            //of the shuffled mer order, we just skip them and go to the next one.
            iterator++;
            return getNextWord();
        }
    }
    
    /**
     * must be called to retireve mers one by one
     * @return the next mer as a @Word, null is no more mers to return or a mer with at least one ambiguous state
     */
    public byte[] getNextByteWord() {
        if (iterator>merOrder.length-1) {
            return null;
        }
        int currentPosition=merOrder[iterator];
        int charactersLeft=sequence.length-currentPosition;
        if (charactersLeft>=minK) {
            byte[] word=null;
            //return null when this mer was marked as containing an ambiguous state
            if (ambiguousKmer[currentPosition]) {
                iterator++;
                return AMBIGUOUS_KMER;
            }
            //otherwise return mer
            if (charactersLeft<k) {
                word=Arrays.copyOfRange(sequence, currentPosition, currentPosition+charactersLeft);
            } else {
                word=Arrays.copyOfRange(sequence, currentPosition, currentPosition+k);
            }
            iterator++;
            return word;
            
        } else {
            //Infos.println("Skip word on position "+currentPosition+": length < minK !");
            //this allow to skip words that are too short but in the middle
            //of the shuffled mer order, we just skip them and go to the next one.
            iterator++;
            return getNextByteWord();
        }
    }
    
    
    /**
     * must be called after instantiation if one is interested to retrieve \n
     * the same shuffled mer order
     * @param seed 
     */
    public void forceSeed(long seed) {
        this.seed=seed;
        shuffledMerOrder();
    }
    
    private void shuffledMerOrder() {
        for (int i = 0; i < merOrder.length; i++) {
           merOrder[i]=i;
        }
        Random generator = null;
        if (seed!=null) {
            generator=new Random(seed);
        } else {
            generator=new Random(System.nanoTime());
        }
        for (int i = 0; i < merOrder.length - 1; i++) {
          int j = i + generator.nextInt(merOrder.length - i);
          int t = merOrder[j];
          merOrder[j] = merOrder[i];
          merOrder[i] = t;
        }
        
    }
    
    private void sequencialMerOrder() {
        //ie, [0,k],[k,2k],[2k,3k], ... ,[1,k+1],[k+1,2K+1], ... ,[2,k+2],[k+2,2k+2]
        int counter=0;
        int shift=0; //consumed on the left
        for (int i = 0; i < merOrder.length ; i++) {
            if (shift==k) {break;}
            for (int j = 0; j < (merOrder.length/k)+1; j++) { //+1 to get the last incomplete mer (length<k)
                if ((shift+j*k)<merOrder.length) {
                    merOrder[counter]=shift+j*k;
                    counter++;
                }
            }
            shift++;
        }
    }
    
    public int getStep() {
        return this.step;
    }
    

    
}
