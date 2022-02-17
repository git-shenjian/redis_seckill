package cn.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @description:使用redis实现分布式锁
 * @author: sj
 * @createDate: 2021/11/3 12:59
 */
@RestController
public class RedisLock {

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("testLock")
    public void testLock(){
        System.out.println("11111111111111111111");
        String uuid = UUID.randomUUID().toString();
        String locKey = "lock"; // 锁住的是每个商品的数据
        //设置key-value并设置过期时间
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(locKey, uuid, 3, TimeUnit.SECONDS);

        if(lock){//设置锁

            Object value = redisTemplate.opsForValue().get("num");
            //2.1判断num为空return
            if(StringUtils.isEmpty(value)){
                return;
            }
            int num = Integer.parseInt(value+"");
            redisTemplate.opsForValue().increment("num",++num);//num每次+1


            // 定义lua 脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            // 使用redis执行lua执行
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            // 设置一下返回值类型 为Long
            // 因为删除判断的时候，返回的0,给其封装为数据类型。如果不封装那么默认返回String 类型，
            // 那么返回字符串与0 会有发生错误。
            redisScript.setResultType(Long.class);
            // 第一个要是script 脚本 ，第二个需要判断的key，第三个就是key所对应的值。
            redisTemplate.execute(redisScript, Arrays.asList(locKey), uuid);


            /*
            这种方法没有办法保证原子性，可以通过上述lua脚本保证原子性
            String lockUuid = (String) redisTemplate.opsForValue().get("lock");
            if(uuid.equals(lockUuid)){
                //释放锁
                redisTemplate.delete("lock");
            }*/

        }else{
            try {
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

}
