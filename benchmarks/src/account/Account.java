package account;

import java.util.*;

/*
 * The Bank class is a demo for a multi-threaded system which manages accounts
 * while keeping track of their internal balance. The chance for error is very low
 * although possible for the lack of synchronization.
 */
public class Account{

	// Total balance as recorded in bank.
	 public static int Bank_Total = 0;
	
	// all accounts
	 public static BankAccount[] accounts; 
	
	// random numbers generator
	 public static Random Bank_random = new Random(1);
	
	// The number of accounts is randomly chosen from [10,110]
	// int NUM_ACCOUNTS = Math.abs((Bank_random.nextInt()%10 + 1)*10);
	 int NUM_ACCOUNTS =2;
	/*
	 * Method main creates all the accounts from which the Bank accepts requests
	 * for actions. The total sum of the accounts is recorded on each
	 * action execution.
	 */
	public static void main(String args[]){
		new Account().go(args);
	}
	public void go(String[] args)
	{
		
		accounts = new BankAccount[NUM_ACCOUNTS];

		// create all accounts
		for(int i = 0; i< NUM_ACCOUNTS; i++){
			accounts[i] = new BankAccount(i);
		}
	
		System.out.println("Bank system started");

		// start all accounts
		for(int i = 0; i< NUM_ACCOUNTS; i++){
			accounts[i].start();
		}

		// wait for all threads (accounts) to die.
		for(int i = 0; i< NUM_ACCOUNTS; i++){
			if(accounts[i].isAlive()){
				i = 0;
				// if some are alive, sleep for a while
				try{
					Thread.sleep(500);
				}catch(Exception exception){
				}
			}
		}	

		System.out.println("");
		System.out.println("End of the week.");

		int Total_Balance = 0;
		// sum up all balances.
		for(int i = 0; i< NUM_ACCOUNTS; i++){
			Total_Balance += accounts[i].Balance;
		}	
		
		checkResult(Total_Balance);
	}
	public void checkResult(int Total_Balance)
	{
		// Give report.
		System.out.println("Bank records = "+Bank_Total+", accounts balance = "+Total_Balance+".");
		if(Bank_Total == Total_Balance)
			System.out.println("Records match.");
		else
		{
			try{
				throw new RuntimeException("ERROR: records don't match !!!");
			}catch(Exception e)
			{
				e.printStackTrace();
           		"leap_Crashed_with".equals(e);
          		
          		System.exit(-1);
			}
		}
	}

	/*
	 * The Service method performs the actual action on the account, 
	 * and it also updates the Bank's records. (Bank_Total)
	 */
	public static void Service(int id,int sum){
		accounts[id].Balance += sum;
		Bank_Total += sum;
	}
	
	
	
}

