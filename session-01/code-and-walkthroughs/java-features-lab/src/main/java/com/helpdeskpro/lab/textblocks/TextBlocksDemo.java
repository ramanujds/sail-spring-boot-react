package com.helpdeskpro.lab.textblocks;

/**
 * Text blocks: run this before and after you complete TODO 4.1.
 */
public class TextBlocksDemo {

    public static void main(String[] args) {
        System.out.println("--- concatenated (provided) ---");
        System.out.println(TicketQueries.byStatusAndPriorityConcatenated());

        System.out.println("--- text block (TODO 4.1) ---");
        try {
            System.out.println(TicketQueries.byStatusAndPriority());
        } catch (UnsupportedOperationException e) {
            System.out.println(e.getMessage());
        }
    }
}
