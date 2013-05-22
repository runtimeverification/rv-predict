package deadlock;

public class Client extends Thread {

	private Account a1, a2;
	
	Client(Account a1, Account a2)
	{
		this.a1 = a1;
		this.a2 = a2;
	}
	
	public void run()
	{
		move(a1,a2);
	}
	
	private synchronized void move(Account a1, Account a2)
	{
		a2.deposit(a1,100);
	}
}
