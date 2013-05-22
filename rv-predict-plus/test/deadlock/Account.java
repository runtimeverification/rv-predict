package deadlock;

public class Account {
	
	private int balance;
	
	Account(int money)
	{
		this.balance = money;
	}
	public int getBalance()
	{
		return balance;
	}
	public synchronized void deposit(Account a, int i)
	{
		i = a.withdraw(i);
		balance = balance+i;
	}
	public synchronized int withdraw(int amt)
	{
		if(balance>=amt)
		{
			balance = balance - amt;
			return amt;
		}
		else
		{
			return 0;
		}
	}
}
