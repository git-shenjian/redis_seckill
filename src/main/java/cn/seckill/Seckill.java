package cn.seckill;

import cn.util.JedisPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

@RestController
public class Seckill {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

    @GetMapping("seckill")
    public Boolean redis_seckill(){
        String userid = new Random().nextInt(50000) + "";
        boolean seckill = seckill("01", userid);//存在库存遗留问题
        //boolean seckill = seckillByLua("01", userid);//使用lua脚本解决库存遗留问题
        return seckill;
    }

    public boolean seckillByLua(String prodid,String userid){
        //方法一：使用redisTemplate
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(secKillScript);
        // 设置一下返回值类型 为Long
        // 因为删除判断的时候，返回的0,给其封装为数据类型。如果不封装那么默认返回String 类型，
        // 那么返回字符串与0 会有发生错误。
        redisScript.setResultType(Long.class);
        // 第一个要是script 脚本 ，第二个需要判断的key，第三个就是key所对应的值。
        String reString = (String) redisTemplate.execute(redisScript, Arrays.asList(userid, prodid));
        if ("0".equals( reString )  ) {
            System.err.println("已抢空！！");
        }else if("1".equals( reString )  )  {
            System.out.println("抢购成功！！！！");
        }else if("2".equals( reString )  )  {
            System.err.println("该用户已抢过！！");
        }else{
            System.err.println("抢购异常！！");
        }
        return true;

        //方法二、使用jedis
        /*JedisPool jedispool =  JedisPoolUtil.getJedisPoolInstance();
        Jedis jedis=jedispool.getResource();
        String sha1=  jedis.scriptLoad(secKillScript);
        Object result= jedis.evalsha(sha1, 2, userid,prodid);
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
        return true;*/
    }

    public boolean seckill(String prodid,String userid){
        //如果商品id和用户id为空，直接返回
        if(prodid==null||userid==null){
            return false;
        }

        //拼接商品库存key
        String kcKey = prodid+"kc";
        //拼接已经抢到商品用户key
        String userKey = "user"+prodid;

        //开启事务
        redisTemplate.setEnableTransactionSupport(true);
        //监控库存key,watch（）监视key，在事务执行过程中，如果key对应的值被改变，则事务被打断
        redisTemplate.watch(kcKey);

        //判断库存是否为null
        String kc = redisTemplate.opsForValue().get(kcKey).toString();
        if(kc==null){
            System.out.println("秒杀活动还未开始");
            return false;
        }
        //判断已经抢到的用户中是否包含当前用户
        Set users = redisTemplate.opsForSet().members(userKey);
        for(Object user:users){
            if(userid==user.toString()){
                System.out.println("您已秒杀过，不能在秒杀了");
                return false;
            }
        }
        //判断是否还有库存
        if(Integer.parseInt(kc)<=0){
            System.out.println("秒杀已经结束了");
            return false;
        }

        //使用事务
        redisTemplate.multi();
        //库存-1
        redisTemplate.opsForValue().decrement(kcKey);
        //吧抢到的用户id加入redis
        redisTemplate.opsForSet().add(userKey,userid);

        //执行
        List<Object> result = redisTemplate.exec();
        if(result.size()==0||result==null){
            System.out.println("秒杀失败。。。。");
            return false;
        }
        System.out.println("秒杀成功。。。。");
        return true;
    }

}
