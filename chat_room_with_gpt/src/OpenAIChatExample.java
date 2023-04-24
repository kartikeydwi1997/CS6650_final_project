import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Scanner;

/**
 * This class uses ChatGPT to generate responses for the chat application.
 */
public class OpenAIChatExample {

    /**
     * Generates a response to the input text using the ChatGPT model.
     * @param prompt The user's input text.
     * @return The chatbot's response to the input text.
     */
    public static String getOpenAIResponse(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        String data = "{\n" +
                "    \"model\":\"gpt-3.5-turbo\",\n" +
                "    \"messages\":[\n" +
                "        {\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},\n" +
                "        {\"role\": \"user\", \"content\": \"Who won the world series in 2020?\"},\n" +
                "        {\"role\": \"assistant\", \"content\": \"The Los Angeles Dodgers won the World Series in 2020.\"},\n"
                +
                "        {\"role\": \"user\", \"content\": \"" + prompt + "\"}\n" +
                "    ]\n" +
                "}";

        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Bearer <YOUR_API_KEY>");

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Print response
            JSONObject jsonObj = new JSONObject(response.toString());
            JSONArray choicesArr = jsonObj.getJSONArray("choices");
            JSONObject messageObj = choicesArr.getJSONObject(0).getJSONObject("message");
            String responseMessage = messageObj.getString("content");
            return responseMessage;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\u001B[32mYou: \u001B[0m");
            String userInput = scanner.nextLine();
            if (userInput.equals("exit")) {
                break;
            }
            String response = getOpenAIResponse(userInput);
            System.out.println("\u001B[31mChatGPT: \u001B[0m" + response);
            System.out.println();
        }
    }
}