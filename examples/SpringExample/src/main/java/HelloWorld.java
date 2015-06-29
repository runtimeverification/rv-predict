/**
 * Spring bean
 *
 */
public class HelloWorld {
    private String name;
    static Object lock = new Object();

    static int x=0;

    public void setName(String name) {
        this.name = name;
    }

    public void printHello() {
        System.out.println("Hello ! " + name);
        MyThread t = new MyThread();

        t.start();

        synchronized(lock)
        {
            x=1;//race here
        }

    }

    static class MyThread extends Thread
    {

        public void run()
        {

            synchronized(lock)
            {
                x=0;
            }

            System.out.println(x);

        }
    }
}

