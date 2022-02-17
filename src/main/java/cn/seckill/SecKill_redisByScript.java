package cn.seckill;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import cn.util.JedisPoolUtil;
import io.lettuce.core.internal.HostAndPort;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


public class SecKill_redisByScript {
	
	private static final  org.slf4j.Logger logger =LoggerFactory.getLogger(SecKill_redisByScript.class) ;

	public static void main(String[] args) {
		JedisPool jedispool =  JedisPoolUtil.getJedisPoolInstance();
 
		Jedis jedis=jedispool.getResource();
		System.out.println(jedis.ping());
		
		Set<HostAndPort> set=new HashSet<HostAndPort>();

	//	doSecKill("201","sk:0101");
	}
	
	static String secKillScript ="local userid=KEYS[1];\n" +
			"local prodid=KEYS[2];\n" +
			"local qtkey=prodid..\"kc\";\n" +
			"local usersKey=\"user\"..prodid;\n" +
			"local userExists=redis.call(\"sismember\",usersKey,userid);\n" +
			"if tonumber(userExists)==1 then\n" +
			"    return 2;\n" +
			"end\n" +
			"local num= redis.call(\"get\" ,qtkey);\n" +
			"if tonumber(num)<=0 then\n" +
			"    return 0;\n" +
			"else\n" +
			"    redis.call(\"decr\",qtkey);\n" +
			"    redis.call(\"sadd\",usersKey,userid);\n" +
			"end\n" +
			"return 1;\n";
			 
	static String secKillScript2 = 
			"local userExists=redis.call(\"sismember\",\"{sk}:0101:usr\",userid);\r\n" +
			" return 1";

	public static boolean doSecKill(String uid,String prodid) throws IOException {

		JedisPool jedispool =  JedisPoolUtil.getJedisPoolInstance();
		Jedis jedis=jedispool.getResource();
		String sha1=  jedis.scriptLoad(secKillScript);
		Object result= jedis.evalsha(sha1, 2, uid,prodid);
		String reString=String.valueOf(result);
		if ("0".equals( reString )  ) {
			System.err.println("已抢空！！");
		}else if("1".equals( reString )  )  {
			System.out.println("抢购成功！！！！");
		}else if("2".equals( reString )  )  {
			System.err.println("该用户已抢过！！");
		}else{
			System.err.println("抢购异常！！");
		}
		jedis.close();
		return true;
	}
}
