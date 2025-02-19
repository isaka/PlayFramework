/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import com.typesafe.config.Config;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import org.apache.pekko.stream.Materializer;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;
import play.libs.ws.StandaloneWSClient;
import play.libs.ws.WSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;

/**
 * The Play module to provide Java bindings for WS to an AsyncHTTPClient implementation.
 *
 * <p>This binding does not bind an AsyncHttpClient instance, as it's assumed you'll use the Scala
 * and Java modules together.
 */
public class AhcWSModule extends Module {

  @Override
  public List<Binding<?>> bindings(final Environment environment, final Config config) {
    // AsyncHttpClientProvider is added by the Scala API
    return Arrays.asList(
        bindClass(StandaloneWSClient.class).toProvider(StandaloneWSClientProvider.class),
        bindClass(WSClient.class).toProvider(AhcWSClientProvider.class));
  }

  @Singleton
  public static class AhcWSClientProvider implements Provider<WSClient> {
    private final AhcWSClient client;

    @Inject
    public AhcWSClientProvider(StandaloneWSClient standaloneWSClient, Materializer materializer) {
      this.client = new AhcWSClient((StandaloneAhcWSClient) standaloneWSClient, materializer);
    }

    @Override
    public WSClient get() {
      return client;
    }
  }

  @Singleton
  public static class StandaloneWSClientProvider implements Provider<StandaloneWSClient> {

    private final StandaloneWSClient standaloneWSClient;

    @Inject
    public StandaloneWSClientProvider(AsyncHttpClient asyncHttpClient, Materializer materializer) {
      this.standaloneWSClient = new StandaloneAhcWSClient(asyncHttpClient, materializer);
    }

    @Override
    public StandaloneWSClient get() {
      return standaloneWSClient;
    }
  }
}
