/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import jakarta.inject.Inject;
import java.io.IOException;
import org.apache.pekko.stream.Materializer;
import play.api.libs.ws.ahc.AhcWSClientConfig;
import play.api.libs.ws.ahc.cache.AhcHttpCache;
import play.libs.ws.StandaloneWSClient;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;

/**
 * A WS client backed by AsyncHttpClient implementation.
 *
 * <p>See https://www.playframework.com/documentation/latest/JavaWS for documentation.
 */
public class AhcWSClient implements WSClient {

  private final StandaloneAhcWSClient client;
  private final Materializer materializer;

  public AhcWSClient(AsyncHttpClient client, Materializer materializer) {
    this.client = new StandaloneAhcWSClient(client, materializer);
    this.materializer = materializer;
  }

  @Inject
  public AhcWSClient(StandaloneAhcWSClient client, Materializer materializer) {
    this.client = client;
    this.materializer = materializer;
  }

  /**
   * Creates WS client manually from configuration, internally creating a new instance of
   * AsyncHttpClient and managing its own thread pool.
   *
   * <p>This client is not managed as part of Play's lifecycle, and <b>must</b> be closed by calling
   * ws.close(), otherwise you will run into memory leaks.
   *
   * @param config a config object, usually from AhcWSClientConfigFactory
   * @param cache if not null, provides HTTP caching.
   * @param materializer an Pekko materializer
   * @return a new instance of AhcWSClient.
   */
  public static AhcWSClient create(
      AhcWSClientConfig config, AhcHttpCache cache, Materializer materializer) {
    final StandaloneAhcWSClient client = StandaloneAhcWSClient.create(config, cache, materializer);
    return new AhcWSClient(client, materializer);
  }

  @Override
  public Object getUnderlying() {
    return client.getUnderlying();
  }

  @Override
  public play.api.libs.ws.WSClient asScala() {
    return new play.api.libs.ws.ahc.AhcWSClient(
        new play.api.libs.ws.ahc.StandaloneAhcWSClient(
            (AsyncHttpClient) getUnderlying(), materializer));
  }

  @Override
  public WSRequest url(String url) {
    final StandaloneAhcWSRequest plainWSRequest = client.url(url);
    return new AhcWSRequest(this, plainWSRequest);
  }

  @Override
  public void close() throws IOException {
    client.close();
  }

  /**
   * Return the implementation interface of {@link StandaloneAhcWSClient}.
   *
   * @return {@link StandaloneWSClient}
   */
  public StandaloneWSClient getStandaloneWSClient() {
    return client;
  }
}
