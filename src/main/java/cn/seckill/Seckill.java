package cn.seckill;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
public class Seckill {
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("seckill")
    public Boolean redis_seckill(){
        UUID uuid = UUID.randomUUID();
        boolean seckill = seckill("01", uuid.toString());
        return seckill;
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

        //库存-1
        redisTemplate.opsForValue().decrement(kcKey);
        //把抢到的用户id加入redis
        SetOperations setOperations = redisTemplate.opsForSet();
        setOperations.add(userKey,userid);
        System.out.println("秒杀成功");
        return true;
    }
}
