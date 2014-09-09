package com.example;
 
public class Main
{
 
    private static int foo = 1;
 
    public static void main(String[] args) throws InterruptedException
    {
        new Thread(() -> foo = 2).start();
        new Thread(() -> foo = 3).start();
        new Thread(() -> System.out.println(foo)).start();
        new Thread(() -> System.out.println(foo)).start();
 
        Thread.sleep(1000);
    }
}