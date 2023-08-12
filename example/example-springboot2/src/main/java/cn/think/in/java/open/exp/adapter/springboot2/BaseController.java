package cn.think.in.java.open.exp.adapter.springboot2;

import cn.think.in.java.open.exp.adapter.springboot2.example.UserService;
import cn.think.in.java.open.exp.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.*;

/**
 * @Author cxs
 * @Description
 * @date 2023/8/9
 * @version 1.0
 **/
@RestController
@RequestMapping("/base")
@Slf4j
public class BaseController {
    static ThreadLocal<String> context = new InheritableThreadLocal<>();
    ExpAppContext expAppContext = ExpAppContextSpiFactory.getFirst();
    Map<String, Integer> sortMap = new HashMap<>();
    Map<String, String> pluginIdTenantIdMap = new HashMap<>();

    public BaseController() {
        sortMap.put("example-plugin1_1.0.0", 1);
        sortMap.put("example-plugin2_2.0.0", 2);
        pluginIdTenantIdMap.put("example-plugin2_2.0.0", "12345");
        pluginIdTenantIdMap.put("example-plugin1_1.0.0", "12345");

        expAppContext.setTenantCallback(new TenantCallback() {
            @Override
            public Integer getSort(String pluginId) {
                // 获取这个插件的排序
                return sortMap.get(pluginId);
            }

            @Override
            public Boolean isOwnCurrentTenant(String pluginId) {
                // 判断当前租户是不是这个匹配这个插件
                return context.get().equals(pluginIdTenantIdMap.get(pluginId));
            }
        });

    }


    @RequestMapping("/run")
    public String run(String tenantId) {
        // 上下文设置租户 id
        context.set(tenantId);
        try {
            List<UserService> userServices = expAppContext.get(UserService.class);
            // first 第一个就是这个租户优先级最高的.
            Optional<UserService> optional = userServices.stream().findFirst();
            if (optional.isPresent()) {
                optional.get().createUserExt();
            } else {
                return "not found";
            }
            return "success";
        } finally {
            // 上下文删除租户 id
            context.remove();
        }
    }


    @RequestMapping("/install")
    public String install(String path, String tenantId) throws Throwable {
        Plugin plugin = expAppContext.load(new File(path));

        sortMap.put(plugin.getPluginId(), Math.abs(new Random().nextInt(100)));
        pluginIdTenantIdMap.put(plugin.getPluginId(), tenantId);

        return plugin.getPluginId();
    }

    @RequestMapping("/unInstall")
    public String unInstall(String pluginId) throws Exception {
        log.info("plugin id {}", pluginId);
        expAppContext.unload(pluginId);
        pluginIdTenantIdMap.remove(pluginId);
        sortMap.remove(pluginId);
        return "ok";
    }
}