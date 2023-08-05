/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2023 Joe Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.mailific.spf.main;

import java.net.InetAddress;
import java.util.stream.Stream;
import net.mailific.spf.Result;
import net.mailific.spf.Settings;
import net.mailific.spf.Spf;
import net.mailific.spf.SpfImp;
import net.mailific.spf.dns.JndiResolver;
import net.mailific.spf.dns.NameResolver;

public class Main {

  public static void main(String[] args) {
    if (args == null
        || Stream.of(args).anyMatch(s -> s.equals("-h"))
        || args.length < 2
        || args.length > 3) {
      System.out.println("Usage: java -jar <path to jar> <ip> <sender>");
      System.exit(1);
    }

    try {
      InetAddress ip = InetAddress.getByName(args[0]);
      String sender = args[1];
      String domain = sender.substring(sender.indexOf("@") + 1);
      String ehlo = (args.length > 2) ? args[2] : domain;

      NameResolver dns = new JndiResolver();
      Settings settings = new Settings();
      Spf spf = new SpfImp(dns, settings);
      Result result = spf.checkHost(ip, domain, sender, ehlo);

      System.out.println(result);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
