package imu.conflux.cache.executor.lua.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
public class RedisLuaCommand {

    private final String script;
    private final List<String> keys;
    private final List<String> values;

    public RedisLuaCommand(String script, List<String> keys, List<String> values) {
        this.script = script;
        this.keys = Optional.ofNullable(keys).orElseGet(Collections::emptyList);
        this.values = Optional.ofNullable(values).orElseGet(Collections::emptyList);
    }

    public int getTotalElement() {

        return keys.size() + values.size();
    }

}
