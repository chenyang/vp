package ventePriveMultiThread;

import java.io.IOException;
import java.util.Map.Entry;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CallBuyItemThread implements Runnable{

	private MapperPidPfid mapper;
	private String markCode;
	private String pid;
	private String pfid;

	public CallBuyItemThread(String markCode, String pid, String pfid, MapperPidPfid mapper){
		this.markCode = markCode;
		this.pid = pid;
		this.pfid = pfid;
		this.mapper = mapper;
	}

	@Override
	public void run() {
		try {
			//mock
			//SingletonShare.getInstance().addOneItemToBoughtItems(mapper);			
			//real buy
			callWsToBuyItem(markCode, mapper, pid, pfid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void callWsToBuyItem(String markCode, MapperPidPfid mapper, String pid, String pfid){
		//REAL invoke ws call
		if(!pid.equals(pfid)){ //just when PID!=PFID
			try{
				StringBuffer strOut = new StringBuffer();
				strOut.append("  Analyzed Mapping: PID="+pid+", PFID="+pfid+", MARK code="+markCode+"; ");
				//strOut.append("  Waiting for "+(SingletonShare.SLEEP_BUY)+" mills to call VP ws..;  ");
				//Thread.sleep(SingletonShare.SLEEP_BUY);
				Connection connection = Jsoup.connect("http://fr.vente-privee.com/cart/CartServices/AddToCartOrCanBeReopened");
				for (Entry<String, String> cookie : SingletonShare.getInstance().getLoginCookies().entrySet()) {
					connection.cookie(cookie.getKey(), cookie.getValue());
				}
				Document doc = 
						connection
						.data("pid", pid)
						.data("pfid", pfid)
						.data("q", "1")
						.ignoreContentType(true)
						.post();
				
				if(doc!=null && !doc.body().text().isEmpty()){
					JsonParser parser = new JsonParser();
					JsonObject o = (JsonObject)parser.parse(doc.body().text());
					String desc = o.get("Description").toString();

					if(desc.toLowerCase().contains("success")){//if succeed to buy item
						//add one bought item
						SingletonShare.getInstance().getBoughtItems().add(mapper);
						strOut.append("added to bought items in panier:[PID="+pid+",PFID="+pfid+"]; ");
					}else{
						strOut.append("Sold out or system error;  ");
					}
					strOut.append("SERVER RESPONSE:"+doc.body().text()+";");
				}else{
					strOut.append("NO SERVER RESPONSE..");
				}
				System.out.println(strOut);
			}catch(IOException e){
				System.out.println("  IOEXCEPTION call WS [PID="+pid+", PFID="+pfid+"]: "+ e.getCause().toString());
				//callWsToBuyItem(MapperPidPfid mapper, String pid, String pfid);
			}
		}
	}

}