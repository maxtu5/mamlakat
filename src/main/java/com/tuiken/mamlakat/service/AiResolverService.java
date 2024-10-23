package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.Gender;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;

@Service
@RequiredArgsConstructor
public class AiResolverService {

    private final ChatClient aiClient;

    public String findChild(String child, String parent, Country country) {
        Gender childGender = Gender.fromTitle(child);
        String childType = childGender == null ?
                "child" :
                childGender.toString().equalsIgnoreCase("MALE") ? "son" : "daughter";

        String promtTemplate = """
                Return me a link to a  wikipedia page for %s, %s of %s, monarch of %s.
                Provide response in JSON format only. 
                The format should be a JSON object like {"link": "https://..."}. Provide no other commentary. 
                Make sure there are no newline characters in the JSON object response. 
        """;
        String prompt = String.format(promtTemplate, child, childType, parent, country.toString());

//        System.out.println(prompt);
        String response = aiClient.call(prompt);
        try {
            JSONObject urlObject = new JSONObject(response);
            response = urlObject.getString("link");
            response = response.trim();
            if (response.charAt(response.length()-1)==')') {
                int ind = response.lastIndexOf('(');
                response = response.substring(0, ind);
                if (response.charAt(response.length()-1)=='_') {
                    response = response.substring(0, response.length()-1);
                }
            }
            response = URLDecoder.decode(response);
            System.out.println(response);
        } catch (JSONException e) {
            response="";
        }
        return response;
    }
}
