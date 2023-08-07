package comp512st.tiapp;

import comp512.gcl.*;
import comp512.ti.*;
import comp512.utils.*;

import comp512st.paxos.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.Arrays;

import java.util.logging.*;

public class TreasureIslandApp implements Runnable
{
	TreasureIsland ti;
	private Logger logger;

	Thread tiThread;
	boolean keepExploring;

	Paxos paxos;

	public TreasureIslandApp(Paxos paxos, Logger logger, String gameId, int numPlayers, int yourPlayer)
	{
		this.paxos = paxos;
		this.logger = logger;
		this.keepExploring = true;
		ti = new TreasureIsland(logger, gameId, numPlayers, yourPlayer);
		tiThread = new Thread(this);
		tiThread.start();
	}

	public void run()
	{
		while(keepExploring) // TODO: Make sure all the remaining messages are processed in the case of a graceful shutdown.
		{
			try
			{
				Object[] info  = (Object[]) paxos.acceptTOMsg();
				logger.fine("Received :" + Arrays.toString(info));
				move((Integer)info[0], (Character)info[1]);
				displayIsland();
			}
			catch(InterruptedException ie)
			{
				if(keepExploring)
					logger.log(Level.SEVERE, "Encountered InterruptedException while waiting for messages.", ie);
				break;
			}
		}
	}

	public void displayIsland()
	{
		ti.displayIsland();
	}

	public synchronized void move(int playerNum, char direction)
	{
		move(playerNum, direction, true);
	}

	public synchronized void move(int playerNum, char direction, boolean displayIsland)
	{
		ti.move(playerNum, direction);
		if(displayIsland)
			ti.displayIsland();
	}

	public static void main(String args[]) throws IOException, InterruptedException
	{

		if(args.length != 5)
		{
			System.err.println("Usage: java comp512st.tiapp.TreasureIslandApp processhost:port processhost:port,processhost:port,... gameid numplayers playernum");
			System.exit(1);
		}

		// Setup logger and logging levels.
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS.%1$tN %1$Tp %2$s %4$s: %5$s%6$s%n");
		Logger logger = Logger.getLogger("TreasureIsland");
		logger.setLevel(Level.FINE);

		/*
		Handler consoleHandler = new ConsoleHandler();
		//logger.setLevel(Level.WARNING);
		//logger.setLevel(Level.INFO);
		//logger.setLevel(Level.ALL);
		consoleHandler.setLevel(Level.FINE);
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
		*/

		// Send logging to a file.
    try
    {
		// Put log files into log folder for organization
		Files.createDirectories(Paths.get("./logs"));
		// Add timestamp to log for easier retrieval and sorting
		FileHandler fh = new FileHandler("./logs/" + (new SimpleDateFormat("yyyy-MM-dd+HH-mm-ss").format(new Date())) + "." + args[2]+"-"+args[0].replace(':','.')+ '-' + args[4] + "-processinfo-.log");
		logger.addHandler(fh);
		SimpleFormatter formatter = new SimpleFormatter();
		fh.setFormatter(formatter);
		logger.setUseParentHandlers(false);
    }
    catch(SecurityException se) {
		se.printStackTrace();
		throw new RuntimeException("SecurityException while initializing process log file.");
	}
    catch(IOException ie) {
		ie.printStackTrace();
		throw new RuntimeException("IOException while initializing process log file.");
	}

		logger.info("Started with arguments : " + Arrays.toString(args));

		// For simulating any failure conditions.
		FailCheck failCheck = new FailCheck(logger);

		String gameid  = args[2]; // pass a different gameid to get a different island map.
		int numPlayers = Integer.parseInt(args[3]); // total number of players in the game
		int playerNum  = Integer.parseInt(args[4]); // your player number / id

		Paxos paxos = new Paxos(args[0], args[1].split(","), logger, failCheck) ;
		TreasureIslandApp ta = new TreasureIslandApp(paxos, logger, gameid, numPlayers, playerNum);
		ta.displayIsland();

		Scanner sc = new Scanner(System.in);
		while(true) // Just keep polling for the user's input.
		{
			String cmd = sc.next().toUpperCase();
			logger.fine("cmd is : " + cmd);
			if(cmd.equals("E")) break;

			// Allow using more intuitive control scheme
			switch (cmd) {
				case "T":
				case "5":
					cmd = "U";
					break;
				case "F":
				case "1":
					cmd = "L";
					break;
				case "G":
				case "2":
					cmd = "D";
					break;
				case "H":
				case "6":
					cmd = "R";
					break;
			}

			switch(cmd)
			{
				case "L":
				case "R":
				case "U":
				case "D": // Capture the move and broadcast it to everyone along with the player number.
					// Remember, this should block till this move has been accepted by the majority.
					//	The logic for that should be built into the paxos module.
					paxos.broadcastTOMsg(new Object[]{ playerNum, cmd.charAt(0) });
					break;
				case "FI": // The process is to fail immediately.
					failCheck.setFailurePoint(FailCheck.FailureType.IMMEDIATE);
					break;
				case "FRP": // The process is supposed to fail immediately when it receives a propose message.
					failCheck.setFailurePoint(FailCheck.FailureType.RECEIVEPROPOSE);
					break;
				case "FSV": // The process is supposed to fail immediately after it sends a vote for leader election.
					failCheck.setFailurePoint(FailCheck.FailureType.AFTERSENDVOTE);
					break;
				case "FSP": // The process is supposed to fail immediately after it sends a proposal to become leader.
					failCheck.setFailurePoint(FailCheck.FailureType.AFTERSENDPROPOSE);
					break;
				case "FOL": // The process is supposed to fail immediately after a majority has accepted it as the leader.
					failCheck.setFailurePoint(FailCheck.FailureType.AFTERBECOMINGLEADER);
					break;
				case "FMV": // The process is supposed to fail immediately after a majority has accepted it’s proposed value.
					failCheck.setFailurePoint(FailCheck.FailureType.AFTERVALUEACCEPT);
					break;
				default:
					String msg = "Command " + cmd + " is not a valid command for this app.";
					System.err.println(msg);
					logger.warning(msg);
			}
		}

		String msg = "Shutting down Paxos...";
		System.out.println(msg);
		logger.info(msg);
		ta.keepExploring = false;
		ta.tiThread.join(1000); // Wait maximum 1s for the app to process any more incoming messages that was in the queue.
		paxos.shutdownPaxos(); // shutdown paxos.
		ta.tiThread.interrupt(); // interrupt the app thread if it has not terminated.
		ta.displayIsland(); // display the final map
		msg = "Process terminated.";
		System.out.println(msg);
		logger.info(msg);
		System.exit(0);
	}
}
