package ventePriveSingleThread;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class VentePriveSingleThread {
	//46348, andre
	//46865, DC
	//47466, test
	private static String[] markCodes = {"46348", "46865", "46582"};
	private static String targetDateStr = "2015-09-22 07:00:30";
	private static String email ="chenyanggao123@gmail.com";
	private static String password ="Pol00ql5!123";

	private static final int SLEEP_STUCKER = 1000*60*1;// 1 min
	private static final int SLEEP_BUY = 5;//buy gap..
	private static final int SLEEP_COUNTDOWN_SMALL = 5000;//5 seds
	private static final int SLEEP_COUNTDOWN_BIG = 1000*60*5;//each 5 mins
	private static final int SLEEP_ERROR = 1000*30;//30s
	private static String urlIncreseItem = "https://secure.fr.vente-privee.com/cart/Widgets/Cart/IncreaseProductQuantity";
	private static String urlDecreaseItem = "https://secure.fr.vente-privee.com/cart/Widgets/Cart/DecreaseProductQuantity";
	private static List<MapperPidPfid> listBoughtItems;
	private static Map<String, String> loginCookies;

	public static void main(String[] args) {
		listBoughtItems = new ArrayList<MapperPidPfid>();
		System.out.println("  SYSTEM CONFIGED: "+"["+email+"]"+", markCodes:"+Arrays.toString(markCodes)+", targetDate:"+targetDateStr);
		startCountdown(targetDateStr);

		try {
			//do business
			startBusiness();
			System.out.println("SYSTEM ENDS:"+new Date());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loginPhase() throws IOException{
		/**  Login phase **/
		System.out.println("  SYSTEM LOGGING..");
		Response response = Jsoup.connect("https://secure.fr.vente-privee.com/authentication/Portal/FR")
				.data("Email", email)
				.data("Password", password)
				.method(Method.POST).execute();
		loginCookies = response.cookies();
		System.out.println("  SYSTEM LOGGED IN STATUS: "+response.statusCode());
	}

	private static void doTypeOneAnalyse(String visitedUrl, String markCode, String productTypeCatgory, Map<String, String> loginCookies) throws IOException{
		Document typeOneMainPage = Jsoup.connect(visitedUrl).get();
		//Mark's category page
		Elements lists = typeOneMainPage.select(".menuEV_Container >li");
		for(Element el :lists){
			String href4Article = el.select("a").attr("href");
			//System.out.println("  "+href4Article);
			Document typeOneArticlePage = Jsoup.connect("http://fr.vente-privee.com"+href4Article).get();

			//Category's element page
			Elements artList = typeOneArticlePage.select(".artList >li");
			for(Element artEl:artList){
				String href4Express = artEl.select(".infoExpressBt").attr("hrefExpress");
				String[] temps = href4Express.split("/");
				String pfid = temps[temps.length-1]; // get product family id
				//System.out.println("  "+href4Express+", "+pfid);

				//element express page
				Document typeOneExpressPage = Jsoup.connect("http://fr.vente-privee.com"+href4Express).get();
				List<MapperPidPfid> listMapper = new ArrayList<MapperPidPfid>();
				Elements models = typeOneExpressPage.select("#model >option");
				if(!models.isEmpty()){ //if multi-model product:
					for(Element model:models){
						String pid = model.attr("value");
						MapperPidPfid mapper = new MapperPidPfid();
						mapper.setPid(pid);
						mapper.setPfid(pfid);
						listMapper.add(mapper);
					}
				}else{//if no models to choose.. if single product..
					pfid = typeOneExpressPage.select("#singleProduct").attr("productfamilyid");
					String pid= typeOneExpressPage.select("#singleProduct").attr("productid");
					MapperPidPfid mapper = new MapperPidPfid();
					mapper.setPid(pid);
					mapper.setPfid(pfid);
					listMapper.add(mapper);
				}

				for(MapperPidPfid mapper: listMapper){
					mapper.setMarkCode(markCode);
					String pid = mapper.getPid();
					pfid = mapper.getPfid();
					if(!pid.equals(pfid)){ //just when PID!=PFID
						try{
							System.out.print("  Analyzed Mapping: PID="+pid+", PFID="+pfid+", MARK code="+markCode+"; ");
							System.out.print("  Waiting for "+(SLEEP_BUY/1000)+" seds to call VP ws..;  ");
							Thread.sleep(SLEEP_BUY);
							Connection connection = Jsoup.connect("http://fr.vente-privee.com/cart/CartServices/AddToCartOrCanBeReopened");
							for (Entry<String, String> cookie : loginCookies.entrySet()) {
								connection.cookie(cookie.getKey(), cookie.getValue());
							}
							Document doc = 
									connection
									.data("pid", pid)
									.data("pfid", pfid)
									.data("q", "1")
									.ignoreContentType(true)
									.post();
							if(!doc.body().text().isEmpty()){
								JsonParser parser = new JsonParser();
								JsonObject o = (JsonObject)parser.parse(doc.body().text());
								String desc = o.get("Description").toString();

								if(desc.toLowerCase().contains("success")){//if succeed to buy item
									//add one bought item
									listBoughtItems.add(mapper);
									System.out.print("added to bought items in panier:[PID="+pid+",PFID="+pfid+"]; ");
								}else{
									System.out.print("Sold out or system error;  ");
								}
								System.out.print("SERVER RESPONSE:"+doc.body().text()+";");
							}else{
								System.out.print("NO SERVER RESPONSE..");
							}
							System.out.println();
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				}
			}
		}
	}


	//2nd page style analysis
	private static void doTypeTwoAnalyse(String visitedUrl, String markCode, String productTypeCatgory, Map<String, String> loginCookies) throws IOException{

	}

	private static void startBusiness() throws IOException, InterruptedException{

		/**login phase**/
		loginPhase();

		/**  Analysis page and buy items*/
		for(String markCode :markCodes){
			doPageAnalysis(markCode, "Universe", loginCookies);
		}

		/** stuck timer*/
		
		/*while(true){
			stuckTimer();
		}*/
	}

	private static void doPageAnalysis(String markCode, String productTypeCatgory, Map<String, String> loginCookies) throws IOException{
		String visitedUrl = "http://fr.vente-privee.com/catalog/"+productTypeCatgory+"/Operation/"+markCode+"/site/1";
		System.out.println("--Analysising url...");
		System.out.println("  "+visitedUrl);
		doTypeOneAnalyse(visitedUrl, markCode, productTypeCatgory, loginCookies);
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
						Thread.sleep(SLEEP_COUNTDOWN_SMALL);
					}else{
						Thread.sleep(SLEEP_COUNTDOWN_BIG);
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Mechanism to maker the timer suck..
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws Exception 
	 */	
	private static void stuckTimer() throws InterruptedException, IOException{
		System.out.println("--Staring stuck timer..");
		Connection connection;
		/** login again **/
		loginPhase();

		if(!listBoughtItems.isEmpty()){
			MapperPidPfid mapper = listBoughtItems.get(0);
			//increase 1 item
			System.out.println("  Waiting for "+(SLEEP_STUCKER/1000)+" seds to increase item to keep alive session..");
			Thread.sleep(SLEEP_STUCKER);
			System.out.println("  Increasing item: pid:"+mapper.getPid()+", pfid:"+mapper.getPfid()+", markcode:"+mapper.getMarkCode());
			connection = Jsoup.connect(urlIncreseItem);
			for (Entry<String, String> cookie : loginCookies.entrySet()) {
				connection.cookie(cookie.getKey(), cookie.getValue());
			} 
			connection
			.data("productId", mapper.getPid())
			.data("operationId", mapper.getMarkCode()) //mark id
			.data("productFamilyId", mapper.getPfid())
			.ignoreContentType(true)
			.post();

			//then decrease 1 item in order not to bother other clients..
			System.out.println("  Waiting for "+(SLEEP_STUCKER/1000)+" seds to decrease item to keep session alive..");
			Thread.sleep(SLEEP_STUCKER);
			System.out.println("  Decreasing item: pid:"+mapper.getPid()+", pfid:"+mapper.getPfid()+", markcode:"+mapper.getMarkCode());
			connection = Jsoup.connect(urlDecreaseItem);
			for (Entry<String, String> cookie : loginCookies.entrySet()) {
				connection.cookie(cookie.getKey(), cookie.getValue());
			}
			connection
			.data("productId", mapper.getPid())
			.data("operationId", mapper.getMarkCode()) //mark id
			.data("productFamilyId", mapper.getPfid())
			.ignoreContentType(true)
			.post();
		}else{
			System.out.println("--BUSINESS ERROR NO ITEMS CAN BE FOUND, will wait for "+(SLEEP_ERROR/1000)+" seds and restart..");
			Thread.sleep(SLEEP_ERROR);
			//restartBusiness until success
			startBusiness();
		}	
	}

}
