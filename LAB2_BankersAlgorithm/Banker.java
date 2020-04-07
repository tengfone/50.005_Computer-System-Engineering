import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

public class Banker {
	private int numberOfCustomers; // the number of customers
	private int numberOfResources; // the number of resources

	private int[] available; // the available amount of each resource
	private int[][] maximum; // the maximum demand of each customer
	private int[][] allocation; // the amount currently allocated
	private int[][] need; // the remaining needs of each customer |||||||||||||| MAX - ALLOCATION

	/**
	 * Constructor for the Banker class.
	 * 
	 * @param resources         An array of the available count for each resource.
	 * @param numberOfCustomers The number of customers.
	 */
	public Banker(int[] resources, int numberOfCustomers) {
		// TODO: set the number of resources
		numberOfResources = resources.length;
		// TODO: set the number of customers
		this.numberOfCustomers = numberOfCustomers;
		// TODO: set the value of bank resources to available
		available = resources;
		// TODO: set the array size for maximum, allocation, and need
		maximum = new int[numberOfCustomers][numberOfResources];
		allocation = new int[numberOfCustomers][numberOfResources];
		need = new int[numberOfCustomers][numberOfResources];
	}

	/**
	 * Sets the maximum number of demand of each resource for a customer.
	 * 
	 * @param customerIndex The customer's index (0-indexed).
	 * @param maximumDemand An array of the maximum demanded count for each
	 *                      resource.
	 */
	public void setMaximumDemand(int customerIndex, int[] maximumDemand) {
		// TODO: add customer, update maximum and need
		maximum[customerIndex] = maximumDemand; // customer indexes
		for (int i = 0; i < numberOfResources; i++) { // need n by m matrix, need[i,j] = k
			need[customerIndex][i] = maximumDemand[i] - allocation[customerIndex][i];
		}
	}

	/**
	 * Prints the current state of the bank.
	 */
	public void printState() {
		System.out.println("\nCurrent state:");
		// print available
		System.out.println("Available:");
		System.out.println(Arrays.toString(available));
		System.out.println("");

		// print maximum
		System.out.println("Maximum:");
		for (int[] aMaximum : maximum) {
			System.out.println(Arrays.toString(aMaximum));
		}
		System.out.println("");
		// print allocation
		System.out.println("Allocation:");
		for (int[] anAllocation : allocation) {
			System.out.println(Arrays.toString(anAllocation));
		}
		System.out.println("");
		// print need
		System.out.println("Need:");
		for (int[] aNeed : need) {
			System.out.println(Arrays.toString(aNeed));
		}
		System.out.println("");
	}

	/**
	 * Requests resources for a customer loan. If the request leave the bank in a
	 * safe state, it is carried out.
	 * 
	 * @param customerIndex The customer's index (0-indexed).
	 * @param request       An array of the requested count for each resource.
	 * @return true if the requested resources can be loaned, else false.
	 */
	public synchronized boolean requestResources(int customerIndex, int[] request) {
		// TODO: print the request
		System.out.println("Customer " + customerIndex + " requesting");
		System.out.println(Arrays.toString(request));
		for (int i = 0; i < numberOfResources; i++) {
			// TODO: check if request larger than need
			if (request[i] > need[customerIndex][i])
				return false;
		}
		for (int i = 0; i < numberOfResources; i++) {
			// TODO: check if request larger than available
			if (request[i] > available[i])
				return false;
		}
		// if safe
		if (checkSafe(customerIndex, request)) {
			for (int i = 0; i < numberOfResources; i++) {
				available[i] -= request[i];
				allocation[customerIndex][i] += request[i];
				need[customerIndex][i] -= request[i];
			}
			return true;
		} else return false;
	}

	/**
	 * Releases resources borrowed by a customer. Assume release is valid for
	 * simplicity.
	 * 
	 * @param customerIndex The customer's index (0-indexed).
	 * @param release       An array of the release count for each resource.
	 */
	public synchronized void releaseResources(int customerIndex, int[] release) {
		// TODO: print the release
		System.out.println("Customer " + customerIndex + " releasing");
		System.out.println(Arrays.toString(release));

		// TODO: release the resources from customer customerNumber
		for (int i = 0; i < numberOfResources; i++) {
			available[i] += release[i];
			allocation[customerIndex][i] -= release[i];
			need[customerIndex][i] += release[i];
		}
	}

	/**
	 * Checks if the request will leave the bank in a safe state.
	 * 
	 * @param customerIndex The customer's index (0-indexed).
	 * @param request       An array of the requested count for each resource.
	 * @return true if the requested resources will leave the bank in a safe state,
	 *         else false
	 */
	private synchronized boolean checkSafe(int customerIndex, int[] request) {
		// init
		int[] temp_avail = Arrays.copyOf(available, available.length);
		int[][] temp_need = Arrays.copyOf(need,need.length);
		int[][] temp_allocation = Arrays.copyOf(allocation, allocation.length);
		int[] work = new int[numberOfCustomers];
		
		boolean[] finish = new boolean[numberOfCustomers];
		boolean possible;
		boolean toWork = true;

		for (int i = 0; i < numberOfResources; i++) {
			temp_avail[i] -= request[i];
			temp_need[customerIndex][i] -= request[i];
			temp_allocation[customerIndex][i] += request[i];
		}

		// work = temp_avail;
		work = temp_avail;

		// finish(all) = false;
		for (int i = 0; i < numberOfCustomers; i++) {
			finish[i] = false;
		}

		// possible = true;
		possible = true;

		while (possible) {
			possible = false;
			for (int i = 0; i < numberOfCustomers; i++) {
				toWork = true;
				for (int z = 0; z < numberOfResources; z++) {
					if (temp_need[i][z] > work[z])
						toWork = false;
				}
				if (finish[i] == false && toWork) {
					possible = true;
					for (int j = 0; j < numberOfResources; j++) {
						work[j] += temp_allocation[i][j];
					}
					finish[i] = true;
				}
			}
		}

		// manipulate back
		for(int i =0; i < numberOfResources; i++){
			temp_allocation[customerIndex][i] -= request[i];
			temp_need[customerIndex][i] += request[i];
		}
		
		// return(finish(all) == true)
		for (int i = 0; i < numberOfCustomers; i++) {
			if (finish[i] == false)
				return false;
		}
		return true;
	}

	/**
	 * Parses and runs the file simulating a series of resource request and
	 * releases. Provided for your convenience.
	 * 
	 * @param filename The name of the file.
	 */
	public static void runFile(String filename) {

		try {
			BufferedReader fileReader = new BufferedReader(new FileReader(filename));

			String line = null;
			String[] tokens = null;
			int[] resources = null;

			int n, m;

			try {
				n = Integer.parseInt(fileReader.readLine().split(",")[1]);
			} catch (Exception e) {
				System.out.println("Error parsing n on line 1.");
				fileReader.close();
				return;
			}

			try {
				m = Integer.parseInt(fileReader.readLine().split(",")[1]);
			} catch (Exception e) {
				System.out.println("Error parsing n on line 2.");
				fileReader.close();
				return;
			}

			try {
				tokens = fileReader.readLine().split(",")[1].split(" ");
				resources = new int[tokens.length];
				for (int i = 0; i < tokens.length; i++)
					resources[i] = Integer.parseInt(tokens[i]);
			} catch (Exception e) {
				System.out.println("Error parsing resources on line 3.");
				fileReader.close();
				return;
			}

			Banker theBank = new Banker(resources, n);

			int lineNumber = 4;
			while ((line = fileReader.readLine()) != null) {
				tokens = line.split(",");
				if (tokens[0].equals("c")) {
					try {
						int customerIndex = Integer.parseInt(tokens[1]);
						tokens = tokens[2].split(" ");
						resources = new int[tokens.length];
						for (int i = 0; i < tokens.length; i++)
							resources[i] = Integer.parseInt(tokens[i]);
						theBank.setMaximumDemand(customerIndex, resources);
					} catch (Exception e) {
						System.out.println("Error parsing resources on line " + lineNumber + ".");
						fileReader.close();
						return;
					}
				} else if (tokens[0].equals("r")) {
					try {
						int customerIndex = Integer.parseInt(tokens[1]);
						tokens = tokens[2].split(" ");
						resources = new int[tokens.length];
						for (int i = 0; i < tokens.length; i++)
							resources[i] = Integer.parseInt(tokens[i]);
						theBank.requestResources(customerIndex, resources);
					} catch (Exception e) {
						System.out.println("Error parsing resources on line " + lineNumber + ".");
						fileReader.close();
						return;
					}
				} else if (tokens[0].equals("f")) {
					try {
						int customerIndex = Integer.parseInt(tokens[1]);
						tokens = tokens[2].split(" ");
						resources = new int[tokens.length];
						for (int i = 0; i < tokens.length; i++)
							resources[i] = Integer.parseInt(tokens[i]);
						theBank.releaseResources(customerIndex, resources);
					} catch (Exception e) {
						System.out.println("Error parsing resources on line " + lineNumber + ".");
						fileReader.close();
						return;
					}
				} else if (tokens[0].equals("p")) {
					theBank.printState();
				}
			}
			fileReader.close();
		} catch (IOException e) {
			System.out.println("Error opening: " + filename);
		}

	}

	/**
	 * Main function
	 * 
	 * @param args The command line arguments
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			runFile(args[0]);
		}
	}

}