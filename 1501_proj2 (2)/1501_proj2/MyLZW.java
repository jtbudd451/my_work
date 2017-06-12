/******************************************************************************
 *  Compilation:  javac MyLZW.java
 *  Execution:    java MyLZW - < input.txt   (compress)
 *  Execution:    java MyLZW + < input.txt   (expand)
 *  Dependencies: BinaryIn.java BinaryOut.java
 *  Data files:   http://algs4.cs.princeton.edu/55compression/abraLZW.txt
 *                http://algs4.cs.princeton.edu/55compression/ababLZW.txt
 *
 *  Compress or expand binary input from standard input using LZW.
 *
 *  WARNING: STARTING WITH ORACLE JAVA 6, UPDATE 7 the SUBSTRING
 *  METHOD TAKES TIME AND SPACE LINEAR IN THE SIZE OF THE EXTRACTED
 *  SUBSTRING (INSTEAD OF CONSTANT SPACE AND TIME AS IN EARLIER
 *  IMPLEMENTATIONS).
 *
 *  See <a href = "http://java-performance.info/changes-to-string-java-1-7-0_06/">this article</a>
 *  for more details.
 *
 ******************************************************************************/

import edu.princeton.cs.algs4.BinaryStdIn;
import edu.princeton.cs.algs4.BinaryStdOut;
import edu.princeton.cs.algs4.TST;
import java.lang.Math;

/**
 *  The {@code MyLZW} class provides static methods for compressing
 *  and expanding a binary input using LZW compression over the 8-bit extended
 *  ASCII alphabet with 12-bit codewords.
 *  <p>
 *  For additional documentation,
 *  see <a href="http://algs4.cs.princeton.edu/55compress">Section 5.5</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public class MyLZW {
    private static int R = 256;        // number of input chars
    private static int L = 512;       // number of codewords = 2^W
    private static int W = 9;         // codeword width
    private static boolean n_mode;      // Boolean for Reset Mode 
    private static boolean r_mode;      // Boolean for Reset Mode 
    private static boolean m_mode;      // Boolean for Monitor Mode

    // vars for Monitor Mode
    private static boolean cb_full = false;      // Monitoring for code book full
    private static double unpressed_bits = 0;    // Counts the amount of data that has been read in so far
    private static double unpressed_bytes = 0;    // Counts the amount of data that has been read in so far in bytes
    private static double pressed_bits = 0;    // Counts the amount of data compressed
    private static double pressed_bytes = 0;    // Counts the amount of data compressed in bytes
    private static double ratio;        // read in / read out 
    private static double old_ratio;        // read in / read out 
    private static double test;             // variable for monitor mode 

    // Do not instantiate.
    private MyLZW() { }

    private static TST<Integer> new_book(){
        TST<Integer> st = new TST<Integer>();
        for (int i = 0; i < R; i++)
            st.put("" + (char) i, i);
        return st;
    }

    /**
     * Reads a sequence of 8-bit bytes from standard input; compresses
     * them using LZW compression with 12-bit codewords; and writes the results
     * to standard output.
     */
    public static void compress() {
        if (n_mode == true) BinaryStdOut.write('n');        // encode compression mode to n mode for decompression
        if (r_mode == true) BinaryStdOut.write('r');        // encode compression mode to r mode for decompression
        if (m_mode == true) BinaryStdOut.write('m');        // encode compression mode to m mode for decompression
        String input = BinaryStdIn.readString();
        TST<Integer> st = new_book();
        int code = R+1;  // R is codeword for EOF

        while (input.length() > 0) {
            String s = st.longestPrefixOf(input);  // Find max prefix match s.
            BinaryStdOut.write(st.get(s), W);      // Print s's encoding.
            int t = s.length();
            // Ration Math
            unpressed_bytes += t;
            pressed_bits += W;
            pressed_bytes = pressed_bits/8;                 // converts bits to bytes 
            ratio = unpressed_bytes/pressed_bytes;
            test = old_ratio/ratio;   

            if (code == L && W < 16) {              // if codebook full    
                W ++;                          
                L *= 2;                             // Resize code book up to 2^16
                System.err.println("Resizing codebook to "+L);
            }
            if (code == L && r_mode == true) {       // Reset Mode: code book is full at 2^16
                System.err.println("Reseting... number of bytes processed ");
                st = new_book();                    // Get a new code book filled will ASCII chars
                code = R+1;                         // R is codeword for EOF
                L = 512;                           // Reset values of L and W
                W = 9;
            }
            if (code == L && m_mode == true) {
                if (cb_full == false) {
                    System.err.println("The current ration is "+ratio);
                    old_ratio = ratio;
                    cb_full = true;
                }
                if (test >= 1.1) {
                    System.err.println("Compression Ration exceeds 1.1...Bytes processed: pressed_bits " + pressed_bytes + " in_ data "+ unpressed_bits);
                    st = new_book();                    // Get a new code book filled will ASCII chars
                    code = R+1;                         // R is codeword for EOF
                    L = 512;                           // Reset values of L and W
                    W = 9;
                    cb_full = false;
                }

            }

            if (t < input.length() && code < L)    // Add s to symbol table.
                st.put(input.substring(0, t + 1), code++);
            input = input.substring(t);            // Scan past s in input.
        }
        BinaryStdOut.write(R, W);
        BinaryStdOut.close();
    }

    /**
     * Reads a sequence of bit encoded using LZW compression with
     * 12-bit codewords from standard input; expands them; and writes
     * the results to standard output.
     */
    public static void expand() {
        char mode = BinaryStdIn.readChar();
        //if (mode == 'n') r_mode = true;   not necessary   char needs to be consumed though..
        if (mode == 'r') r_mode = true;
        if (mode == 'm') m_mode = true;
        String[] st = new String[65536];    // 65536 = 2^16 longest code we can have
        int i; // next available codeword value

        // initialize symbol table with all 1-character strings
        for (i = 0; i < R; i++)
            st[i] = "" + (char) i;
        st[i++] = "";                        // (unused) lookahead for EOF

        int codeword = BinaryStdIn.readInt(W);

        pressed_bits += W;   // compressed data from file 
        pressed_bytes = pressed_bits / 8;

        if (codeword == R) return;           // expanded message is empty string
        String val = st[codeword];

        while (true) {
            BinaryStdOut.write(val);
            // Ration Math
            unpressed_bytes += val.length();         // unpressed length value 
            ratio = unpressed_bytes/pressed_bytes;    // pressed data / unpressed data
            test = old_ratio/ratio;
            if (i == L && W < 16) {
                W++;
                L *= 2;                     // expand codebook size to 2^(W++)  Hack around using Math.pow.... returns a double;
                System.err.println("Resizing decompression codebook");
            }
            if (i == L && r_mode == true) {
                System.err.println("Reseting in reset mode...");
                st = new String[65536];
                // initialize symbol table with all 1-character strings
                for (i = 0; i < R; i++)
                    st[i] = "" + (char) i;
                st[i++] = "";                        // (unused) lookahead for EOF
                L = 512;
                W = 9;
            }
            if (i == L && m_mode == true) {
                if (cb_full == false) {
                    System.err.println("The current ration is "+ratio);
                    old_ratio = ratio;
                    cb_full = true;
                }
                if (test >= 1.1) {
                    System.err.println("Compression Ration exceeds 1.1...Bytes processed: pressed_bits " + pressed_bytes + " in_ data "+ unpressed_bytes);
                    st = new String[65536];
                    // initialize symbol table with all 1-character strings
                    for (i = 0; i < R; i++)
                        st[i] = "" + (char) i;
                    st[i++] = "";                        // (unused) lookahead for EOF
                    L = 512;                           // Reset values of L and W
                    W = 9;
                    cb_full = false;
                }
            }

            codeword = BinaryStdIn.readInt(W);
            unpressed_bits += W;

            if (codeword == R) break;
            String s = st[codeword];
            if (i == codeword) s = val + val.charAt(0);   // special case hack
            if (i < L) st[i++] = val + s.charAt(0);
            val = s;
        }
        BinaryStdOut.close();
    }

    /**
     * Sample client that calls {@code compress()} if the command-line
     * argument is "-" an {@code expand()} if it is "+".
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        if (args[0].equals("+")) expand();
        else if (args[0].equals("-")) {
            if (args[1].equals("n")) n_mode = true;
            if (args[1].equals("r")) r_mode = true;
            if (args[1].equals("m")) m_mode = true;
            compress();
        }
        else throw new IllegalArgumentException("Illegal command line argument");
    }

}

/******************************************************************************
 *  Copyright 2002-2016, Robert Sedgewick and Kevin Wayne.
 *
 *  This file is part of algs4.jar, which accompanies the textbook
 *
 *      Algorithms, 4th edition by Robert Sedgewick and Kevin Wayne,
 *      Addison-Wesley Professional, 2011, ISBN 0-321-57351-X.
 *      http://algs4.cs.princeton.edu
 *
 *
 *  algs4.jar is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  algs4.jar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with algs4.jar.  If not, see http://www.gnu.org/licenses.
 ******************************************************************************/
