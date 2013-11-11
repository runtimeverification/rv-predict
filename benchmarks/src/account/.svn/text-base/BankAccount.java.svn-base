package account;

import java.util.*;

/*
 * The Account class represents the actual accounts in the Bank.
 * It's run() method simulates actions on the account.
 */
class BankAccount extends Thread {

	// The balance of the account.
	public int Balance = 0;
	
	// The account id (allocated by the bank)
	public int Account_Id = 0;
	
	// Maximum sum for a single action
	static int MAX_SUM = 300;
	
	// Internal randomizer for the account (simulates account owner)
	Random random = new Random(Account.Bank_random.nextInt());
	
	/*
	 * The constructor initiates the account's id.
	 */
	BankAccount(int id){
		Account_Id = id;
	}

	/*
	 * The Action method simulates an action on the account, 
	 * where the sum of the action is randomly chosen, and 
	 * then the bank is requested for a service.
	 */
	public void Action(){

		// get a random sum in the range [0,MAX_SUM]
		int sum = random.nextInt()%MAX_SUM;
		System.out.println("sum: "+ sum);
		// perform action in Bank
		Account.Service(Account_Id,sum);
	}

	
	/*
	 * The run method performs the actual actions on the account, and
	 * it is supposed to simulate the account owner's actions during the week.
	 */
	public void run(){
		// maximum 500 actions
		//int loop = Math.abs(random.nextInt()%500);
		int loop = 2;
		System.out.println("loop: "+ loop);
		// perform action
		for(int i = 0; i< loop; i++){
			this.Action();
			//sleep a little
			try{
				Thread.sleep(10);
			}catch(Exception exception){
			}
		}
		System.out.print(".");
	}
}