package deadlock;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try{
		Account a1 = new Account(1000);
		Account a2 = new Account(1000);

		Client client1 = new Client(a1,a2);
		Client client2 = new Client(a2,a1);
		
		client1.start();
		client2.start();
		
		client1.join();
		client2.join();
		
		System.out.println(a1.getBalance()+" "+a2.getBalance());
		
		}catch(Exception e)
		{
			e.printStackTrace();
		}	

	}

}
