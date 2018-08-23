package org.folio.rest.api;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.support.client.ReferenceRecordClient;
import org.folio.rest.support.http.InterfaceUrls;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class ContributorTypesTest extends TestBase {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static ReferenceRecordClient contributorTypesClient;

  @BeforeClass
  public static void beforeAll() {
    contributorTypesClient = new ReferenceRecordClient(
      client, InterfaceUrls::contributorTypesUrl, "contributor type", StorageTestSuite.TENANT_ID, "contributorTypes");
  }

  @Before
  public void beforeEach()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    contributorTypesClient.deleteAllIndividually();
  }

  @Test
  public void canBeSortedByNameInExpectedWay()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException,
    UnsupportedEncodingException {

    contributorTypesClient.create(new JsonObject()
      .put("name", "Book designer")
      .put("code", "book-designer")
      .put("source", "test"));
    contributorTypesClient.create(new JsonObject()
      .put("name", "Bookjacket designer")
      .put("code", "bookjacket-designer")
      .put("source", "test"));
    contributorTypesClient.create(new JsonObject()
      .put("name", "Bookplate designer")
      .put("code", "bookplate-designer")
      .put("source", "test"));
    contributorTypesClient.create(new JsonObject()
      .put("name", "Book producer")
      .put("code", "book-producer")
      .put("source", "test"));

    List<JsonObject> sortedRecords =
      contributorTypesClient.getByQuery("cql.allRecords=1 sortBy name");

    assertThat(sortedRecords.size(), is(4));

    List<String> sortedValues = sortedRecords.stream()
      .map(record -> record.getString("name"))
      .collect(Collectors.toList());

    log.info(String.format("Sorted names: %s", String.join(",", sortedValues)));

    assertThat(sortedValues, contains("Book designer", "Book producer", "Bookjacket designer",
      "Bookplate designer"));

  }
}
