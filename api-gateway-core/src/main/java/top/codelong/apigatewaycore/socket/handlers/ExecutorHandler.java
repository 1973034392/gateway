package top.codelong.apigatewaycore.socket.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.codelong.apigatewaycore.common.GatewayServer;
import top.codelong.apigatewaycore.common.HttpStatement;
import top.codelong.apigatewaycore.common.result.Result;
import top.codelong.apigatewaycore.config.GlobalConfiguration;
import top.codelong.apigatewaycore.connection.BaseConnection;
import top.codelong.apigatewaycore.connection.DubboConnection;
import top.codelong.apigatewaycore.connection.HTTPConnection;
import top.codelong.apigatewaycore.socket.BaseHandler;
import top.codelong.apigatewaycore.utils.RequestParameterUtil;
import top.codelong.apigatewaycore.utils.RequestResultUtil;

import java.util.Map;

/**
 * 执行器处理器
 * 负责根据请求类型选择HTTP或Dubbo连接并执行请求
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ExecutorHandler extends BaseHandler<FullHttpRequest> {
    @Resource
    private GlobalConfiguration config;
    @Resource
    private GatewayServer gatewayServer;

    /**
     * 处理请求执行
     *
     * @param ctx     ChannelHandler上下文
     * @param channel 当前Channel
     * @param request HTTP请求
     */
    @Override
    protected void handle(ChannelHandlerContext ctx, Channel channel, FullHttpRequest request) {
        // 从Channel属性获取HttpStatement
        HttpStatement httpStatement = (HttpStatement) channel.attr(AttributeKey.valueOf("HttpStatement")).get();
        log.debug("开始处理请求执行，URI: {}, 类型: {}", request.uri(),
                httpStatement.getIsHttp() ? "HTTP" : "Dubbo");

        // 获取请求参数
        Map<String, Object> parameters;
        try {
            parameters = RequestParameterUtil.getParameters(request);
        } catch (Exception e) {
            DefaultFullHttpResponse response = RequestResultUtil.parse(Result.error(e.getMessage()));
            channel.writeAndFlush(response);
            return;
        }
        log.trace("请求参数: {}", parameters);

        BaseConnection connection;
        String url = RequestParameterUtil.getUrl(request);

        try {
            // 获取服务地址
            String serverAddr = gatewayServer.getOne();
            log.debug("获取到服务地址: {}", serverAddr);

            // 根据请求类型创建连接
            if (httpStatement.getIsHttp()) {
                url = "http://" + serverAddr + url;
                log.debug("创建HTTP连接，完整URL: {}", url);
                connection = new HTTPConnection(config.getHttpClient());
            } else {
                url = serverAddr.split(":")[0] + ":20880";
                log.debug("创建Dubbo连接，服务地址: {}", url);
                connection = new DubboConnection(config.getDubboServiceMap());
            }

            // 执行请求
            Result data = connection.send(parameters, url, httpStatement);
            log.debug("请求执行成功，结果状态码: {}", data.getCode());

            // 将结果存入Channel属性
            channel.attr(AttributeKey.valueOf("data")).set(data);
        } catch (Exception e) {
            log.error("服务调用失败，URI: {}, 错误: {}", request.uri(), e.getMessage(), e);
            DefaultFullHttpResponse response = RequestResultUtil.parse(Result.error("服务调用失败"));
            channel.writeAndFlush(response);
            return;
        }

        // 传递给下一个处理器
        ctx.fireChannelRead(request);
    }
}