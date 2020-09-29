package httpConnectionInJava11;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Request {

    @Test
    public void sendPOSTRequestWhenConnectViaSystemProxy() throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://postman-echo.com/post"))
                .headers("Content-Type", "text/plain;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString("Sample body"))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .proxy(ProxySelector.getDefault())
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
        assertEquals(response.statusCode(), HttpURLConnection.HTTP_OK);
    }

    @Test
    public void sendRequestWithONRedirect() throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://stackoverflow.com"))
                .version(HttpClient.Version.HTTP_1_1)
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());

        assertEquals(response.statusCode(), HttpURLConnection.HTTP_OK);
        assertEquals(response.request()
                .uri()
                .toString(), "https://stackoverflow.com/");
    }

    @Test
    public void sendRequestWithPasswordAuthentication() throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://postman-echo.com/basic-auth"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("postman", "password".toCharArray());
                    }
                })
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(response.statusCode(), HttpURLConnection.HTTP_OK);
    }

    @Test
    public void sendAsyncPostRequest() throws URISyntaxException, InterruptedException, ExecutionException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://postman-echo.com/post"))
                .headers("Content-Type", "text/plain;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString("Sample body"))
                .build();

        CompletableFuture<HttpResponse<String>> response = HttpClient.newBuilder()
                .build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(response.get()
                .statusCode(), HttpURLConnection.HTTP_OK);
    }

    @Test
    public void sendRequestViaSystemProxyAndDataFromFile() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://postman-echo.com/post"))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofFile(Paths.get("file.json")))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .proxy(ProxySelector.getDefault())
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode());
        System.out.println(response.body());

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(responseBody -> responseBody.statusCode() + responseBody.body())
                .thenAccept(System.out::println);

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(responseBody -> responseBody.statusCode() + responseBody.body())
                .thenAccept(System.out::println);

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(responseBody -> responseBody.statusCode() + responseBody.body())
                .thenAccept(System.out::println);
    }

    @Test
    public void sendMultipleAsynchronousRequests() throws URISyntaxException, InterruptedException, ExecutionException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://postman-echo.com/get"))
                .GET()
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletableFuture<HttpResponse<String>> response1 = HttpClient.newBuilder()
                .executor(executorService)
                .build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString());

        CompletableFuture<HttpResponse<String>> response2 = HttpClient.newBuilder()
                .executor(executorService)
                .build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString());

        CompletableFuture<HttpResponse<String>> response3 = HttpClient.newBuilder()
                .executor(executorService)
                .build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString());

        CompletableFuture.allOf(response1, response2, response3)
                .join();

        assertEquals(response1.get().statusCode(), HttpURLConnection.HTTP_OK);
        assertEquals(response2.get().statusCode(), HttpURLConnection.HTTP_OK);
        assertEquals(response3.get().statusCode(), HttpURLConnection.HTTP_OK);
    }


    @Test
    public void sendMultipleRequestViaStream() throws URISyntaxException, ExecutionException, InterruptedException {
        List<URI> targets = Arrays.asList(new URI("https://postman-echo.com/get?key1=value1"),
                new URI("https://postman-echo.com/get?key2=value2"));

        HttpClient client = HttpClient.newHttpClient();

        List<CompletableFuture<String>> futures = targets.stream()
                .map(target -> client.sendAsync(HttpRequest.newBuilder(target)
                        .GET()
                        .build(), HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                )
                .collect(Collectors.toList());
        if (futures.get(0).get().contains("key1")) {
            assertTrue(futures.get(0).get().contains("value1"));
            assertTrue(futures.get(1).get().contains("value2"));
        } else {
            assertTrue(futures.get(1).get().contains("value2"));
            assertTrue(futures.get(1).get().contains("value1"));
        }
    }

    @Test
    public void sendARequestWithoutSavingCookiesToStorage() throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://postman-echo.com/get"))
                .GET()
                .build();

        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_NONE);

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());

        CookieStore store = cookieManager.getCookieStore();
        System.out.println("\nCookies: " + store.getCookies());
    }

    @Test
    public void sendARequestWithSavingCookiesToStorage() throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://postman-echo.com/get"))
                .GET()
                .build();

        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());

        CookieStore store = cookieManager.getCookieStore();
        System.out.println("\nCookies: " + store.getCookies());
    }
}