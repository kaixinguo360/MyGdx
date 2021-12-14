package com.my.utils.world.com;

import com.my.utils.world.AssetsManager;
import com.my.utils.world.LoadContext;
import com.my.utils.world.Loader;
import com.my.utils.world.World;
import com.my.utils.world.sys.RenderSystem;

import java.util.HashMap;
import java.util.Map;

public class RenderLoader implements Loader {

    @Override
    public <E, T> T load(E config, Class<T> type, LoadContext context) {
        AssetsManager assetsManager = context.getEnvironment("world", World.class).getAssetsManager();
        Map<String, Object> map = (Map<String, Object>) config;
        String renderConfigId = (String) map.get("renderConfigId");
        RenderSystem.RenderConfig renderConfig = assetsManager.getAsset(renderConfigId, RenderSystem.RenderConfig.class);
        return (T) renderConfig.newInstance();
    }

    @Override
    public <E, T> E getConfig(T obj, Class<E> configType, LoadContext context) {
        AssetsManager assetsManager = context.getEnvironment("world", World.class).getAssetsManager();
        Render render = (Render) obj;
        String renderConfigId = assetsManager.getId(RenderSystem.RenderConfig.class, render.renderConfig);
        return (E) new HashMap<String, Object>(){{
            put("renderConfigId", renderConfigId);
        }};
    }

    @Override
    public <E, T> boolean handleable(Class<E> configType, Class<T> targetType) {
        return (Map.class.isAssignableFrom(configType)) && (targetType == Render.class);
    }
}
