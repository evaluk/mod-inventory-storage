package org.folio.rest.support.client;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.api.StorageTestSuite;
import org.folio.rest.support.HttpClient;
import org.folio.rest.support.JsonArrayHelper;
import org.folio.rest.support.Response;
import org.folio.rest.support.ResponseHandler;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class ReferenceRecordClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final HttpClient client;
  private final UrlMaker urlMaker;
  private final String resourceName;
  private final String tenantId;
  private final String collectionArrayPropertyName;

  public ReferenceRecordClient(
    HttpClient client,
    UrlMaker urlMaker,
    String resourceName,
    String tenantId,
    String collectionArrayPropertyName) {

    this.client = client;
    this.urlMaker = urlMaker;
    this.resourceName = resourceName;
    this.tenantId = tenantId;
    this.collectionArrayPropertyName = collectionArrayPropertyName;
  }

  public String create(JsonObject record)
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    CompletableFuture<Response> completed = new CompletableFuture<>();

    client.post(urlMaker.combine(""), record,
      StorageTestSuite.TENANT_ID, ResponseHandler.json(completed));

    Response response = completed.get(5, TimeUnit.SECONDS);

    assertThat(response.getStatusCode(), is(201));

    return response.getJson().getString("id");
  }

  public void deleteAllIndividually()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    List<JsonObject> records = getAll();

    records.forEach(record -> {
      try {
        CompletableFuture<Response> deleteFinished = new CompletableFuture<>();

        client.delete(urlMaker.combine(String.format("/%s",
          record.getString("id"))), tenantId,
          ResponseHandler.any(deleteFinished));

        Response deleteResponse = deleteFinished.get(5, TimeUnit.SECONDS);

        assertThat(String.format(
          "Failed to delete %s: %s", resourceName, deleteResponse.getBody()),
          deleteResponse.getStatusCode(), is(204));

      } catch (Throwable e) {
        assertThat(String.format("Exception whilst deleting %s individually: %s",
          resourceName, e.toString()),
          true, is(false));
      }
    });
  }

  public List<JsonObject> getByQuery(String query)
    throws UnsupportedEncodingException,
    MalformedURLException, InterruptedException, ExecutionException, TimeoutException {

    CompletableFuture<Response> getFinished = new CompletableFuture<>();

    client.get(urlMaker.combine(String.format("?query=%s",
      URLEncoder.encode(query, "UTF-8"))), tenantId,
      ResponseHandler.any(getFinished));

    Response response = getFinished.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Get records by query failed: %s", response.getBody()),
      response.getStatusCode(), is(200));

    log.info(String.format("Received: '%s'", response.getBody()));

    JsonObject json = response.getJson();

    if(!json.containsKey(collectionArrayPropertyName)) {
      throw new RuntimeException(String.format(
        "Collection array property \"%s\" is not present in: %s",
        collectionArrayPropertyName, json.encodePrettily()));
    }

    return JsonArrayHelper.toList(json
      .getJsonArray(collectionArrayPropertyName));
  }

  private List<JsonObject> getAll()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    CompletableFuture<Response> getFinished = new CompletableFuture<>();

    client.get(urlMaker.combine(""), tenantId,
      ResponseHandler.any(getFinished));

    Response response = getFinished.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Get all records failed: %s", response.getBody()),
      response.getStatusCode(), is(200));

    JsonObject json = response.getJson();

    if(!json.containsKey(collectionArrayPropertyName)) {
      throw new RuntimeException(String.format(
        "Collection array property \"%s\" is not present in: %s",
        collectionArrayPropertyName, json.encodePrettily()));
    }

    return JsonArrayHelper.toList(json
      .getJsonArray(collectionArrayPropertyName));
  }

  @FunctionalInterface
  public interface UrlMaker {
    URL combine(String subPath) throws MalformedURLException;
  }
}
