package comp512st.tests;

import comp512.gcl.*;
import comp512.ti.*;
import comp512.utils.*;

import comp512st.paxos.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import java.util.Random;

import java.util.logging.*;

import java.time.*;

public class TreasureIslandAppAuto implements Runnable
{
	TreasureIsland ti;
	private Logger logger;

	Thread tiThread;
	boolean keepExploring;
	boolean updateDisplay;

	Paxos paxos;

	public TreasureIslandAppAuto(Paxos paxos, Logger logger, String gameId, int numPlayers, int yourPlayer)
	{
		this.paxos = paxos;
		this.logger = logger;
		this.keepExploring = true;
		this.updateDisplay = false;
		ti = new TreasureIsland(logger, gameId, numPlayers, yourPlayer);

    try
    {
      String updDisplay = System.getenv("UPDATEDISPLAY");
      if(updDisplay != null && updDisplay.equalsIgnoreCase("true"))
      {
        this.updateDisplay = true;
        logger.info("UPDATEDISPLAY is set to " + updateDisplay + ", display will be constantly updated.");
      }
    }
    catch(SecurityException se)
    { logger.log(Level.WARNING, "Encountered SecurityException while trying to access the environment variable UPDATEDISPLAY.", se); }

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
				move((Integer)info[0], (Character)info[1], updateDisplay);
				//displayIsland(); //we do not want to keep constantly refreshing the output display.
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

	private static class AutoMoveGenerator
	{
			private final static String[] VALIDMOVES = { "L", "R", "U", "D" }; 
			public final static Map<String, String> FAILMODECODES = new HashMap<>();
			static
			{
				FAILMODECODES.put("IMMEDIATE","FI");
				FAILMODECODES.put("RECEIVEPROPOSE","FRP");
				FAILMODECODES.put("AFTERSENDVOTE","FSV");
				FAILMODECODES.put("AFTERSENDPROPOSE","FSP");
				FAILMODECODES.put("AFTERBECOMINGLEADER","FOL");
				FAILMODECODES.put("AFTERVALUEACCEPT","FMV");
			}

			private Random rand;
			private int maxmoves;
			private int interval;
			private String failmode;

			private Logger logger;

			private Clock clock;
			private long lastmovetime = 0L;
			private long totalmoves = 0L;

			private boolean sendfail;

			public AutoMoveGenerator(int maxmoves, int interval, String randseed, String failmode, Logger logger)
			{
				this.maxmoves = maxmoves;
				this.interval = interval;
				this.failmode = failmode;

				this.sendfail = false;

				this.logger = logger;

				if(this.failmode != null && FAILMODECODES.get(failmode) == null)
				{
					logger.log(Level.SEVERE, "Incorrect failmode parameter " + failmode);
					throw new IllegalArgumentException("Incorrect failmode parameter " + failmode);
				}

				this.rand = new Random(randseed.hashCode());
				logger.info("AutoMoveGenerator setup maxmoves = " + this.maxmoves + ", interval = " + this.interval + ", randseed = " + randseed +  ", failmode = " + this.failmode);

				clock = Clock.systemDefaultZone();
			}

			public String nextMove()
			{
				int moveid = rand.nextInt(maxmoves); // used to make a random move / fail.
				if(maxmoves < 10) // for very small maxmoves, we do not want to fail quickly. So pick a larger number.
					moveid = rand.nextInt(20);

				logger.fine("moveid = " + moveid);

				long currenttime = clock.millis();
				if(currenttime - lastmovetime < interval) // we have some more time left before next move, so pause a bit.
					try { Thread.sleep(interval - (currenttime - lastmovetime)); } catch (InterruptedException ie) { logger.log(Level.SEVERE, "Encountered InterruptedException while pausing for the next move."); }

				if(totalmoves == maxmoves)  // we have done max moves.
				{ 
					lastmovetime = clock.millis();
					return "E";
				}

				if(failmode != null && !sendfail && moveid < 5) // For lower numbers, initiate a failure, if enabled.
				{
					sendfail = true;
					lastmovetime = clock.millis();
					return FAILMODECODES.get(this.failmode);
				}

				totalmoves++;
				lastmovetime = clock.millis();
				return VALIDMOVES[rand.nextInt(VALIDMOVES.length)];
			}
	}

	public static void main(String args[]) throws IOException, InterruptedException
	{
		if(args.length < 5)
		{
			System.err.println("Usage: java comp512st.tiapp.TreasureIslandAppAuto processhost:port processhost:port,processhost:port,... gameid numplayers playernum maxmoves interval randseed [FAILMODE]");
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

		int maxmoves = Integer.parseInt(args[5]); // maximum number of automatic moves to be generated
		int interval = Integer.parseInt(args[6]); // time to pause after the last move was successfull.
		String randseed = args[7]; // Use to seed the random move generator.
		String failmode = null;
		if(args.length == 9) // Are we asked to install a fail point?
			failmode = args[8];

		Paxos paxos = new Paxos(args[0], args[1].split(","), logger, failCheck) ;
		TreasureIslandAppAuto ta = new TreasureIslandAppAuto(paxos, logger, gameid, numPlayers, playerNum);
		ta.displayIsland(); // display the initial map

		AutoMoveGenerator moveGen = new AutoMoveGenerator(maxmoves, interval, randseed, failmode, logger);
		while(true) // Just keep polling for the user's input.
		{
			String cmd = moveGen.nextMove();
			logger.fine("cmd is : " + cmd);
			if(cmd.equals("E")) break;

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
					logger.warning("Command " + cmd + " is not a valid command for this app.");
			}
		}

		logger.info("Done with all my moves ..."); // we just chill for a bit to ensure we got all the messages from others before we shutdown.
																							// May have to increase this for higher maxmoves and smaller intervals.
		try{ Thread.sleep(5000); } catch (InterruptedException ie) { logger.log(Level.SEVERE, "I got InterruptedException when I was chilling after all my moves.", ie); }
		ta.keepExploring = false;
		ta.tiThread.join(1000); // Wait maximum 1s for the app to process any more incoming messages that was in the queue.
		String msg = "Shutting down Paxos...";
		System.out.println(msg);
		logger.info(msg);
		paxos.shutdownPaxos(); // shutdown paxos.
		ta.tiThread.interrupt(); // interrupt the app thread if it has not terminated.
		ta.displayIsland(); // display the final map
		msg = "Process terminated.";
		System.out.println(msg);
		logger.info(msg);
		System.exit(0);
	}
}
