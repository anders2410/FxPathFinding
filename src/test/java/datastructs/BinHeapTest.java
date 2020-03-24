package datastructs;

import datastructures.BinaryHeapPriorityQueue;
import model.Edge;
import model.Graph;
import model.Node;
import model.Util;
import org.junit.Before;
import org.junit.Test;
import paths.AlgorithmMode;
import paths.SSSP;
import paths.ShortestPathResult;
import pbfparsing.PBFParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static paths.SSSP.*;

public class BinHeapTest {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.println("Binary Heap Test\n\n");
        System.out.println("Enter size of Binary heap");
        /** Make object of BinaryHeap **/
        Comparator<Integer> objectComparator = Comparator.comparingInt(i -> i);
        BinaryHeapPriorityQueue bh = new BinaryHeapPriorityQueue(objectComparator, scan.nextInt());

        char ch;
        /**  Perform Binary Heap operations  **/
        do {
            System.out.println("\nBinary Heap Operations\n");
            System.out.println("1. insert ");
            System.out.println("2. delete min");
            System.out.println("3. check full");
            System.out.println("4. check empty");
            System.out.println("5. clear");

            int choice = scan.nextInt();
            switch (choice) {
                case 1:
                    System.out.println("Enter integer element to insert");
                    bh.insert(scan.nextInt());
                    break;
                case 2:
                    try {
                        System.out.println("Min Element : " + bh.poll());
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                case 3:
                    System.out.println("Full status = " + bh.isFull());
                    break;
                case 4:
                    System.out.println("Empty status = " + bh.isEmpty());
                    break;
                case 5:
                    bh.reset();
                    System.out.println("Heap Cleared\n");
                    break;
                default:
                    System.out.println("Wrong Entry \n ");
                    break;
            }
            /** Display heap **/
            System.out.println(bh);
            System.out.println("\nDo you want to continue (Type y or n) \n");
            ch = scan.next().charAt(0);
        } while (ch == 'Y' || ch == 'y');
    }
}
