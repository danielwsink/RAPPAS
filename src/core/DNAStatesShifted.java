/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import etc.Infos;
import etc.exceptions.NonIUPACStateException;
import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author ben
 */
public class DNAStatesShifted extends AbstractStates implements Serializable {
 
    private static final long serialVersionUID = 6003L;
    
    //compression of 4 DNA residues in 1 byte, 2 bits per residue.
    //if 4 bases, in 1 byte:  00|00|00|00
    //          4 positions:   3  2  1  0
    //00000011 mask for position 0
    //00001100 mask for position 1
    //00110000 mask for position 2
    //11000000 mask for position 3
    byte[] maskArray={(byte)0x03,(byte)0x0C,(byte)0x30,(byte)0xC0};
    
    char[] states = {'A','T','C','G','N','-','.'};
    HashMap<Character,Boolean> convertedToN=new HashMap<>();
    byte[] bytes = {(byte)0x00,(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x04,(byte)0x05,(byte)0x06};     

    public DNAStatesShifted() {
        //ambigous states which are allowed
        ambigousStates=3;
        //fill hashmap that correspond to IUPAC code
        convertedToN.put('R', Boolean.TRUE);convertedToN.put('r', Boolean.TRUE);
        convertedToN.put('Y', Boolean.TRUE);convertedToN.put('y', Boolean.TRUE);
        convertedToN.put('S', Boolean.TRUE);convertedToN.put('s', Boolean.TRUE);
        convertedToN.put('W', Boolean.TRUE);convertedToN.put('w', Boolean.TRUE);
        convertedToN.put('K', Boolean.TRUE);convertedToN.put('k', Boolean.TRUE);
        convertedToN.put('M', Boolean.TRUE);convertedToN.put('m', Boolean.TRUE);
        convertedToN.put('B', Boolean.TRUE);convertedToN.put('b', Boolean.TRUE);
        convertedToN.put('D', Boolean.TRUE);convertedToN.put('d', Boolean.TRUE);
        convertedToN.put('H', Boolean.TRUE);convertedToN.put('h', Boolean.TRUE);
        convertedToN.put('V', Boolean.TRUE);convertedToN.put('v', Boolean.TRUE);
    }
    
    
    
    
    /**
     * compress DNA mer into a mer.size/4 byte array, compressing 4 residues per byte
     * @param bytes
     * @return 
     */
    @Override
    public byte[] compressMer(byte[] bytes) {
        //chose number of bytes necessary to represent this DNA
        //we can code 4 bases in 1 byte
        int byteCount=new Double(Math.ceil((0.0+bytes.length)/4)).intValue();
        //the kmer compressed in byteCount bytes
        byte[] kmer=new byte[byteCount];
        
        byte fourBasesByte=0x00;
        int insertions=0;
        for (int i = 0; i < bytes.length; i++) {
            if ( (i>0) & (i%4==0) ) {      
                //instance copy this byte in byte array
                kmer[(i/4)-1]=new Byte(fourBasesByte).byteValue();
                //reset pivot byte
                fourBasesByte=0x00;
            }
            //shift the 2 bits coding the i-th base, in the i%4 byte
            fourBasesByte= (byte) ( fourBasesByte | (bytes[i] << (2*(i%4))) ) ;            
        }
        //last mer fragment was last than 4 bases
        //use a complete byte, and at expansion, k will be used to know how
        //many bit pairs will need to be read.
        if (insertions<kmer.length) {
            kmer[kmer.length-1]=fourBasesByte;
        }

        return kmer;
        
    }
    
    /**
     * expand compressed DNA mer into char array
     * @param mer
     * @param k
     * @return 
     */
    @Override
    public char[] expandMer(byte[] mer, int k) {
        
        char[] charMer=new char[k];
        
        for (int i = 0; i < mer.length; i++) {
            byte b = mer[i];
            for (int j = 0; j < 4; j++) {
                //to skip last unused bits
                if ( (i*4)+j > k-1) {
                    break;
                }
                int residue=b;
                residue= (byte) (b & maskArray[j]) ;
                //masking by 0x00FF necessary to avoid that the head bits of integer
                //shifted too (when shift > 5), as in java they are 1 (positive/negative interval of int) by default
                residue=residue & 0x00FF; 
                residue= (byte) (residue >> (j*2));
                charMer[(i*4)+j]=byteToState((byte)residue);
            }
        }
        return charMer;
    }

    /**
     * 
     * @param c
     * @return
     * @throws NonIUPACStateException 
     */
    @Override
    protected byte charToByte(char c) throws NonIUPACStateException {
        byte b=-1;
        switch (c) {
            case 'a':
                b=0x00; break;
            case 'A':
                b=0x00; break;
            case 't':
                b=0x01; break;
            case 'T':
                b=0x01; break;
            case 'u':
                b=0x01; break;
            case 'U':
                b=0x01; break; 
            case 'c':
                b=0x02; break;
            case 'C':
                b=0x02; break;
            case 'g':
                b=0x03; break;
            case 'G':
                b=0x03; break;
            case 'N':
                b=0x04; break;
            case 'n':
                b=0x04; break;
            case '-':
                b=0x05; break;
            case '.':
                b=0x06; break;  
            default:
                if (convertedToN.containsKey(c)) {
                    Infos.println("Unexpected IUPAC state in the sequence (not ATUCGN-.), replaced with N. (char='"+String.valueOf(c)+"')");
                    b=0x04;
                } else {
                    throw new NonIUPACStateException(this, c);
                }
                break; //put N if other IUPAC base
        }
        return b;
    }
    
    @Override
    public char byteToState(byte b) {
        return states[b];
    }
    
    @Override
    public byte stateToByte(char c) throws NonIUPACStateException{
        return bytes[charToByte(c)];
    }
    

    @Override
    public String getSequence(byte[] bytes) {
        char[] c=new char[bytes.length];
        for (int i = 0; i < c.length; i++) {
            c[i]=states[bytes[i]];
            
        }
        return new String(c);
    }

    @Override
    public int getStateCount() {
        return states.length;
    }
    
    @Override
    public int getNonAmbiguousStatesCount() {
        return states.length-ambigousStates;
    }

    @Override
    public int stateToInt(char c) throws NonIUPACStateException {
        if (c=='U') {
            c='T';
        }
        return bytes[charToByte(c)];
    }
    
    
    
    
}
