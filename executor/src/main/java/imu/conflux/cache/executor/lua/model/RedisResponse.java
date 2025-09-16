package imu.conflux.cache.executor.lua.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
public class RedisResponse {

    private final long commandId;
    private final Object data;
    private final Throwable error;

    public RedisResponse(long commandId, Object data) {
        this.commandId = commandId;
        this.data = data;
        this.error = null;
    }

    public RedisResponse(long commandId, Throwable error) {
        this.commandId = commandId;
        this.data = null;
        this.error = error;
    }

    public RedisResponse(long commandId, Object data, Throwable error) {
        this.commandId = commandId;
        this.data = data;
        this.error = error;
    }
}
