package ventePriveMultiThread;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;

public class VentePriveMultiThread {
	//46348, andre
	//46865, DC
	//47466, test
	//46582, ANTONIO CROCE
	//47090, TI SENTO
	//46768, Bleu blan rouge
	private static String[] markCodes = {"46348", "46865", "46582"};


	public static void main(String[] args) {

		//countdown to ventePrive
		startCountdown(SingletonShare.targetDateStr);

		System.out.println("  SYSTEM CONFIGED: "+"["+SingletonShare.email+"]"+", targetDate:"+SingletonShare.targetDateStr);
		System.out.println("  SYSTEM START: "+new Date());
		try{
			
			//login phase
			SingletonShare.getInstance().loginPhase();

			ExecutorService executor = Executors.newFixedThreadPool(SingletonShare.THREADPOOL_FOR_MARK);
			Collection<Future<?>> futures = new LinkedList<Future<?>>();
			for(String markCode : markCodes){
				Runnable worker= new BusinessThread(markCode, "Universe");
				futures.add(executor.submit(worker));
			}
			//halt execution until the ExecutorService has processed all of the Runnable tasks
			for (Future<?> future:futures) {
				future.get();
			}
			executor.shutdown();
			executor.awaitTermination(60, TimeUnit.SECONDS);

		}catch (InterruptedException | ExecutionException | IOException e) {
			System.err.println("FATAL ERROR in calling main Business Thread: " +e.toString());
			e.printStackTrace();
		}finally{
			System.out.println("  In cart, we have "+SingletonShare.getInstance().getBoughtItems().size()+" items bought");
			System.out.println("  SYSTEM ENDS: "+new Date());
		}
	}

	private static void startCountdown(String targetDateStr){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		while(true){
			try {
				Long targetDateTime = sdf.parse(targetDateStr).getTime();
				Long currentDateTime = new Date().getTime();
				if(currentDateTime-targetDateTime>0){
					break;
				}else{
					System.out.println("  "+(new Date().toString())+"--SYSTEM COUNT DOWN IN "+(targetDateTime-currentDateTime)/1000+" SECONDS..");
					if((targetDateTime-currentDateTime)/1000<60){ //less than 1 min
						Thread.sleep(SingletonShare.SLEEP_COUNTDOWN_SMALL);
					}else{
						Thread.sleep(SingletonShare.SLEEP_COUNTDOWN_BIG);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
