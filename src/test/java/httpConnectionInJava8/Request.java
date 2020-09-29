package httpConnectionInJava8;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Request {

    @Test
    public void sendGETRequest() throws IOException {
        URL url = new URL("https://reqres.in/api/users");                  //Создаём объект который сопоставляется с URL-адресом
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(); // Отправляем запрос на сервер для установки соединения (то самое трёхкратное рукопожатие -handshake);
        connection.setRequestMethod("GET");                                      //Устанавливаем какой метод будем использовать
        connection.setConnectTimeout(10000);                                     //Устанавливаем время для ожидания соединения и чтения
        connection.setReadTimeout(10000);
        connection.setRequestProperty("accept", "application/json");             //Устанавливаем необходимые заголовки
        connection.setRequestProperty("User-agent", "Test HTTP-connection with Java 8");
        connection.setDoInput(true);                                            //Устанавливаем значение true для указания, что мы будем читать из потока. Но по умолчанию так же true.

        InputStream inputStream = connection.getInputStream();                   //Читаем данные из открытого соединения данные
        DataInputStream dataInputStream = new DataInputStream(inputStream);  //оборачиваем их для считывание данных как примитивный тип
        System.out.printf("Received response body:\n%s\n", new String(dataInputStream.readAllBytes(), UTF_8)); //считываем их как байты

        inputStream.close();
        dataInputStream.close();
        assertEquals(connection.getResponseCode(), HttpURLConnection.HTTP_OK);
    }

    @Test
    public void sendPOSTRequest() throws IOException {
        String requestBody = "{\"name\": \"morpheus\", \"job\": \"leader\"}";
        URL url = new URL("https://reqres.in/api/users");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-agent", "Test HTTP-connection with Java 8");
        connection.setDoOutput(true);

        OutputStream os = connection.getOutputStream();
        byte[] bytes = requestBody.getBytes(UTF_8);
        os.write(bytes, 0, bytes.length);
        os.close();

        DataInputStream dataInputStream = new DataInputStream(connection.getInputStream());
        System.out.printf("Received response body:\n%s\n", new String(dataInputStream.readAllBytes(), UTF_8));
        dataInputStream.close();

        assertEquals(connection.getResponseCode(), HttpURLConnection.HTTP_CREATED);
    }

    @Test
    public void sendPOSTRequestAndReadCookie() throws IOException {
        URL url = new URL("https://reqres.in/api/users");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.disconnect();
        connection.setDoOutput(true);

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream("file.json"));
        String message = new String(bis.readAllBytes(), UTF_8);

        OutputStream os = connection.getOutputStream();
        byte[] bytes = message.getBytes(UTF_8);
        os.write(bytes, 0, bytes.length);
        os.close();
        bis.close();

        System.out.println(new String(connection.getInputStream().readAllBytes(), UTF_8));

        String cookieHeader = connection.getHeaderField("Set-Cookie");
        List<HttpCookie> cookies = HttpCookie.parse(cookieHeader);
        System.out.println(cookies.get(0));
    }
}
