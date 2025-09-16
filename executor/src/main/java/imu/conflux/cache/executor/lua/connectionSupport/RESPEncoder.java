package imu.conflux.cache.executor.lua.connectionSupport;

import imu.conflux.cache.executor.lua.model.RedisLuaCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;


public class RESPEncoder extends MessageToByteEncoder<RedisLuaCommand> {


    @Override
    protected void encode(final ChannelHandlerContext ctx, final RedisLuaCommand luaCommand, final ByteBuf byteBuf) {

        validate(luaCommand);

        int elementCount = 3 + luaCommand.getTotalElement();

        // RESP协议 数组前缀   *N\r\n  其中N特指元素个数
        byteBuf.writeByte('*');
        byteBuf.writeBytes(String.valueOf(elementCount).getBytes());
        byteBuf.writeBytes("\r\n".getBytes());

        writeBulkString(byteBuf, "EVAL");
        writeBulkString(byteBuf, luaCommand.getScript());
        writeBulkString(byteBuf, String.valueOf(luaCommand.getKeys().size()));
        for (String key : luaCommand.getKeys()) writeBulkString(byteBuf, key);
        for (String arg : luaCommand.getValues()) writeBulkString(byteBuf, arg);
    }

    private void writeBulkString(ByteBuf byteBuf, String s) {
        if (s == null) {
            // 处理 NULL 值: $-1\r\n
            byteBuf.writeByte('$');
            byteBuf.writeBytes("-1".getBytes());
            byteBuf.writeBytes("\r\n".getBytes());
            return;
        }

        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byteBuf.writeByte('$');
        byteBuf.writeBytes(String.valueOf(bytes.length).getBytes());
        byteBuf.writeBytes("\r\n".getBytes());
        byteBuf.writeBytes(bytes);
        byteBuf.writeBytes("\r\n".getBytes());
    }

    private void validate(final RedisLuaCommand luaCommand) {
        if (luaCommand == null) throw new NullPointerException("RedisLuaCommand对象为空");

        final String luaScript = luaCommand.getScript();
        if (luaScript == null || luaScript.isBlank()) throw new IllegalArgumentException("RedisLuaCommand的Lua脚本为空");
    }
}
