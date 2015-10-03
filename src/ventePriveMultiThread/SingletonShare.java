package ventePriveMultiThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingletonShare {
	private Queue<MapperPidPfid> boughtItems;
	private Map<String, String> loginCookies;

	public static final int SLEEP_BUY = 5; //buy gap..
	public static final int SLEEP_STUCKER = 1000*60;// 1 Min
	public static final int SLEEP_ERROR = 1000*30;//30s
	public static final String urlIncreseItem = "https://secure.fr.vente-privee.com/cart/Widgets/Cart/IncreaseProductQuantity";
	public static final String urlDecreaseItem = "https://secure.fr.vente-privee.com/cart/Widgets/Cart/DecreaseProductQuantity";
	public static final int THREADPOOL_FOR_CATEGORY = 8;
	public static final int THREADPOOL_FOR_ARTICLE = 50;
	public static final int THREADPOOL_FOR_MARK = 3; //by default, 4 threads
	public static final int SLEEP_COUNTDOWN_SMALL = 5000;//5 seds
	public static final int SLEEP_COUNTDOWN_BIG = 1000*60*5;//each 5 mins
	
	
	public static final String targetDateStr = "2015-09-22 07:00:30";
	public static final String email ="chenyanggao123@gmail.com";
	public static final String password ="Pol00ql5!123";
	
	private Logger logger;

	
	public void loginPhase() throws IOException{
		logger = LoggerFactory.getLogger(SingletonShare.class);
		
		/**  Login phase **/
		logger.info("SYSTEM LOGGING..");
		Response response = Jsoup.connect("https://secure.fr.vente-privee.com/authentication/Portal/FR")
				.data("Email", SingletonShare.email)
				.data("Password", SingletonShare.password)
				.method(Method.POST).execute();
		SingletonShare.getInstance().setLoginCookies(response.cookies()); 
		logger.info("SYSTEM LOGGED IN STATUS: "+response.statusCode());
	}

	private SingletonShare(){
		boughtItems =  new ConcurrentLinkedQueue<MapperPidPfid>();
		setLoginCookies(new ConcurrentHashMap <String, String>());
	}

	private static class Loader {
		static SingletonShare INSTANCE = new SingletonShare();
	}

	public static SingletonShare getInstance(){
		return Loader.INSTANCE;
	}

	public Queue<MapperPidPfid> getBoughtItems() {
		return boughtItems;
	}

	public void setBoughtItems(Queue<MapperPidPfid> boughtItems) {
		this.boughtItems = boughtItems;
	}

	public void addOneItemToBoughtItems(MapperPidPfid mapper){
		boughtItems.add(mapper);
	}

	public Map<String, String> getLoginCookies() {
		return loginCookies;
	}

	public void setLoginCookies(Map<String, String> loginCookies) {
		this.loginCookies = loginCookies;
	}
}
