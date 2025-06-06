package top.codelong.apigatewaycenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.codelong.apigatewaycenter.dao.entity.GatewayServerDetailDO;
import top.codelong.apigatewaycenter.dao.mapper.GatewayServerDetailMapper;
import top.codelong.apigatewaycenter.dao.mapper.GatewayServerMapper;
import top.codelong.apigatewaycenter.dto.req.HeartBeatReqVO;
import top.codelong.apigatewaycenter.dto.req.ServerDetailRegisterReqVO;
import top.codelong.apigatewaycenter.enums.StatusEnum;
import top.codelong.apigatewaycenter.service.GatewayServerDetailService;
import top.codelong.apigatewaycenter.utils.UniqueIdUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @description 针对表【gateway_server_detail(系统详细信息表)】的数据库操作Service实现
 * @createDate 2025-05-23 16:05:44
 */
@Service
@RequiredArgsConstructor
public class GatewayServerDetailServiceImpl extends ServiceImpl<GatewayServerDetailMapper, GatewayServerDetailDO> implements GatewayServerDetailService {
    private final GatewayServerDetailMapper gatewayServerDetailMapper;
    private final UniqueIdUtil uniqueIdUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GatewayServerMapper gatewayServerMapper;

    @Override
    public Boolean register(ServerDetailRegisterReqVO reqVO) {
        Integer count = gatewayServerDetailMapper.registerIfAbsent(reqVO.getServerAddress());
        if (count > 0) {
            return true;
        }
        GatewayServerDetailDO detailDO = new GatewayServerDetailDO();
        detailDO.setId(uniqueIdUtil.nextId());
        detailDO.setServerId(reqVO.getServerId());
        detailDO.setServerAddress(reqVO.getServerAddress());
        detailDO.setStatus(StatusEnum.ENABLE.getValue());
        try {
            gatewayServerDetailMapper.insert(detailDO);
        } catch (Exception e) {
            throw new RuntimeException("注册创建失败");
        }
        return true;
    }

    @Override
    public Boolean offline(Long id) {
        GatewayServerDetailDO detailDO = gatewayServerDetailMapper.selectById(id);
        if (detailDO == null) {
            throw new RuntimeException("服务不存在");
        }
        detailDO.setStatus(StatusEnum.DISABLE.getValue());
        try {
            gatewayServerDetailMapper.updateById(detailDO);
        } catch (Exception e) {
            throw new RuntimeException("下线失败");
        }
        return true;
    }

    @Override
    public Boolean keepAlive(HeartBeatReqVO reqVO) {
        String safeKey = reqVO.getSafeKey();
        String server = gatewayServerMapper.getServerNameBySafeKey(safeKey);
        Map<Object, Object> entries = redisTemplate.opsForHash()
                .entries("heartbeat:server:" + server + ":" + reqVO.getAddr());
        if (entries.isEmpty()) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("lastTime", LocalDateTime.now().toString());
            map.put("startTime", LocalDateTime.now().toString());
            map.put("url", reqVO.getAddr());
            map.put("weight", 1);
            redisTemplate.opsForHash().putAll("heartbeat:server:" + server + ":" + reqVO.getAddr(), map);
            redisTemplate.expire("heartbeat:server:" + server + ":" + reqVO.getAddr(), 30, TimeUnit.SECONDS);
            return true;
        }
        redisTemplate.opsForHash().put("heartbeat:server:" + server + ":" + reqVO.getAddr(), "lastTime", LocalDateTime.now().toString());
        redisTemplate.expire("heartbeat:server:" + server + ":" + reqVO.getAddr(), 30, TimeUnit.SECONDS);
        return true;
    }
}




