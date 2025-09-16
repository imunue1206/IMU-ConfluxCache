package imu.conflux.cache.executor.lua.connectionSupport;

import imu.conflux.cache.executor.lua.model.RedisResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RESPDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 确保有足够的数据读取前缀
        if (in.readableBytes() < 1) {
            return;
        }

        // 标记当前读取位置
        in.markReaderIndex();

        // 读取第一个字节判断数据类型
        byte prefix = in.readByte();

        try {
            switch (prefix) {
                case '+': // 简单字符串
                    out.add(readSimpleString(in));
                    break;
                case '-': // 错误
                    out.add(new RespError(readSimpleString(in)));
                    break;
                case ':': // 整数
                    out.add(readInteger(in));
                    break;
                case '$': // 批量字符串
                    out.add(readBulkString(in));
                    break;
                case '*': // 数组
                    out.add(readArray(in));
                    break;
                default:
                    throw new IllegalStateException("Unknown RESP prefix: " + (char) prefix);
            }
        } catch (NotEnoughDataException e) {
            // 数据不足，重置读取位置并等待更多数据
            in.resetReaderIndex();
        }
    }

    private String readSimpleString(ByteBuf in) throws NotEnoughDataException {
        String line = readLine(in);
        if (line == null) {
            throw new NotEnoughDataException();
        }

        return line;
    }


    private Long readInteger(ByteBuf in) throws NotEnoughDataException {
        String line = readLine(in);
        if (line == null) {
            throw new NotEnoughDataException();
        }
        return Long.parseLong(line);
    }

    private Object readBulkString(ByteBuf in) throws NotEnoughDataException {
        // 读取长度
        String lengthStr = readLine(in);
        if (lengthStr == null) {
            throw new NotEnoughDataException();
        }

        int length = Integer.parseInt(lengthStr);

        // 处理 NULL 值
        if (length == -1) {
            return null;
        }

        // 检查是否有足够的数据
        if (in.readableBytes() < length + 2) { // +2 是为了 \r\n
            throw new NotEnoughDataException();
        }

        // 读取内容
        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        // 跳过 \r\n
        in.skipBytes(2);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private List<Object> readArray(ByteBuf in) throws NotEnoughDataException {
        // 读取数组长度
        String lengthStr = readLine(in);
        if (lengthStr == null) {
            throw new NotEnoughDataException();
        }

        int length = Integer.parseInt(lengthStr);

        // 处理 NULL 数组
        if (length == -1) {
            return null;
        }

        List<Object> array = new ArrayList<>(length);

        // 递归读取数组元素
        for (int i = 0; i < length; i++) {
            // 标记当前位置
            in.markReaderIndex();

            // 读取前缀
            if (in.readableBytes() < 1) {
                throw new NotEnoughDataException();
            }

            byte prefix = in.readByte();
            Object element;

            switch (prefix) {
                case '+':
                    element = readSimpleString(in);
                    break;
                case '-':
                    element = new RespError(readSimpleString(in));
                    break;
                case ':':
                    element = readInteger(in);
                    break;
                case '$':
                    element = readBulkString(in);
                    break;
                case '*':
                    element = readArray(in);
                    break;
                default:
                    throw new IllegalStateException("Unknown RESP prefix in array: " + (char) prefix);
            }

            array.add(element);
        }

        return array;
    }

    private String readLine(ByteBuf in) {
        // 查找 \r\n 的位置
        int endIndex = findLineEndIndex(in);
        if (endIndex == -1) {
            return null;
        }

        // 计算长度
        int length = endIndex - in.readerIndex();

        // 读取内容
        String line = in.readCharSequence(length, StandardCharsets.UTF_8).toString();

        // 跳过 \r\n
        in.skipBytes(2);

        return line;
    }

    private int findLineEndIndex(ByteBuf in) {
        int fromIndex = in.readerIndex();
        int toIndex = in.writerIndex();

        for (int i = fromIndex; i < toIndex - 1; i++) {
            if (in.getByte(i) == '\r' && in.getByte(i + 1) == '\n') {
                return i;
            }
        }

        return -1;
    }

    private static class NotEnoughDataException extends Exception { }

    public static class RespError {
        private final String error;

        public RespError(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            return "RespError{" + "error='" + error + '\'' + '}';
        }
    }
}
