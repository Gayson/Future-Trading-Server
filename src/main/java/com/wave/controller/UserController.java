package com.wave.controller;

import com.wave.cache.TradeCache;
import com.wave.model.ContractItem;
import com.wave.model.TradeRecord;
import com.wave.model.User;
import com.wave.model.UserContract;
import com.wave.repository.EntityRepository.ContractItemRepository;
import com.wave.repository.EntityRepository.TradeRecordRepository;
import com.wave.repository.EntityRepository.UserContractRepository;
import com.wave.repository.EntityRepository.UserRepository;
import com.wave.staticsetting.ReturnStatus;
import com.wave.staticsetting.StaticConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;


@RestController
public class UserController {
    @Autowired
    private UserRepository user_repository;
    

    @Autowired
    private ContractItemRepository contractItem_repository;
    @Autowired
    private HttpServletRequest Request;
    @Autowired
    private TradeRecordRepository tradeRecordRepository;
    @Autowired
    private UserContractRepository userContractRepository;
    @Autowired
    private TradeCache tradeCache;

    @Autowired
    HttpServletRequest request;

    @RequestMapping(value = "/user/login")
    public HashMap<String,Integer> login(@RequestParam(value = "password")String password,
    									@RequestParam(value = "email")String email) {
    	System.out.println(password + email);
    	User user = user_repository.findByEmail(email);
    	if(user == null || BCrypt.checkpw(password, user.getPassword()) == false)
    		return ReturnStatus.getReturn(ReturnStatus.LOGIN_PASSERROR);
    	else
    		return ReturnStatus.getReturn(ReturnStatus.LOGIN_SUCCESS);

//        return ReturnStatus.getReturn(ReturnStatus.LOGIN_PASSERROR);
    }
//    public HashMap<String,Integer> login() {
//        return ReturnStatus.getReturn(ReturnStatus.LOGIN_SUCCESS);
//    }
    
    @RequestMapping(value = "/user/login_fail")
    public HashMap<String, Integer> login_fail(){
        return ReturnStatus.getReturn(ReturnStatus.LOGIN_PASSERROR);
    }

    @RequestMapping(value = "/user/login_success")
    public HashMap<String,Integer> login_success(){
        return ReturnStatus.getReturn(ReturnStatus.LOGIN_SUCCESS);
    }

    @RequestMapping(value = "/user/register")
    public HashMap<String,Integer> register(@RequestParam(value = "username")String username,
                                            @RequestParam(value = "password")String password,
                                            @RequestParam(value = "age",defaultValue = "0")String age_str,
                                            @RequestParam(value = "email")String email){

        password=new BCryptPasswordEncoder().encode(password);
        System.out.println(password.length());
        int age=0;
        String[] split1=email.split("@");
        if(split1.length==2){
            String[] split2=split1[1].split(".");
            if(split2.length==2){
                if(!split2[1].equals("cn")&&!split2[2].equals("com")) return ReturnStatus.getReturn(ReturnStatus.EMAIL_ERROR);
            }else return ReturnStatus.getReturn(ReturnStatus.EMAIL_ERROR);
        }else return ReturnStatus.getReturn(ReturnStatus.EMAIL_ERROR);

        Pattern pattern=Pattern.compile("[0-9]*");
        if(pattern.matcher(age_str).matches()){
            age=Integer.parseInt(age_str);
        }else {
            return ReturnStatus.getReturn(ReturnStatus.INPUT_ERROR);
        }

        User user=new User();
        user.setNickname(username);
        user.setPassword(password);
        user.setAge(age);
        user.setEmail(email);
        user.setPortrait_url(StaticConfig.DEFAULT_PORTRAIT_URL);
        user.setEnabled(true);
        user.setTotalProfile(0);
        user.setDepist((float) 0.01);
        
        user.setAccount_balance(50000);
        
        try{
            user_repository.save(user);
            return ReturnStatus.getReturn(ReturnStatus.REGISTER_SUCCESS);
        }catch (Exception e){
            e.printStackTrace();
            return ReturnStatus.getReturn(ReturnStatus.EMAIL_EXIST);
        }
    }


    
    @RequestMapping(value = "/user/logintest")
    public String logintest(){
        return "login_success";
    }

    @RequestMapping(value = "/user/logout")
    public void logout(){}

    @RequestMapping(value = "/user/logout_success")
    public HashMap<String,Integer> logout_success(){
        return ReturnStatus.getReturn(ReturnStatus.LOGOUT_SUCCESS);
    }

    @RequestMapping(value = "/user/test")
    public HashMap<String, Integer> test() {
        return ReturnStatus.getReturn(ReturnStatus.LOGIN_USER_NONEXIST);
    }
    
    @RequestMapping(value = "/user/sell")
    public HashMap<String,Object> UserSell(@RequestParam(value = "name")String name,
    					  					@RequestParam(value = "price")Float price,
    					  					 @RequestParam(value = "hands")Float hands,
    					  					 @RequestParam(value = "email")String email){
//    					  					 @RequestParam HttpServletRequest request){
    		
    	//判断余额是否足够 更新交易列表 用户余额 持仓列表，返回当前所有持仓信息
    	Date date = new Date();
    		
    	//获取用户信息
    	User user = new User();
//    	String phone_number=request.getRemoteUser();
    	user = user_repository.findByEmail(email);
    		
    	//卖出价格
    	float account_balance = user.getAccount_balance() + hands * price;
    		
    	//获取合约信息
		ContractItem contractItem = contractItem_repository.findByTicker(name);
    		
    	//更新持仓列表
    	UserContract userContract = new UserContract();
    	userContract.setUser(user);
    	userContract.setAmount(hands);
    	userContract.setOpen_offset(0);
    	userContract.setPrice(price);
    	userContract.setDir(0);
    	userContract.setDate(date);
    	userContract.setContract_item(contractItem);
    	userContractRepository.save(userContract);
    		
    	//更新交易列表
    	TradeRecord tradeRecord = new TradeRecord();
    	tradeRecord.setUser(user);
    	tradeRecord.setAmount(hands);
    	tradeRecord.setDate(date);
    	tradeRecord.setOpen_offset(0);	//0表示开仓
    	tradeRecord.setPrice(price);
    	tradeRecord.setTrading_type(0);	//0表示市价
    	tradeRecord.setContract_item(contractItem);
    	tradeRecord.setType(0);	//0表示卖出
    	tradeRecord.sethandsId(userContract.getId());
    	tradeRecordRepository.save(tradeRecord);
    	
    	//更新用户信息
    	user.setAccount_balance(account_balance);
    	user_repository.save(user);
    	//返回消息
    	HashMap<String, Object> result = new HashMap<String, Object>();
    	result.put("name", name);
    	result.put("success", true);
    	result.put("msg", "success");
    	result.put("time", date.toString());
    	result.put("order", tradeRecord.getId());
    	return result;
    	
    }

    @RequestMapping(value = "/user/buy")
    public HashMap<String, Object> UserBuy(@RequestParam(value = "name")String name,
											@RequestParam(value = "price")Float price,
											@RequestParam(value = "hands")Float hands,
											@RequestParam(value = "email")String email){
    	HashMap<String, Object> result = new HashMap<>();
    	result.put("name", name);
    	User user = new User();
    	Date date = new Date();
    	user = user_repository.findByEmail(email);
    	if(user.getAccount_balance() - price * hands < 0) {
    		result.put("msg", "余额不足");
    		result.put("success", true);
        	result.put("time", date.toString());
    		return result;
    	}
    	//买进价格
		float account_balance = user.getAccount_balance() - hands * price;
		
		//获取合约信息
		ContractItem contractItem = contractItem_repository.findByTicker(name);

		//更新持仓列表
		UserContract userContract = new UserContract();
		userContract.setUser(user);
		userContract.setAmount(hands);
		userContract.setOpen_offset(0);
		userContract.setDir(1);
		userContract.setPrice(price);
		userContract.setDate(date);
		userContract.setContract_item(contractItem);
		userContractRepository.save(userContract);
		
		//更新交易列表
    	TradeRecord tradeRecord = new TradeRecord();
    	tradeRecord.setUser(user);
    	tradeRecord.setAmount(hands);
    	tradeRecord.setDate(date);
    	tradeRecord.setOpen_offset(0);	//0表示开仓
    	tradeRecord.setPrice(price);
    	tradeRecord.setTrading_type(0);	//0表示市价
    	tradeRecord.setContract_item(contractItem);
    	tradeRecord.setType(0);	//0表示卖出
    	tradeRecord.sethandsId(userContract.getId());
    	tradeRecordRepository.save(tradeRecord);
		
		//更新用户信息
		user.setAccount_balance(account_balance);
		user_repository.save(user);
		result.put("success", true);
    	result.put("msg", "success");
    	result.put("time", date.toString());
    	result.put("order", tradeRecord.getId());

    	return result;
    }
    
    @RequestMapping(value = "user/handsRefresh")
    public ArrayList<HashMap<String, Object>> HandsRefresh(@RequestParam(value = "email")String email) {
//    											@RequestParam HttpServletRequest request) {
    	User user = user_repository.findByEmail(email);
    	
    	List<UserContract> userContracts = userContractRepository.findByUser(user);
    	
    	ArrayList<HashMap<String, Object>> results = new ArrayList<>();
    	for (UserContract userContract : userContracts) {
    		if(userContract.getOpen_offset() == 1)
    			continue;
    		HashMap<String, Object> result = new HashMap<>();
    		ContractItem contractItem =  userContract.getContract_item();
    		float openPrice = userContract.getPrice();
    		float closePrice = tradeCache.getContract(contractItem.getTicker()).getInfo().getLatest_price();
    		result.put("name", contractItem.getTicker().toUpperCase());
    		result.put("hands", userContract.getAmount());
    		result.put("openPrice", openPrice);
			result.put("closePrice",closePrice);
			if(userContract.getDir() == 0){
				result.put("dir", "卖出");
				result.put("profit", Math.round((openPrice - closePrice)*userContract.getAmount()*100)/100);
			}
			else {
				result.put("dir", "买进");
				result.put("profit", Math.round((closePrice - openPrice)*userContract.getAmount()*100)/100);
			}
			result.put("stopLoss", Math.round(openPrice * 0.1*100*userContract.getAmount())/100);
			result.put("stopWin", Math.round(openPrice * 0.3*100*userContract.getAmount())/100);
			result.put("rebate", Math.round(((openPrice - closePrice) * 10000*userContract.getAmount())*100)/100);
			result.put("interest", 0);
			
			result.put("time", userContract.getDate().toString());
			result.put("order", userContract.getId());
			results.add(result);
		}
    	return results;
    }
    
    @RequestMapping(value = "/user/accountrefresh")
    public HashMap<String, Object> AccountRefresh(@RequestParam(value = "email")String email) {
    	User user = user_repository.findByEmail(email);
    	
    	HashMap<String, Object> result = new HashMap<>();
    	result.put("account", user.getNickname());
    	result.put("balance", user.getAccount_balance());
    	float netWorth = calculateNetWorth(user);
    	float marginLevel = user.getDepist();
    	float occupyMargin = 0;
    	result.put("netWorth", netWorth);
    	if(netWorth < 0)
    		result.put("netWorhColor", "red");
    	else
    		result.put("netWorhColor", "green");
    	float handsMargin = user.getDepist() * user.getAccount_balance();
    	result.put("handsMargin", Math.round((handsMargin)*100)/100);
    	result.put("occupyMargin", Math.round((occupyMargin)*100)/100);
    	result.put("usableBalance", Math.round((handsMargin - occupyMargin)*100)/100);
    	result.put("totalProfile", Math.round(user.getTotalProfile()*100)/100);
    	result.put("marginLevel", Math.round((marginLevel*100)/100));
    	return result;
    }
    
    @RequestMapping(value = "/user/offhands")
    public HashMap<String, Object> OffHands(@RequestParam(value = "order") long order, //持仓单号
    										@RequestParam(value = "price") float price,
    										@RequestParam(value = "email") String email) {
    	HashMap<String, Object> result = new HashMap<>();
    	Date date = new Date();
    	User user = user_repository.findByEmail(email);
    	UserContract userContract = userContractRepository.findById(order);
    	TradeRecord tradeRecord = new TradeRecord();
    	tradeRecord.setPrice(price);
    	tradeRecord.setAmount(userContract.getAmount());
    	tradeRecord.sethandsId(order);
    	tradeRecord.setContract_item(userContract.getContract_item());
    	tradeRecord.setDate(date);
    	tradeRecord.setUser(user);
    	tradeRecord.setOpen_offset(1);
    	userContract.setOpen_offset(1);
    	float account_balance = user.getAccount_balance();
    	ArrayList<TradeRecord> oldTradeRecord = tradeRecordRepository.findByHandsId(order);
    	if(userContract.getDir() == 0) {
    		tradeRecord.setTrading_type(1);
    		if(account_balance - tradeRecord.getPrice() < 0) {
    			result.put("success", false);
    	    	result.put("msg", "余额不足");
    	    	result.put("time", date.toString());
    	    	return result;
    		}	
    		user.setAccount_balance(account_balance - tradeRecord.getPrice()*userContract.getAmount());
    		user.setTotalProfile(user.getTotalProfile() + (oldTradeRecord.get(0).getPrice() - price)*userContract.getAmount());
    	}
    	else {
    		tradeRecord.setTrading_type(0); 
    		user.setAccount_balance(account_balance + tradeRecord.getPrice()*userContract.getAmount());
    		user.setTotalProfile(user.getTotalProfile() + (price - oldTradeRecord.get(0).getPrice())*userContract.getAmount());
    	}
    	user_repository.save(user);
    	tradeRecordRepository.save(tradeRecord);
    	userContractRepository.save(userContract);
    	
    	result.put("success", true);
    	result.put("msg", "success");
    	result.put("time", date.toString());
    	return result;
    }

    
    @RequestMapping(value = "/user/donerefresh")
    public List<HashMap<String, Object>> DoneRefresh(@RequestParam(value = "email") String email) {
    	ArrayList<HashMap<String, Object>> results = new ArrayList<HashMap<String,Object>>();
    	User user = user_repository.findByEmail(email);
    	List<TradeRecord> tradeRecords = tradeRecordRepository.findByUser(user);
    	for(TradeRecord tradeRecord : tradeRecords) {
    		HashMap<String, Object> result = new HashMap<>();
    		result.put("doneID", tradeRecord.getId());
    		result.put("orderID", tradeRecord.gethandsId());
    		result.put("name", tradeRecord.getContract_item().getTicker().toUpperCase());
    		result.put("tradeType", "市价");
    		if(tradeRecord.getOpen_offset() == 0)
    			result.put("opType", "开");
    		else
    			result.put("opType", "平");
    		if(tradeRecord.getType() == 0)
    			result.put("dir", "卖出");
    		else
    			result.put("dir", "买进");
    		result.put("hands", tradeRecord.getAmount());
    		result.put("rebate", 0);
    		result.put("donePrice", tradeRecord.getPrice());
    		result.put("time", tradeRecord.getDate().toString());
    		result.put("note", "");
    		results.add(result);
    	}
    	return results;
    }
    
    @RequestMapping(value = "/user/profilerefresh")
    public List<HashMap<String, Object>> ProfileRefresh(@RequestParam(value = "email") String email) {
    	ArrayList<HashMap<String, Object>> results = new ArrayList<HashMap<String,Object>>();
    	User user = user_repository.findByEmail(email);
    	ArrayList<UserContract> userContracts = userContractRepository.findByUser(user);
    	for(UserContract userContract : userContracts) {
    		if(userContract.getOpen_offset() == 0)
    			continue;
    		HashMap<String, Object> result = new HashMap<>();
    		ArrayList<TradeRecord> tradeRecords = tradeRecordRepository.findByHandsId(userContract.getId());
    		result.put("name", contractItem_repository.findById(userContract.getContract_item().getId()).getTicker().toUpperCase());
    		result.put("hands", userContract.getAmount());
    		float openPrice = tradeRecords.get(0).getPrice();
    		float closePrice = tradeRecords.get(1).getPrice();
    		result.put("openPrice", openPrice);
    		result.put("offPrice", closePrice);
    		result.put("interest", 0);
    		result.put("rebate", Math.round(((openPrice - closePrice) *userContract.getAmount()* 10000)*100)/100);
    		if(userContract.getDir() == 0) {
    			result.put("dir", "卖出");
    			result.put("profile", Math.round((openPrice - closePrice)*userContract.getAmount()*100)/100);
    		}
    		else {
    			result.put("dir", "买进");
    			result.put("profile", Math.round((closePrice - openPrice)*userContract.getAmount()*100)/100);
    		}
    		result.put("doneID", tradeRecords.get(1).getId());
    		result.put("handsID", userContract.getId());
    		result.put("openTime", tradeRecords.get(0).getDate().toString());
    		result.put("doneTime", tradeRecords.get(1).getDate().toString());
    		results.add(result);
    	}
    	return results;
    }

    @RequestMapping(value = "/user/get_user")
    public User getUser(){
        return user_repository.findByPhoneNumber(request.getRemoteUser());
    }
    
    @RequestMapping(value = "/user/user_info")
    public HashMap<String, Object> UserInfo(@RequestParam(value = "email")String email) {
    	User user = user_repository.findByEmail(email);
    	HashMap<String, Object> result = new HashMap<>();
    	result.put("user_id", user.getId());
    	result.put("username", user.getNickname());
    	result.put("profile", user.getTotalProfile());
    	result.put("age", user.getAge());
    	result.put("email", user.getEmail());
    	result.put("gender", user.getSex());
    	System.out.println(user.getEmail());
    	return result;
    }

    
    @RequestMapping(value = "/user/editUserInfo")
    public HashMap<String, Integer> editUserInfo(@RequestParam(value = "email")String email,
    											@RequestParam(value = "username")String username,
    											@RequestParam(value = "age")String age_str,
    											@RequestParam(value = "sex")String sex_str) {
    	try{
            int age=0;
            int sex=0;
            Pattern pattern=Pattern.compile("[0-9]*");
            if(pattern.matcher(age_str).matches()&&pattern.matcher(sex_str).matches()){
                age=Integer.parseInt(age_str);
                sex=Integer.parseInt(sex_str);
            }else {
                return ReturnStatus.getReturn(ReturnStatus.INPUT_ERROR);
            }

    		User user = user_repository.findByEmail(email);
    		user.setNickname(username);
    		user.setAge(age);
    		user.setSex(sex);
    		user_repository.save(user);
    		return ReturnStatus.getReturn(ReturnStatus.REGISTER_SUCCESS);
    	}catch (Exception e) {
			// TODO: handle exception
    		return ReturnStatus.getReturn(ReturnStatus.INPUT_ERROR);
		}
    }
    
    @RequestMapping(value = "/user/trade_record")
    public ArrayList<HashMap<String, Object>> TradeRecord(@RequestParam(value = "email")String email) {
    	ArrayList<HashMap<String, Object>> results = new ArrayList<HashMap<String,Object>>();
    	User user = user_repository.findByEmail(email);
    	ArrayList<TradeRecord> tradeRecords = new ArrayList<>();
    	tradeRecords = tradeRecordRepository.findByUser(user);
    	for (TradeRecord tradeRecord : tradeRecords) {
    		HashMap<String, Object> result = new HashMap<>();
    		result.put("contract_id", tradeRecord.getId());
    		result.put("time", tradeRecord.getDate().toString());
    		result.put("trading_type", "市价");
    		if(tradeRecord.getType() == 1)
    			result.put("dir", "买进");
    		else
    			result.put("dir", "卖出");
    		result.put("amount", tradeRecord.getAmount());
    		result.put("price", tradeRecord.getPrice());
    		results.add(result);
    	}
    	return results;
    }

    @RequestMapping(value = "/user/account_info")
    public HashMap<String, Object> AccountInfo(@RequestParam(value = "email") String email){
    	User user = user_repository.findByEmail(email);
    	HashMap<String, Object> results = new HashMap();
    	results.put("name", user.getNickname());
    	results.put("account_balance", user.getAccount_balance());
    	ArrayList<UserContract> userContracts = new ArrayList<>();
    	userContracts = userContractRepository.findByUser(user);
    	ArrayList<HashMap<String, Object>> contract_info = new ArrayList<>();
    	for(UserContract userContract : userContracts){
    		if(userContract.getOpen_offset() == 1)
    			continue;
    		HashMap<String, Object> result = new HashMap<>();
    		result.put("contract_id", userContract.getId());
    		result.put("name", userContract.getContract_item().getTicker());
    		result.put("amount", userContract.getAmount());
    		result.put("price", userContract.getPrice());
    		if(userContract.getDir() == 0)
    			result.put("open_offset", "卖出");
    		else
    			result.put("open_offset", "买进");
    		contract_info.add(result);
    	}
    	results.put("contract_info", contract_info);
    	return results;
    }

    @RequestMapping(value = "user/applyAllowance")
	public int applyAllowance(@RequestParam(value = "email")String email){
		User user=user_repository.findByEmail(email);
        float netWorth=calculateNetWorth(user);
        if(netWorth<=1000){
            user.setAccount_balance(user.getAccount_balance()+50000);
            return 0;
        }
		return 1;
	}

	private float calculateNetWorth(User user){
        Set<UserContract> userContracts=user.getContracts();

        float netWorth = user.getAccount_balance();
        float marginLevel = user.getDepist();
        float occupyMargin = 0;
        for(UserContract userContract : userContracts) {
            if(userContract.getOpen_offset() == 1)
                continue;
            occupyMargin += marginLevel * userContract.getPrice() * userContract.getAmount();
            if (userContract.getDir() == 0)
                netWorth -= userContract.getPrice()*userContract.getAmount();
            else
                netWorth += userContract.getPrice()*userContract.getAmount();
        }
        return netWorth;
    }
}


