package input;

import java.util.Scanner;

public class MyScanner {
    public static void main(String[] args) {
        System.out.println("Give me a number for x, please.");
        Scanner scan = new Scanner(System.in);
        int num = scan.nextInt();
        System.out.println("Got `" + num + "`.");
    }
}