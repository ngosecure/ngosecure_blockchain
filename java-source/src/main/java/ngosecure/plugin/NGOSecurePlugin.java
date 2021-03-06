package ngosecure.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.webserver.services.WebServerPluginRegistry;
import ngosecure.api.NGOSecureApi;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class NGOSecurePlugin implements WebServerPluginRegistry {
    private final List<Function<CordaRPCOps, ?>> webApis = ImmutableList.of(NGOSecureApi::new);

    private final Map<String, String> staticServeDirs = ImmutableMap.of(
            "ngosecure", getClass().getClassLoader().getResource("ngosecureWeb").toExternalForm()
    );

    @Override
    public List<Function<CordaRPCOps, ?>> getWebApis() {
        return webApis;
    }

    @Override
    public Map<String, String> getStaticServeDirs() {
        return staticServeDirs;
    }

    @Override
    public void customizeJSONSerialization(ObjectMapper objectMapper) {
    }
}